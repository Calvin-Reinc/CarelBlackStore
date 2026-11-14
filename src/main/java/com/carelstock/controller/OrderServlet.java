/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.controller;

import com.carelstock.model.Order;
import com.carelstock.model.OrderItem;
import com.carelstock.service.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author squid
 */



/**
 * @author squid
 */
@WebServlet("/api/orders")
public class OrderServlet extends HttpServlet {
    private OrderService orderService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.orderService = new OrderService();
    }

    // 1. GET - Retrieve all orders or a single order by ID (?id=1) including line items
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String idParam = req.getParameter("id");
        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int orderId = Integer.parseInt(idParam);
                Order order = orderService.getOrderById(orderId);

                if (order != null) {
                    resp.getWriter().write(convertOrderToJson(order));
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Order with ID " + orderId + " not found.\"}");
                }
            } else {
                List<Order> orders = orderService.getAllOrders();
                resp.getWriter().write(convertOrderListToJson(orders));
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid order ID format.\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // 2. POST - Create a new order with line items and atomic transaction processing
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder jsonBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Failed to read request body.\"}");
            return;
        }

        try {
            // Simple lightweight parsing for demonstration matching manual serialization style
            String rawJson = jsonBody.toString();
            
            // Extract customer_name
            String customerName = extractJsonField(rawJson, "customer_name");
            if (customerName == null || customerName.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing required field: customer_name.\"}");
                return;
            }

            // Extract items array block
            List<OrderItem> items = parseOrderItemsFromJson(rawJson);
            if (items.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Order must contain at least one valid item with product_id and quantity.\"}");
                return;
            }

            Order newOrder = new Order();
            newOrder.setCustomerName(customerName);
            newOrder.setItems(items);

            boolean created = orderService.createOrder(newOrder);

            if (created) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"message\": \"Order placed successfully and stock deducted.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Failed to place order. Check product IDs or stock availability.\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // Helper to extract simple string fields from raw JSON string
    private String extractJsonField(String json, String fieldName) {
        String searchKey = "\"" + fieldName + "\":";
        int idx = json.indexOf(searchKey);
        if (idx == -1) return null;
        int start = idx + searchKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) {
            if (json.charAt(start) == '\"') {
                start++;
                int end = json.indexOf("\"", start);
                return end != -1 ? json.substring(start, end) : null;
            }
            start++;
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim().replace("\"", "");
    }

    // Helper to parse items from a basic JSON string structure
    private List<OrderItem> parseOrderItemsFromJson(String json) {
        List<OrderItem> items = new ArrayList<>();
        int itemsIdx = json.indexOf("\"items\"");
        if (itemsIdx == -1) return items;

        int bracketStart = json.indexOf("[", itemsIdx);
        int bracketEnd = json.indexOf("]", bracketStart);
        if (bracketStart == -1 || bracketEnd == -1) return items;

        String itemsContent = json.substring(bracketStart + 1, bracketEnd);
        String[] itemObjects = itemsContent.split("\\},\\s*\\{");

        for (String obj : itemObjects) {
            String cleanObj = obj.replace("{", "").replace("}", "");
            String productIdStr = extractJsonField("{" + cleanObj + "}", "product_id");
            String quantityStr = extractJsonField("{" + cleanObj + "}", "quantity");

            if (productIdStr != null && quantityStr != null) {
                try {
                    int productId = Integer.parseInt(productIdStr.trim());
                    int quantity = Integer.parseInt(quantityStr.trim());
                    if (quantity > 0) {
                        OrderItem item = new OrderItem();
                        item.setProductId(productId);
                        item.setQuantity(quantity);
                        items.add(item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return items;
    }

    // Manual JSON serialization for single order (including line items)
    private String convertOrderToJson(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{\"order_id\":%d,\"customer_name\":\"%s\",\"total_amount\":%s,\"status\":\"%s\",\"created_at\":\"%s\",\"items\":[",
            o.getOrderId(),
            escapeJson(o.getCustomerName()),
            o.getTotalAmount(),
            escapeJson(o.getStatus()),
            o.getCreatedAt()
        ));

        if (o.getItems() != null) {
            for (int i = 0; i < o.getItems().size(); i++) {
                OrderItem item = o.getItems().get(i);
                sb.append(String.format("{\"order_item_id\":%d,\"product_id\":%d,\"quantity\":%d,\"unit_price\":%s}",
                    item.getOrderItemId(),
                    item.getProductId(),
                    item.getQuantity(),
                    item.getUnitPrice()
                ));
                if (i < o.getItems().size() - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String convertOrderListToJson(List<Order> orders) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            sb.append(convertOrderToJson(orders.get(i)));
            if (i < orders.size() - 1) {
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