package com.carelstock.dao;

import com.carelstock.database.DatabaseConnection;
import com.carelstock.model.PendingCheckout;

import java.sql.*;

public class PendingCheckoutDAO {

    public boolean create(PendingCheckout pc) {
        String sql = "INSERT INTO pending_checkouts "
                + "(merchant_reference, ozow_payment_id, customer_name, customer_email, customer_phone, shipping_address, items_json, total_amount, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, pc.getMerchantReference());
            pstmt.setString(2, pc.getGatewayReference());
            pstmt.setString(3, pc.getCustomerName());
            pstmt.setString(4, pc.getCustomerEmail());
            pstmt.setString(5, pc.getCustomerPhone());
            pstmt.setString(6, pc.getShippingAddress());
            pstmt.setString(7, pc.getItemsJson());
            pstmt.setBigDecimal(8, pc.getTotalAmount());
            pstmt.setString(9, pc.getStatus() != null ? pc.getStatus() : "PENDING");

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) pc.setCheckoutId(keys.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateGatewayReference(String merchantReference, String gatewayRef) {
        String sql = "UPDATE pending_checkouts SET ozow_payment_id = ?, updated_at = CURRENT_TIMESTAMP WHERE merchant_reference = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gatewayRef);
            pstmt.setString(2, merchantReference);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public PendingCheckout findByMerchantReference(String merchantReference) {
        String sql = "SELECT * FROM pending_checkouts WHERE merchant_reference = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, merchantReference);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return extract(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Atomic lock: guarantees only one request (redirect verify OR webhook) can create the Order.
     */
    public boolean tryLockForCompletion(String merchantReference) {
        String sql = "UPDATE pending_checkouts SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP "
                + "WHERE merchant_reference = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, merchantReference);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markCompleted(String merchantReference, int orderId) {
        String sql = "UPDATE pending_checkouts SET status = 'COMPLETED', order_id = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE merchant_reference = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.setString(2, merchantReference);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markStatus(String merchantReference, String status) {
        String sql = "UPDATE pending_checkouts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE merchant_reference = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, merchantReference);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private PendingCheckout extract(ResultSet rs) throws SQLException {
        PendingCheckout pc = new PendingCheckout();
        pc.setCheckoutId(rs.getInt("checkout_id"));
        pc.setMerchantReference(rs.getString("merchant_reference"));
        pc.setGatewayReference(rs.getString("ozow_payment_id"));
        pc.setCustomerName(rs.getString("customer_name"));
        pc.setCustomerEmail(rs.getString("customer_email"));
        pc.setCustomerPhone(rs.getString("customer_phone"));
        pc.setShippingAddress(rs.getString("shipping_address"));
        pc.setItemsJson(rs.getString("items_json"));
        pc.setTotalAmount(rs.getBigDecimal("total_amount"));
        pc.setStatus(rs.getString("status"));
        int oid = rs.getInt("order_id");
        pc.setOrderId(rs.wasNull() ? null : oid);
        pc.setCreatedAt(rs.getTimestamp("created_at"));
        pc.setUpdatedAt(rs.getTimestamp("updated_at"));
        return pc;
    }
}