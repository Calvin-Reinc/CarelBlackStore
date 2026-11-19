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

@WebServlet("/api/subscribe")
public class SubscribeServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public record SubscribeRequest(String email) {}

    private CommunityDAO communityDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.communityDAO = new CommunityDAO();
    }

    // Admin only (protected by SecurityFilter)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        sendJson(resp, HttpServletResponse.SC_OK, communityDAO.getAllSubscribers());
    }

    // Public (anyone on index.html can subscribe)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            SubscribeRequest body = MAPPER.readValue(req.getReader(), SubscribeRequest.class);
            if (body == null || body.email() == null || body.email().isBlank()) {
                throw new IllegalArgumentException("Email address is required.");
            }
            String email = body.email().trim().toLowerCase();
            if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }

            boolean added = communityDAO.addSubscriber(email);
            if (added) {
                sendJson(resp, HttpServletResponse.SC_CREATED,
                        Map.of("message", "Subscribed! You will get first notice on new drops."));
            } else {
                sendJson(resp, HttpServletResponse.SC_OK,
                        Map.of("message", "You are already on the drop list."));
            }
        } catch (JsonProcessingException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Invalid JSON body."));
        } catch (IllegalArgumentException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Map.of("error", "Could not subscribe right now."));
        }
    }

    // Admin only
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int id = Integer.parseInt(req.getParameter("id"));
            if (communityDAO.deleteSubscriber(id)) {
                sendJson(resp, HttpServletResponse.SC_OK, Map.of("message", "Subscriber removed."));
            } else {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND, Map.of("error", "Subscriber not found."));
            }
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Valid subscriber id is required."));
        }
    }

    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(json);
    }
}