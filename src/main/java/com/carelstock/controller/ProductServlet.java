package com.carelstock.controller;

import com.carelstock.model.Product;
import com.carelstock.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product endpoints.
 *
 *   GET    /api/products            list (search, category_id, min_price, max_price,
 *                                    sort_by, sort_dir, limit, offset)
 *   GET    /api/products?id=1       single product
 *   POST   /api/products            create  (JSON body OR multipart/form-data with optional "image")
 *   POST   /api/products?id=1       update  (multipart/form-data with optional "image")
 *   PUT    /api/products?id=1       update  (JSON body)
 *   DELETE /api/products?id=1       delete
 *
 * JSON body for POST / PUT:
 *   { "product_name": "Widget", "description": "...", "price": 9.99,
 *     "stock_quantity": 20, "category_id": 1 }
 *
 * @author squid
 */
@WebServlet("/api/products")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 5L * 1024 * 1024,       // 5 MB max per image
        maxRequestSize = 6L * 1024 * 1024     // 6 MB total multipart request
)
public class ProductServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ProductServlet.class.getName());

    // Thread-safe, shared. Java camelCase <-> JSON snake_case (productName <-> product_name).
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Shape of the JSON / multipart data accepted by POST and PUT. */
    public record ProductRequest(String productName, String description, BigDecimal price,
                                 Integer stockQuantity, Integer categoryId, String imageUrl) {}

    private ProductService productService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productService = new ProductService();
    }

    // 1. GET - all products (filtered/sorted/paged) or a single product by ?id=
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");

            if (id != null) {
                Product product = productService.getProductById(id);
                if (product == null) {
                    sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                            Map.of("error", "Product with ID " + id + " not found."));
                    return;
                }
                sendJson(resp, HttpServletResponse.SC_OK, product);
                return;
            }

            String search = req.getParameter("search");
            Integer categoryId = parseOptionalInt(req.getParameter("category_id"), "category_id");
            BigDecimal minPrice = parseOptionalDecimal(req.getParameter("min_price"), "min_price");
            BigDecimal maxPrice = parseOptionalDecimal(req.getParameter("max_price"), "max_price");
            String sortBy = req.getParameter("sort_by");
            String sortDir = req.getParameter("sort_dir");
            Integer limit = parseOptionalInt(req.getParameter("limit"), "limit");
            Integer offset = parseOptionalInt(req.getParameter("offset"), "offset");

            if (limit != null && (limit < 1 || limit > 100)) {
                throw new IllegalArgumentException("limit must be between 1 and 100.");
            }
            if (offset != null && offset < 0) {
                throw new IllegalArgumentException("offset must be 0 or greater.");
            }
            if (sortDir != null && !sortDir.isBlank()
                    && !sortDir.equalsIgnoreCase("ASC") && !sortDir.equalsIgnoreCase("DESC")) {
                throw new IllegalArgumentException("sort_dir must be ASC or DESC.");
            }

            List<Product> products = productService.getFilteredProducts(
                    search, categoryId, minPrice, maxPrice, sortBy, sortDir, limit, offset);
            sendJson(resp, HttpServletResponse.SC_OK, products);

        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 2. POST - create a product (JSON or multipart), or update with image when ?id= is supplied on multipart
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (isMultipart(req)) {
                Integer id = parseOptionalInt(req.getParameter("id"), "id");
                Product product = toProduct(readMultipartFields(req));

                byte[] imageBytes = null;
                String imageContentType = null;
                Part imagePart = req.getPart("image");
                if (imagePart != null && imagePart.getSize() > 0) {
                    try (InputStream in = imagePart.getInputStream()) {
                        imageBytes = in.readAllBytes();
                    }
                    imageContentType = imagePart.getContentType();
                }

                if (id != null) {
                    product.setProductId(id);
                    if (productService.updateProduct(product, imageBytes, imageContentType)) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("message", "Product updated successfully.");
                        body.put("image_url", product.getImageUrl());
                        sendJson(resp, HttpServletResponse.SC_OK, body);
                    } else {
                        sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                                Map.of("error", "Product not found or update failed."));
                    }
                } else {
                    if (productService.createProduct(product, imageBytes, imageContentType)) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("message", "Product created successfully.");
                        body.put("product_id", product.getProductId());
                        body.put("image_url", product.getImageUrl());
                        sendJson(resp, HttpServletResponse.SC_CREATED, body);
                    } else {
                        sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                Map.of("error", "Failed to create product."));
                    }
                }
                return;
            }

            // Standard JSON create
            Product product = toProduct(readBody(req));
            if (productService.createProduct(product)) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Product created successfully.");
                body.put("product_id", product.getProductId());
                body.put("image_url", product.getImageUrl());
                sendJson(resp, HttpServletResponse.SC_CREATED, body);
            } else {
                sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        Map.of("error", "Failed to create product."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 3. PUT - update a product (?id= in the URL, fields in the JSON body)
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (isMultipart(req)) {
                throw new IllegalArgumentException(
                        "Use POST /api/products?id={id} with multipart/form-data to upload an image when updating.");
            }

            Integer id = parseOptionalInt(req.getParameter("id"), "id");
            if (id == null) {
                throw new IllegalArgumentException("Product ID is required for updating.");
            }

            Product product = toProduct(readBody(req));
            product.setProductId(id);

            if (productService.updateProduct(product)) {
                sendJson(resp, HttpServletResponse.SC_OK,
                        Map.of("message", "Product updated successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Product not found or update failed."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 4. DELETE - remove a product by ?id=
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");
            if (id == null) {
                throw new IllegalArgumentException("Product ID is required for deletion.");
            }

            if (productService.deleteProduct(id)) {
                sendJson(resp, HttpServletResponse.SC_OK,
                        Map.of("message", "Product deleted successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Product not found."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // ---------------------------------------------------------------------
    // Request handling helpers
    // ---------------------------------------------------------------------

    private static boolean isMultipart(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    private ProductRequest readBody(HttpServletRequest req) throws IOException {
        ProductRequest body = MAPPER.readValue(req.getReader(), ProductRequest.class);
        if (body == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        return body;
    }

    private ProductRequest readMultipartFields(HttpServletRequest req) {
        String productName = req.getParameter("product_name");
        String description = req.getParameter("description");
        BigDecimal price = parseOptionalDecimal(req.getParameter("price"), "price");
        Integer stockQuantity = parseOptionalInt(req.getParameter("stock_quantity"), "stock_quantity");
        Integer categoryId = parseOptionalInt(req.getParameter("category_id"), "category_id");
        String imageUrl = req.getParameter("image_url");

        return new ProductRequest(productName, description, price, stockQuantity, categoryId, imageUrl);
    }

    /** Validates the request and maps it to the Product model. Messages here are safe to show the client. */
    private Product toProduct(ProductRequest r) {
        if (r.productName() == null || r.productName().isBlank()) {
            throw new IllegalArgumentException("product_name is required.");
        }
        if (r.productName().trim().length() > 255) {
            throw new IllegalArgumentException("product_name must be at most 255 characters.");
        }
        if (r.price() == null || r.price().signum() < 0) {
            throw new IllegalArgumentException("price is required and must be 0 or greater.");
        }
        if (r.stockQuantity() == null || r.stockQuantity() < 0) {
            throw new IllegalArgumentException("stock_quantity is required and must be 0 or greater.");
        }
        if (r.categoryId() == null || r.categoryId() <= 0) {
            throw new IllegalArgumentException("category_id is required.");
        }

        Product p = new Product();
        p.setProductName(r.productName().trim());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStockQuantity(r.stockQuantity());
        p.setCategoryId(r.categoryId());
        if (r.imageUrl() != null && !r.imageUrl().isBlank()) {
            p.setImageUrl(r.imageUrl().trim());
        }
        return p;
    }

    private static Integer parseOptionalInt(String raw, String name) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer.");
        }
    }

    private static BigDecimal parseOptionalDecimal(String raw, String name) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number.");
        }
    }

    // ---------------------------------------------------------------------
    // Response helpers
    // ---------------------------------------------------------------------

    /** Serialises first, so a serialisation failure can't leave a half-written response. */
    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(json);
    }

    /**
     * Single place that maps exceptions to responses.
     */
    private void handleException(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof JsonProcessingException) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "Malformed or invalid JSON body."));
        } else if (e instanceof IllegalArgumentException) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", String.valueOf(e.getMessage())));
        } else if (e instanceof IllegalStateException && e.getMessage() != null
                && (e.getMessage().contains("SizeLimit") || e.getMessage().contains("FileSize"))) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "Uploaded image exceeds the 5 MB size limit."));
        } else {
            LOG.log(Level.SEVERE, "Unhandled error on " + req.getMethod() + " " + req.getRequestURI(), e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Internal server error."));
        }
    }
}