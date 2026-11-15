package com.carelstock.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Logout endpoint.
 *
 *   POST /api/auth/logout      (no body)
 *
 * Always returns 200, even if there was no session (logging out twice is harmless).
 * POST only: a GET would let any page log the user out via an image tag or link.
 *
 * @author squid
 */
@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);   // false: don't create a session just to destroy it
        if (session != null) {
            session.invalidate();
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"message\":\"Logged out successfully.\"}");
    }
}