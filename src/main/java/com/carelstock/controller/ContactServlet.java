package com.carelstock.controller;

import com.carelstock.dao.CommunityDAO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@WebServlet("/api/contact")
public class ContactServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public record ContactRequest(String name, String email, String message) {}

    private CommunityDAO communityDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.communityDAO = new CommunityDAO();
    }

    // Admin only
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        sendJson(resp, HttpServletResponse.SC_OK, communityDAO.getAllContactMessages());
    }

    // Public
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ContactRequest body = MAPPER.readValue(req.getReader(), ContactRequest.class);
            if (body == null || isBlank(body.name()) || isBlank(body.email()) || isBlank(body.message())) {
                throw new IllegalArgumentException("Name, email, and message are required.");
            }

            String name = body.name().trim();
            String email = body.email().trim().toLowerCase();
            String message = body.message().trim();

            if (name.length() > 150) throw new IllegalArgumentException("Name is too long.");
            if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }
            if (message.length() > 3000) throw new IllegalArgumentException("Message must be under 3000 characters.");

            if (communityDAO.addContactMessage(name, email, message)) {
                sendJson(resp, HttpServletResponse.SC_CREATED, Map.of("message", "Message sent successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Map.of("error", "Failed to save message."));
            }
        } catch (JsonProcessingException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Invalid JSON body."));
        } catch (IllegalArgumentException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    // Admin only
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int id = Integer.parseInt(req.getParameter("id"));
            if (communityDAO.deleteContactMessage(id)) {
                sendJson(resp, HttpServletResponse.SC_OK, Map.of("message", "Message deleted."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND, Map.of("error", "Message not found."));
            }
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Valid message id is required."));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(json);
    }
}