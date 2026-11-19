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
        String insertOrderSql = "INSERT INTO orders (customer_name, customer_email, customer_phone, shipping_address, payment_reference, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Step A: Insert the master Order record and retrieve generated order_id
            int orderId;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setString(1, order.getCustomerName());
                pstmtOrder.setString(2, order.getCustomerEmail());
                pstmtOrder.setString(3, order.getCustomerPhone());
                pstmtOrder.setString(4, order.getShippingAddress());
                pstmtOrder.setString(5, order.getPaymentReference());
                pstmtOrder.setBigDecimal(6, order.getTotalAmount());
                pstmtOrder.setString(7, order.getStatus() != null ? order.getStatus() : "COMPLETED");

                int affectedRows = pstmtOrder.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                        order.setOrderId(orderId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Step B: Deduct stock safely and insert order items
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {

                for (OrderItem item : order.getItems()) {
                    pstmtStock.setInt(1, item.getQuantity());
                    pstmtStock.setInt(2, item.getProductId());
                    pstmtStock.setInt(3, item.getQuantity());

                    int stockUpdated = pstmtStock.executeUpdate();
                    if (stockUpdated == 0) {
                        conn.rollback();
                        return false;
                    }

                    pstmtItem.setInt(1, orderId);
                    pstmtItem.setInt(2, item.getProductId());
                    pstmtItem.setInt(3, item.getQuantity());
                    pstmtItem.setBigDecimal(4, item.getUnitPrice());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
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
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                pstmt.setInt(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        order = extractOrderFromResultSet(rs);
                    }
                }
            }

            if (order != null) {
                order.setItems(fetchOrderItemsByOrderId(conn, orderId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return order;
    }

    // Get all orders with items
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Order order = extractOrderFromResultSet(rs);
                order.setItems(fetchOrderItemsByOrderId(conn, order.getOrderId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

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

    private Order extractOrderFromResultSet(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerEmail(rs.getString("customer_email"));
        order.setCustomerPhone(rs.getString("customer_phone"));
        order.setShippingAddress(rs.getString("shipping_address"));
        order.setPaymentReference(rs.getString("payment_reference"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }

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
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtItems = conn.prepareStatement(deleteItemsSql);
                 PreparedStatement pstmtOrder = conn.prepareStatement(deleteOrderSql)) {

                pstmtItems.setInt(1, orderId);
                pstmtItems.executeUpdate();

                pstmtOrder.setInt(1, orderId);
                int affectedRows = pstmtOrder.executeUpdate();

                conn.commit();
                return affectedRows > 0;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}