/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

import com.carelstock.dao.OrderDAO;
import com.carelstock.dao.ProductDAO;
import com.carelstock.model.Order;
import com.carelstock.model.OrderItem;
import com.carelstock.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author squid
 */


public class OrderService {
    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;

    public OrderService() {
        this.orderDAO = new OrderDAO();
        this.productDAO = new ProductDAO();
    }

    // Process an order with server-side validation, price snapshotting, and total calculation
    public boolean createOrder(Order order) {
        // 1. Basic input validation
        if (order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()) {
            return false;
        }

        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            return false;
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        // 2. Validate products, fetch authoritative prices, and compute total amount
        for (OrderItem item : items) {
            if (item.getQuantity() <= 0) {
                return false; // Invalid quantity
            }

            Product product = productDAO.getProductById(item.getProductId());
            if (product == null) {
                return false; // Product does not exist
            }

            if (product.getStockQuantity() < item.getQuantity()) {
                return false; // Insufficient stock
            }

            // Snapshot the official price from the database
            BigDecimal unitPrice = product.getPrice();
            item.setUnitPrice(unitPrice);

            // Accumulate: unitPrice * quantity
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            calculatedTotal = calculatedTotal.add(itemTotal);
        }

        // 3. Assign calculated total to the order
        order.setTotalAmount(calculatedTotal);
        order.setStatus("COMPLETED");

        // 4. Delegate to DAO for transactional insertion and stock deduction
        return orderDAO.createOrder(order);
    }

    // Get an order by its ID
    public Order getOrderById(int orderId) {
        return orderDAO.getOrderById(orderId);
    }

    // Get all orders
    public List<Order> getAllOrders() {
        return orderDAO.getAllOrders();
    }
}
