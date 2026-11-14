/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.controller;

import com.carelstock.dao.CategoryDAO;
import com.carelstock.dao.OrderDAO;
import com.carelstock.dao.ProductDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
/**
 *
 * @author squid
 */


@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    private CategoryDAO categoryDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productDAO = new ProductDAO();
        this.orderDAO = new OrderDAO();
        this.categoryDAO = new CategoryDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            BigDecimal totalInventoryValue = productDAO.getTotalInventoryValue();
            int totalProductsCount = productDAO.getAllProducts().size();
            int totalCategoriesCount = categoryDAO.getAllCategories().size();
            int totalOrdersCount = orderDAO.getAllOrders().size();
            BigDecimal totalRevenue = orderDAO.getTotalRevenue();

            String jsonResponse = String.format(
                "{\"total_inventory_value\":%s,\"total_products\":%d,\"total_categories\":%d,\"total_orders\":%d,\"total_revenue\":%s}",
                totalInventoryValue,
                totalProductsCount,
                totalCategoriesCount,
                totalOrdersCount,
                totalRevenue
            );

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(jsonResponse);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}