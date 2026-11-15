package com.carelstock.controller;

import com.carelstock.model.Category;
import com.carelstock.service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Category endpoints.
 *
 *   GET    /api/categories          list all
 *   GET    /api/categories?id=1     single category
 *   POST   /api/categories          create  (JSON body)
 *   PUT    /api/categories?id=1     update  (JSON body)
 *   DELETE /api/categories?id=1     delete
 *
 * JSON body for POST / PUT:
 *   { "category_name": "Electronics", "description": "..." }
 *
 * @author squid
 */
@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CategoryServlet.class.getName());

    // Thread-safe, shared. Java camelCase <-> JSON snake_case (categoryName <-> category_name).
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Shape of the JSON body accepted by POST and PUT. */
    public record CategoryRequest(String categoryName, String description) {}

    private CategoryService categoryService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.categoryService = new CategoryService();
    }

    // 1. GET - all categories, or a single category by ?id=
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");

            if (id == null) {
                List<Category> categories = categoryService.getAllCategories();
                sendJson(resp, HttpServletResponse.SC_OK, categories);
                return;
            }

            Category category = categoryService.getCategoryById(id);
            if (category == null) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Category with ID " + id + " not found."));
                return;
            }
            sendJson(resp, HttpServletResponse.SC_OK, category);

        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 2. POST - create a category
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Category category = toCategory(readBody(req));

            if (categoryService.createCategory(category)) {
                sendJson(resp, HttpServletResponse.SC_CREATED,
                        Map.of("message", "Category created successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        Map.of("error", "Failed to create category."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 3. PUT - update a category (?id= in the URL, fields in the JSON body)
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");
            if (id == null) {
                throw new IllegalArgumentException("Category ID is required for updating.");
            }

            Category category = toCategory(readBody(req));
            category.setCategoryId(id);

            if (categoryService.updateCategory(category)) {
                sendJson(resp, HttpServletResponse.SC_OK,
                        Map.of("message", "Category updated successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Category not found or update failed."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 4. DELETE - remove a category by ?id=
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");
            if (id == null) {
                throw new IllegalArgumentException("Category ID is required for deletion.");
            }

            if (categoryService.deleteCategory(id)) {
                sendJson(resp, HttpServletResponse.SC_OK,
                        Map.of("message", "Category deleted successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Category not found or could not be deleted."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // ---------------------------------------------------------------------
    // Request handling helpers
    // ---------------------------------------------------------------------

    private CategoryRequest readBody(HttpServletRequest req) throws IOException {
        CategoryRequest body = MAPPER.readValue(req.getReader(), CategoryRequest.class);
        if (body == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        return body;
    }

    /** Validates the request and maps it to the Category model. Messages here are safe to show the client. */
    private Category toCategory(CategoryRequest r) {
        if (r.categoryName() == null || r.categoryName().isBlank()) {
            throw new IllegalArgumentException("category_name is required.");
        }
        if (r.categoryName().trim().length() > 100) {
            throw new IllegalArgumentException("category_name must be at most 100 characters.");
        }

        Category c = new Category();
        c.setCategoryName(r.categoryName().trim());
        c.setDescription(r.description());
        return c;
    }

    private static Integer parseOptionalInt(String raw, String name) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer.");
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
     *  - bad JSON / validation problems -> 400 with a safe message
     *  - anything else                  -> logged on the server, generic 500 to the client
     */
    private void handleException(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof JsonProcessingException) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "Malformed or invalid JSON body."));
        } else if (e instanceof IllegalArgumentException) {   // includes our validation errors
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", String.valueOf(e.getMessage())));
        } else {
            LOG.log(Level.SEVERE, "Unhandled error on " + req.getMethod() + " " + req.getRequestURI(), e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Internal server error."));
        }
    }
}