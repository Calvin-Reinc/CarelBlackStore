/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

/**
 * This one just act as a security layer as to the controller doesnt interact with the DAOs
 *
 * @author squid
 */

import com.carelstock.dao.ProductDAO;
import com.carelstock.model.Product;
import com.carelstock.service.CloudinaryService.UploadResult;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO;
    private final CloudinaryService cloudinaryService;

    public ProductService() {
        this.productDAO = new ProductDAO();
        this.cloudinaryService = new CloudinaryService();
    }

    // Existing constructor preserved for current tests
    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
        this.cloudinaryService = new CloudinaryService();
    }

    // Full constructor injection for testing both DAO and CloudinaryService
    public ProductService(ProductDAO productDAO, CloudinaryService cloudinaryService) {
        this.productDAO = productDAO;
        this.cloudinaryService = cloudinaryService;
    }

    // 1. Create product (JSON-only / no file upload)
    public boolean createProduct(Product product) {
        validateProduct(product);
        return productDAO.createProduct(product);
    }

    // 1b. Create product with optional uploaded image
    public boolean createProduct(Product product, byte[] imageBytes, String contentType) {
        validateProduct(product);

        String uploadedPublicId = null;
        if (imageBytes != null && imageBytes.length > 0) {
            UploadResult upload = cloudinaryService.uploadProductImage(imageBytes, contentType);
            product.setImageUrl(upload.imageUrl());
            product.setImagePublicId(upload.publicId());
            uploadedPublicId = upload.publicId();
        }

        boolean created = productDAO.createProduct(product);
        if (!created && uploadedPublicId != null) {
            // Roll back Cloudinary asset if DB insert failed
            cloudinaryService.deleteImage(uploadedPublicId);
        }
        return created;
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

    public List<Product> getFilteredProducts(String search, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                             String sortBy, String sortDir, Integer limit, Integer offset) {
        int l = (limit != null && limit > 0) ? limit : 20;
        int o = (offset != null && offset >= 0) ? offset : 0;

        return productDAO.getFilteredProducts(search, categoryId, minPrice, maxPrice, sortBy, sortDir, l, o);
    }

    // 4. Update product (preserves existing image if none is supplied on the Product object)
    public boolean updateProduct(Product product) {
        return updateProduct(product, null, null);
    }

    // 4b. Update product with optional new uploaded image
    public boolean updateProduct(Product product, byte[] newImageBytes, String contentType) {
        if (product == null || product.getProductId() <= 0) {
            throw new IllegalArgumentException("Invalid product ID for update.");
        }
        validateProduct(product);

        Product existing = productDAO.getProductById(product.getProductId());
        if (existing == null) {
            return false;
        }

        String newPublicId = null;
        String oldPublicIdToDelete = null;

        if (newImageBytes != null && newImageBytes.length > 0) {
            UploadResult upload = cloudinaryService.uploadProductImage(newImageBytes, contentType);
            product.setImageUrl(upload.imageUrl());
            product.setImagePublicId(upload.publicId());
            newPublicId = upload.publicId();
            oldPublicIdToDelete = existing.getImagePublicId();
        } else {
            // Keep existing image when editing product details without uploading a new file
            if (product.getImageUrl() == null) {
                product.setImageUrl(existing.getImageUrl());
            }
            if (product.getImagePublicId() == null) {
                product.setImagePublicId(existing.getImagePublicId());
            }
        }

        boolean updated = productDAO.updateProduct(product);
        if (updated) {
            if (oldPublicIdToDelete != null && !oldPublicIdToDelete.equals(newPublicId)) {
                cloudinaryService.deleteImage(oldPublicIdToDelete);
            }
        } else if (newPublicId != null) {
            // Roll back newly uploaded image if DB update failed
            cloudinaryService.deleteImage(newPublicId);
        }

        return updated;
    }

    // 5. Delete product by ID (and remove associated Cloudinary image if present)
    public boolean deleteProduct(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive integer.");
        }
        Product existing = productDAO.getProductById(productId);
        boolean deleted = productDAO.deleteProduct(productId);

        if (deleted && existing != null && existing.getImagePublicId() != null) {
            cloudinaryService.deleteImage(existing.getImagePublicId());
        }
        return deleted;
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