/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

import com.carelstock.dao.ProductDAO;
import com.carelstock.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 *
 * @author squid
 */


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductDAO productDAO;

    @InjectMocks
    private ProductService productService;

    private Product validProduct;

    @BeforeEach
    void setUp() {
        validProduct = new Product();
        validProduct.setProductId(1);
        validProduct.setProductName("Mechanical Keyboard");
        validProduct.setPrice(new BigDecimal("120.00"));
        validProduct.setStockQuantity(10);
        validProduct.setCategoryId(2);
    }

    // --- Requirement 3 & 6: Product Validation & Invalid Operations ---
    /*
    @Test
    void testAddProduct_NullOrEmptyName_FailsValidation() {
        validProduct.setProductName("   ");

        boolean result = productService.createProduct(validProduct);

        assertFalse(result, "Product with blank name must be rejected.");
        verify(productDAO, never()).createProduct(any());
    }

    @Test
    void testAddProduct_NegativePrice_FailsValidation() {
        validProduct.setPrice(new BigDecimal("-15.50"));

        boolean result = productService.createProduct(validProduct);
        
        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(validProduct);
        }, "Product with negative price must be rejected.");
        
        assertFalse(result, "");
        verify(productDAO, never()).createProduct(any());
    }

    @Test
    void testAddProduct_NegativeStock_FailsValidation() {
        validProduct.setStockQuantity(-5);

        boolean result = productService.createProduct(validProduct);

        assertFalse(result, "Product with negative stock quantity must be rejected.");
        verify(productDAO, never()).createProduct(any());
    }

    @Test
    void testAddProduct_ValidData_Succeeds() {
        when(productDAO.createProduct(validProduct)).thenReturn(true);

        boolean result = productService.createProduct(validProduct);

        assertTrue(result, "Valid product should be persisted.");
        verify(productDAO, times(1)).createProduct(validProduct);
    }

    // --- Requirement 4: Inventory Calculations ---

    @Test
    void testCalculateTotalInventoryValue_MultipleProducts() {
        Product p1 = new Product();
        p1.setPrice(new BigDecimal("50.00"));
        p1.setStockQuantity(4); // Subtotal = 200.00

        Product p2 = new Product();
        p2.setPrice(new BigDecimal("25.50"));
        p2.setStockQuantity(10); // Subtotal = 255.00

        when(productDAO.getAllProducts()).thenReturn(Arrays.asList(p1, p2));

        BigDecimal totalValue = productService.getTotalInventoryValue();

        assertEquals(0, new BigDecimal("455.00").compareTo(totalValue),
                "Total inventory value should equal sum of (price * stockQuantity).");
    }

    @Test
    void testCalculateTotalInventoryValue_EmptyInventory_ReturnsZero() {
        when(productDAO.getAllProducts()).thenReturn(Collections.emptyList());

        BigDecimal totalValue = productService.getTotalInventoryValue();

        assertEquals(0, BigDecimal.ZERO.compareTo(totalValue),
                "Empty inventory should calculate to 0.00.");
    }
*/
}
