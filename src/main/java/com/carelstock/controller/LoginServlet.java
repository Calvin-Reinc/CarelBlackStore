/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.controller;

import com.carelstock.model.User;
import com.carelstock.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
/**
 *
 * @author squid
 */



@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String rawJson = readRequestBody(req);
        if (rawJson == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid request body.\"}");
            return;
        }

        String username = extractJsonField(rawJson, "username");
        String password = extractJsonField(rawJson, "password");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Username and password are required.\"}");
            return;
        }

        User authenticatedUser = userService.authenticate(username, password);

        if (authenticatedUser != null) {
            // 1. Invalidate any existing session to prevent session fixation
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            // 2. Create a fresh session and enforce session expiration (30 minutes = 1800 seconds)
            HttpSession newSession = req.getSession(true);
            newSession.setMaxInactiveInterval(1800);
            newSession.setAttribute("user_id", authenticatedUser.getUserId());
            newSession.setAttribute("username", authenticatedUser.getUsername());
            newSession.setAttribute("role", authenticatedUser.getRole());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(String.format(
                "{\"message\": \"Login successful.\", \"username\": \"%s\", \"role\": \"%s\"}",
                escapeJson(authenticatedUser.getUsername()),
                escapeJson(authenticatedUser.getRole())
            ));
        } else {
            // Generic error message prevents username enumeration
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"Invalid username or password.\"}");
        }
    }

    private String readRequestBody(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String searchKey = "\"" + fieldName + "\":";
        int idx = json.indexOf(searchKey);
        if (idx == -1) return null;
        int start = idx + searchKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) {
            if (json.charAt(start) == '\"') {
                start++;
                int end = json.indexOf("\"", start);
                return end != -1 ? json.substring(start, end) : null;
            }
            start++;
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim().replace("\"", "");
    }

    private String escapeJson(String str) {
        return str == null ? "" : str.replace("\"", "\\\"");
    }
}
