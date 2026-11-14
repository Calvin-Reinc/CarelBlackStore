/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
/**
 *
 * @author squid
 */

@WebFilter("/api/*")
public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Set Enterprise Security Headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        // 2. Configure CORS Headers (Adjust Origin as needed for your frontend)
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:3000"); 
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 3. Define Public Endpoints
        // Login endpoint is public
        if (requestURI.endsWith("/api/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        // Allow public read access (GET) to products and categories for customers
        boolean isPublicRead = "GET".equalsIgnoreCase(method) && 
            (requestURI.contains("/api/products") || requestURI.contains("/api/categories"));

        if (isPublicRead) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Protect All Other Endpoints (Dashboard, Register, Mutations POST/PUT/DELETE)
        HttpSession session = httpRequest.getSession(false);
        boolean isAuthenticated = (session != null && session.getAttribute("user_id") != null);
        String role = isAuthenticated ? (String) session.getAttribute("role") : null;

        if (!isAuthenticated || !"ADMIN".equals(role)) {
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"error\": \"Access denied. Authentication and ADMIN privileges required.\"}");
            return;
        }

        // Proceed with request if authorized
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}