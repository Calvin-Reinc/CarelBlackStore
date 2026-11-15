/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.dao;

import com.carelstock.database.DatabaseConnection;
import com.carelstock.model.Order;
import com.carelstock.model.OrderItem;
import java.math.BigDecimal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author squid
 */


public class OrderDAO {

    // Create an order with transactional integrity (ACID)
    public boolean createOrder(Order order) {
        String insertOrderSql = "INSERT INTO orders (customer_name, total_amount, status) VALUES (?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // 1. Begin Transaction (Disable auto-commit)

            // Step A: Insert the master Order record and retrieve generated order_id
            int orderId;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setString(1, order.getCustomerName());
                pstmtOrder.setBigDecimal(2, order.getTotalAmount());
                pstmtOrder.setString(3, order.getStatus() != null ? order.getStatus() : "COMPLETED");

                int affectedRows = pstmtOrder.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Step B: Iterate through items, deduct stock safely, and insert order items
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {

                for (OrderItem item : order.getItems()) {
                    // 1. Deduct stock and enforce that stock cannot drop below zero
                    pstmtStock.setInt(1, item.getQuantity());
                    pstmtStock.setInt(2, item.getProductId());
                    pstmtStock.setInt(3, item.getQuantity());

                    int stockUpdated = pstmtStock.executeUpdate();
                    if (stockUpdated == 0) {
                        // Stock was insufficient or product didn't exist -> Trigger Rollback!
                        conn.rollback();
                        return false;
                    }

                    // 2. Add item to batch insertion
                    pstmtItem.setInt(1, orderId);
                    pstmtItem.setInt(2, item.getProductId());
                    pstmtItem.setInt(3, item.getQuantity());
                    pstmtItem.setBigDecimal(4, item.getUnitPrice());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            conn.commit(); // 2. Success! All queries passed—permanently save changes.
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // 3. Failure! Undo everything if any exception occurs.
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore default connection state
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Get an Order by ID (including its associated line items)
    public Order getOrderById(int orderId) {
        String orderSql = "SELECT * FROM orders WHERE order_id = ?";
        String itemsSql = "SELECT * FROM order_items WHERE order_id = ?";

        Order order = null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Fetch the order header
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                pstmt.setInt(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        order = extractOrderFromResultSet(rs);
                    }
                }
            }

            if (order != null) {
                // Fetch all line items for this order
                List<OrderItem> items = new ArrayList<>();
                try (PreparedStatement pstmt = conn.prepareStatement(itemsSql)) {
                    pstmt.setInt(1, orderId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            items.add(extractOrderItemFromResultSet(rs));
                        }
                    }
                }
                order.setItems(items);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return order;
    }

    // Get all orders (Headers only)
    public List<Order> getAllOrders() {
    List<Order> orders = new ArrayList<>();
    String sql = "SELECT * FROM orders ORDER BY created_at DESC";

    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            Order order = extractOrderFromResultSet(rs);
            
            // FETCH AND ATTACH ITEMS FOR THIS SPECIFIC ORDER
            List<OrderItem> items = fetchOrderItemsByOrderId(conn, order.getOrderId());
            order.setItems(items);
            
            orders.add(order);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return orders;
}
    
    // Calculate total revenue generated from all orders
    public BigDecimal getTotalRevenue() {
        String sql = "SELECT SUM(total_amount) AS total_revenue FROM orders WHERE status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                BigDecimal revenue = rs.getBigDecimal("total_revenue");
                return revenue != null ? revenue : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    // Helper mappers
    private Order extractOrderFromResultSet(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }

    private OrderItem extractOrderItemFromResultSet(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderItemId(rs.getInt("order_item_id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        return item;
    }
    
    // Helper method to fetch items for a given order ID
    private List<OrderItem> fetchOrderItemsByOrderId(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }
    
    public boolean deleteOrder(int orderId) throws SQLException {
        String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
        String deleteOrderSql = "DELETE FROM orders WHERE order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Disable auto-commit to manage the transaction safely
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtItems = conn.prepareStatement(deleteItemsSql);
                 PreparedStatement pstmtOrder = conn.prepareStatement(deleteOrderSql)) {

                // 1. Delete associated order items first (if not using ON DELETE CASCADE)
                pstmtItems.setInt(1, orderId);
                pstmtItems.executeUpdate();
                
                // 2. Delete the parent order
                pstmtOrder.setInt(1, orderId);
                int affectedRows = pstmtOrder.executeUpdate();

                // Commit transaction if successful
                conn.commit();
                return affectedRows > 0;

            } catch (SQLException e) {
                // Rollback if anything goes wrong
                conn.rollback();
                throw e;
            }
        }
    }
}
