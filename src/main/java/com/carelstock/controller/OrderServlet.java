package com.carelstock.controller;

import com.carelstock.model.Order;
import com.carelstock.model.OrderItem;
import com.carelstock.service.OrderService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Order endpoints.
 *
 *   GET  /api/orders          list all orders
 *   GET  /api/orders?id=1     single order, including its line items
 *   POST /api/orders          place an order (JSON body); stock is deducted atomically by OrderService
 *
 * JSON body for POST:
 *   { "customer_name": "Acme Ltd",
 *     "items": [ { "product_id": 1, "quantity": 2 },
 *                { "product_id": 5, "quantity": 1 } ] }
 *
 * Orders cannot be edited or deleted through the API (PUT/DELETE return 405).
 *
 * @author squid
 */
@WebServlet("/api/orders")
public class OrderServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(OrderServlet.class.getName());

    private static final int MAX_ITEMS_PER_ORDER = 100;
    private static final int MAX_QUANTITY_PER_ITEM = 100_000;

    // Thread-safe, shared. Java camelCase <-> JSON snake_case (customerName <-> customer_name).
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** One line of the order. Wrapper types so "missing" can be detected. */
    public record OrderItemRequest(Integer productId, Integer quantity) {}

    /** Shape of the JSON body accepted by POST. */
    public record OrderRequest(String customerName, List<OrderItemRequest> items) {}

    private OrderService orderService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.orderService = new OrderService();
    }

    // 1. GET - all orders, or a single order (with line items) by ?id=
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseOptionalInt(req.getParameter("id"), "id");

            if (id == null) {
                List<Order> orders = orderService.getAllOrders();
                sendJson(resp, HttpServletResponse.SC_OK, orders);
                return;
            }

            Order order = orderService.getOrderById(id);
            if (order == null) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "Order with ID " + id + " not found."));
                return;
            }
            sendJson(resp, HttpServletResponse.SC_OK, order);

        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // 2. POST - place a new order with line items
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Order order = toOrder(readBody(req));

            if (orderService.createOrder(order)) {
                sendJson(resp, HttpServletResponse.SC_CREATED,
                        Map.of("message", "Order placed successfully and stock deducted."));
            } else {
                sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("error", "Failed to place order. Check product IDs or stock availability."));
            }
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Parse the ID using your existing helper (assuming required ID for delete)
            Integer id = parseOptionalInt(req.getParameter("id"), "id");

            if (id == null) {
                sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        Map.of("error", "Missing or invalid order ID parameter."));
                return;
            }

            // Call your service layer to handle deletion
            boolean isDeleted = orderService.deleteOrder(id);

            if (!isDeleted) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND, 
                        Map.of("error", "Order with ID " + id + " not found."));
                return;
            }

            // Send a success message back as JSON
            sendJson(resp, HttpServletResponse.SC_OK, 
                    Map.of("message", "Order with ID " + id + " successfully deleted."));

        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    // ---------------------------------------------------------------------
    // Request handling helpers
    // ---------------------------------------------------------------------

    private OrderRequest readBody(HttpServletRequest req) throws IOException {
        OrderRequest body = MAPPER.readValue(req.getReader(), OrderRequest.class);
        if (body == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        return body;
    }

    /** Validates the request and maps it to the Order model. Messages here are safe to show the client. */
    private Order toOrder(OrderRequest r) {
        if (r.customerName() == null || r.customerName().isBlank()) {
            throw new IllegalArgumentException("customer_name is required.");
        }
        if (r.customerName().trim().length() > 255) {
            throw new IllegalArgumentException("customer_name must be at most 255 characters.");
        }
        if (r.items() == null || r.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }
        if (r.items().size() > MAX_ITEMS_PER_ORDER) {
            throw new IllegalArgumentException("Order cannot contain more than " + MAX_ITEMS_PER_ORDER + " items.");
        }

        List<OrderItem> items = new ArrayList<>();
        Set<Integer> seenProducts = new HashSet<>();

        for (OrderItemRequest line : r.items()) {
            if (line == null || line.productId() == null || line.productId() <= 0) {
                throw new IllegalArgumentException("Each item needs a valid product_id.");
            }
            if (line.quantity() == null || line.quantity() <= 0 || line.quantity() > MAX_QUANTITY_PER_ITEM) {
                throw new IllegalArgumentException(
                        "quantity for product " + line.productId() + " must be between 1 and " + MAX_QUANTITY_PER_ITEM + ".");
            }
            if (!seenProducts.add(line.productId())) {
                throw new IllegalArgumentException(
                        "product_id " + line.productId() + " appears more than once. Combine the quantities into one item.");
            }

            OrderItem item = new OrderItem();
            item.setProductId(line.productId());
            item.setQuantity(line.quantity());
            items.add(item);
        }

        Order order = new Order();
        order.setCustomerName(r.customerName().trim());
        order.setItems(items);
        return order;
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