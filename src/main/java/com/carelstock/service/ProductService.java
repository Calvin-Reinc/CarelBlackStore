/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

/**
 *
 * @author squid
 */

import com.carelstock.dao.ProductDAO;
import com.carelstock.model.Product;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    // Constructor injection for potential testing/mocking in later iterations
    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    // 1. Create product with business validation
    public boolean createProduct(Product product) {
        validateProduct(product);
        return productDAO.createProduct(product);
    }

    // 2. Retrieve all products
    public List<Product> getAllProducts() {
        return productDAO.getAllProducts();
    }

    // 3. Retrieve product by ID
    public Product getProductById(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive integer.");
        }
        return productDAO.getProductById(productId);
    }
    
    public BigDecimal getTotalInventoryValue() {
        return productDAO.getTotalInventoryValue();
    }
    
    // Updated method accepting sorting parameters
    public List<Product> getFilteredProducts(String search, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, 
                                             String sortBy, String sortDir, Integer limit, Integer offset) {
        int l = (limit != null && limit > 0) ? limit : 20; // default page size
        int o = (offset != null && offset >= 0) ? offset : 0; // default offset
        
        return productDAO.getFilteredProducts(search, categoryId, minPrice, maxPrice, sortBy, sortDir, l, o);
    }

    // 4. Update product with validation
    public boolean updateProduct(Product product) {
        if (product.getProductId() <= 0) {
            throw new IllegalArgumentException("Invalid product ID for update.");
        }
        validateProduct(product);
        return productDAO.updateProduct(product);
    }

    // 5. Delete product by ID
    public boolean deleteProduct(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive integer.");
        }
        return productDAO.deleteProduct(productId);
    }

    // Centralized business validation rules
    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price cannot be negative.");
        }
        if (product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
    }
}