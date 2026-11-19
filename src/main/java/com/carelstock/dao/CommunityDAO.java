package com.carelstock.dao;

import com.carelstock.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommunityDAO {

    public record Subscriber(int subscriberId, String email, Timestamp createdAt) {}
    public record ContactMessage(int messageId, String name, String email, String message, Timestamp createdAt) {}

    public boolean addSubscriber(String email) {
        String sql = "INSERT INTO subscribers (email) VALUES (?) ON CONFLICT (email) DO NOTHING";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            return pstmt.executeUpdate() > 0; // true = newly added, false = already exists
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save subscriber.", e);
        }
    }

    public List<Subscriber> getAllSubscribers() {
        List<Subscriber> list = new ArrayList<>();
        String sql = "SELECT subscriber_id, email, created_at FROM subscribers ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Subscriber(
                        rs.getInt("subscriber_id"),
                        rs.getString("email"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteSubscriber(int id) {
        String sql = "DELETE FROM subscribers WHERE subscriber_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addContactMessage(String name, String email, String message) {
        String sql = "INSERT INTO contact_messages (name, email, message) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, message);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ContactMessage> getAllContactMessages() {
        List<ContactMessage> list = new ArrayList<>();
        String sql = "SELECT message_id, name, email, message, created_at FROM contact_messages ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ContactMessage(
                        rs.getInt("message_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteContactMessage(int id) {
        String sql = "DELETE FROM contact_messages WHERE message_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}