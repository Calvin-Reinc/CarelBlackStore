/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.dao;

import com.carelstock.database.DatabaseConnection;
import com.carelstock.model.Product;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author squid
 */


public class ProductDAO {

    // Create a new product with category support
    public boolean createProduct(Product product) {
        String sql = "INSERT INTO products (product_name, description, price, stock_quantity, category_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getProductName());
            pstmt.setString(2, product.getDescription());
            pstmt.setBigDecimal(3, product.getPrice());
            pstmt.setInt(4, product.getStockQuantity());
            
            // Handle optional category mapping safely
            if (product.getCategoryId() > 0) {
                pstmt.setInt(5, product.getCategoryId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get a single product by ID
    public Product getProductById(int productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractProductFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get all products
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    // Update an existing product
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET product_name = ?, description = ?, price = ?, stock_quantity = ?, category_id = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getProductName());
            pstmt.setString(2, product.getDescription());
            pstmt.setBigDecimal(3, product.getPrice());
            pstmt.setInt(4, product.getStockQuantity());
            
            if (product.getCategoryId() > 0) {
                pstmt.setInt(5, product.getCategoryId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setInt(6, product.getProductId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete a product
    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to map ResultSet rows to Product objects
    private Product extractProductFromResultSet(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        return product;
    }
    
    // Dynamic search, filter, and pagination method
    // Dynamic search, filter, sorting, and pagination method
    public List<Product> getFilteredProducts(String searchKeyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, 
                                             String sortBy, String sortDir, int limit, int offset) {
        List<Product> products = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 1. Keyword search
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(product_name) LIKE ? OR LOWER(description) LIKE ?)");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        // 2. Category filter
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        // 3. Price range filters
        if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }

        // 4. Sorting (Whitelisted to prevent SQL injection)
        String orderByColumn = "product_id"; // default
        if ("price".equalsIgnoreCase(sortBy)) {
            orderByColumn = "price";
        } else if ("name".equalsIgnoreCase(sortBy)) {
            orderByColumn = "product_name";
        } else if ("created_at".equalsIgnoreCase(sortBy)) {
            orderByColumn = "created_at";
        } else if ("stock".equalsIgnoreCase(sortBy)) {
            orderByColumn = "stock_quantity";
        }

        String direction = "DESC"; // default
        if ("ASC".equalsIgnoreCase(sortDir)) {
            direction = "ASC";
        }

        sql.append(" ORDER BY ").append(orderByColumn).append(" ").append(direction);

        // 5. Pagination
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(extractProductFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
    
    // Calculate total monetary value of all inventory in stock
    public BigDecimal getTotalInventoryValue() {
        String sql = "SELECT SUM(price * stock_quantity) AS total_value FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                BigDecimal value = rs.getBigDecimal("total_value");
                return value != null ? value : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
    
}
