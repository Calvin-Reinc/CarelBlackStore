package com.carelstock.controller;

import com.carelstock.model.Product;
import com.carelstock.service.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author squid
 */


@WebServlet("/api/products")
public class ProductServlet extends HttpServlet {
    private ProductService productService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productService = new ProductService();
    }

    // 1. GET - Retrieve all products or a single product by ID (?id=1)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String idParam = req.getParameter("id");
        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int productId = Integer.parseInt(idParam);
                Product product = productService.getProductById(productId);
                
                if (product != null) {
                    resp.getWriter().write(convertProductToJson(product));
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Product with ID " + productId + " not found.\"}");
                }
            } else {
                // 1. Extract query parameters including sorting
                String search = req.getParameter("search");
                String categoryIdStr = req.getParameter("category_id");
                String minPriceStr = req.getParameter("min_price");
                String maxPriceStr = req.getParameter("max_price");
                String sortBy = req.getParameter("sort_by");       // e.g., price, name, created_at, stock
                String sortDir = req.getParameter("sort_dir");     // e.g., ASC, DESC
                String limitStr = req.getParameter("limit");
                String offsetStr = req.getParameter("offset");

                // 2. Parse optional numeric parameters safely
                Integer categoryId = (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) ? Integer.parseInt(categoryIdStr) : null;
                BigDecimal minPrice = (minPriceStr != null && !minPriceStr.trim().isEmpty()) ? new BigDecimal(minPriceStr) : null;
                BigDecimal maxPrice = (maxPriceStr != null && !maxPriceStr.trim().isEmpty()) ? new BigDecimal(maxPriceStr) : null;
                Integer limit = (limitStr != null && !limitStr.trim().isEmpty()) ? Integer.parseInt(limitStr) : null;
                Integer offset = (offsetStr != null && !offsetStr.trim().isEmpty()) ? Integer.parseInt(offsetStr) : null;

                // 3. Pass everything down through the service layer
                List<Product> products = productService.getFilteredProducts(search, categoryId, minPrice, maxPrice, sortBy, sortDir, limit, offset);
                
                resp.getWriter().write(convertProductListToJson(products));
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid numeric format in query parameters.\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // 2. POST - Create a new product
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String productName = req.getParameter("product_name");
            String description = req.getParameter("description");
            String priceStr = req.getParameter("price");
            String stockStr = req.getParameter("stock_quantity");
            String categoryIdStr = req.getParameter("category_id");

            // Fixed syntax error by removing trailing dangling ||
            if (productName == null || priceStr == null || stockStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing required fields: product_name, price, and stock_quantity are required.\"}");
                return;
            }

            BigDecimal price = new BigDecimal(priceStr);
            int stockQuantity = Integer.parseInt(stockStr);
            int categoryId = 0;
            
            if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
                try {
                    categoryId = Integer.parseInt(categoryIdStr);
                } catch (NumberFormatException e) {
                    // Ignore malformed category ID or leave as 0
                }
            }

            Product newProduct = new Product();
            newProduct.setProductName(productName);
            newProduct.setDescription(description);
            newProduct.setPrice(price);
            newProduct.setStockQuantity(stockQuantity);
            newProduct.setCategoryId(categoryId);

            boolean created = productService.createProduct(newProduct);

            if (created) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"message\": \"Product created successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to create product in database.\"}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid numeric format for price or stock quantity.\"}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // 3. PUT - Update an existing product
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String idParam = req.getParameter("id");
            if (idParam == null || idParam.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Product ID is required for updating.\"}");
                return;
            }

            int productId = Integer.parseInt(idParam);
            String productName = req.getParameter("product_name");
            String description = req.getParameter("description");
            String priceStr = req.getParameter("price");
            String stockStr = req.getParameter("stock_quantity");
            String categoryIdStr = req.getParameter("category_id");

            if (productName == null || priceStr == null || stockStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing required fields for update.\"}");
                return;
            }

            BigDecimal price = new BigDecimal(priceStr);
            int stockQuantity = Integer.parseInt(stockStr);
            int categoryId = 0;

            if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
                try {
                    categoryId = Integer.parseInt(categoryIdStr);
                } catch (NumberFormatException e) {
                    // Ignore malformed category ID
                }
            }

            Product product = new Product();
            product.setProductId(productId);
            product.setProductName(productName);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setCategoryId(categoryId);

            boolean updated = productService.updateProduct(product);

            if (updated) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"Product updated successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Product not found or update failed.\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // 4. DELETE - Remove a product by ID (?id=1)
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String idParam = req.getParameter("id");
        try {
            if (idParam == null || idParam.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Product ID is required for deletion.\"}");
                return;
            }

            int productId = Integer.parseInt(idParam);
            boolean deleted = productService.deleteProduct(productId);

            if (deleted) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"Product deleted successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Product not found.\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // Manual JSON serialization helpers including category_id
    private String convertProductToJson(Product p) {
        return String.format(
            "{\"product_id\":%d,\"product_name\":\"%s\",\"description\":\"%s\",\"price\":%s,\"stock_quantity\":%d,\"category_id\":%d,\"created_at\":\"%s\"}",
            p.getProductId(),
            escapeJson(p.getProductName()),
            escapeJson(p.getDescription()),
            p.getPrice(),
            p.getStockQuantity(),
            p.getCategoryId(),
            p.getCreatedAt()
        );
    }

    private String convertProductListToJson(List<Product> products) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            sb.append(convertProductToJson(products.get(i)));
            if (i < products.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }
}