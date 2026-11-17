package com.carelstock.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 *
 * @author squid
 */
public class Product {
    private int productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private int categoryId;
    private Timestamp createdAt;
    private String imageUrl;
    private String imagePublicId;

    // Constructors
    public Product() {}

    // Existing 7-arg constructor preserved for backward compatibility
    public Product(int productId, String productName, String description, BigDecimal price, int stockQuantity, int categoryId, Timestamp createdAt) {
        this(productId, productName, description, price, stockQuantity, categoryId, createdAt, null, null);
    }

    // Full constructor including image fields
    public Product(int productId, String productName, String description, BigDecimal price, int stockQuantity, int categoryId, Timestamp createdAt, String imageUrl, String imagePublicId) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
        this.imagePublicId = imagePublicId;
    }

    // Getters and Setters
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePublicId() {
        return imagePublicId;
    }

    public void setImagePublicId(String imagePublicId) {
        this.imagePublicId = imagePublicId;
    }
}