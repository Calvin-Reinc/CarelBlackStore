package com.carelstock.controller;

import com.carelstock.dao.CategoryDAO;
import com.carelstock.dao.OrderDAO;
import com.carelstock.dao.ProductDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dashboard summary.
 *
 *   GET /api/dashboard
 *   200 -> { "total_inventory_value", "total_products", "total_categories",
 *            "total_orders", "total_revenue" }
 *
 * TODO (service/DAO phase): this servlet talks to the DAOs directly and counts rows by loading
 * every product, category and order into memory. Both should move: counts into COUNT(*) queries
 * in the DAOs, and the assembly into a DashboardService, so the servlet only calls the service.
 *
 * @author squid
 */
@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

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
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            // An empty table can make SUM() return null, so default to zero
            summary.put("total_inventory_value",
                    Objects.requireNonNullElse(productDAO.getTotalInventoryValue(), BigDecimal.ZERO));
            summary.put("total_products", productDAO.getAllProducts().size());      // TODO: COUNT(*)
            summary.put("total_categories", categoryDAO.getAllCategories().size()); // TODO: COUNT(*)
            summary.put("total_orders", orderDAO.getAllOrders().size());            // TODO: COUNT(*)
            summary.put("total_revenue",
                    Objects.requireNonNullElse(orderDAO.getTotalRevenue(), BigDecimal.ZERO));

            sendJson(resp, HttpServletResponse.SC_OK, summary);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error building dashboard summary", e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Internal server error."));
        }
    }

    /** Serialises first, so a serialisation failure can't leave a half-written response. */
    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(json);
    }
}