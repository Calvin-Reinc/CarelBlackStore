package com.carelstock.controller;

import com.carelstock.model.User;
import com.carelstock.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Login endpoint.
 *
 *   POST /api/auth/login
 *   JSON body: { "username": "admin", "password": "..." }
 *
 *   200 -> session created; { "message", "username", "role" }
 *   400 -> missing fields / malformed JSON
 *   401 -> invalid username or password (same message for both, to prevent username enumeration)
 *
 * On success these session attributes are set (other servlets / the auth filter rely on them):
 *   user_id, username, role
 *
 * @author squid
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    private static final int SESSION_TIMEOUT_SECONDS = 1800; // 30 minutes
    private static final int MAX_USERNAME_LENGTH = 100;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Shape of the JSON body accepted by POST. */
    public record LoginRequest(String username, String password) {}

    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Read bytes, not a Reader: Jackson detects UTF-8 itself, so the request's
            // declared charset can't corrupt non-ASCII characters.
            LoginRequest body = MAPPER.readValue(req.getInputStream(), LoginRequest.class);

            if (body == null
                    || body.username() == null || body.username().isBlank()
                    || body.password() == null || body.password().isEmpty()) {
                sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("error", "Username and password are required."));
                return;
            }

            String username = body.username().trim();
            String password = body.password();

            // Oversized input can't be a real credential; don't hand it to the hashing code.
            if (username.length() > MAX_USERNAME_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                rejectCredentials(req, resp, username);
                return;
            }

            User user = userService.authenticate(username, password);
            if (user == null) {
                rejectCredentials(req, resp, username);
                return;
            }

            // Prevent session fixation: discard any pre-login session and issue a fresh one.
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
            session.setAttribute("user_id", user.getUserId());
            session.setAttribute("adminUser", user.getUsername());
            session.setAttribute("role", user.getRole());

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("message", "Login successful.");
            out.put("username", user.getUsername());
            out.put("role", user.getRole());
            sendJson(resp, HttpServletResponse.SC_OK, out);

        } catch (JsonProcessingException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "Malformed or invalid JSON body."));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error during login", e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Internal server error."));
        }
    }

    /** Same response for "unknown user" and "wrong password". Failed attempts are logged for auditing. */
    private void rejectCredentials(HttpServletRequest req, HttpServletResponse resp, String username) throws IOException {
        LOG.warning("Failed login for username '" + sanitizeForLog(username) + "' from " + req.getRemoteAddr());
        sendJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                Map.of("error", "Invalid username or password."));
    }

    /** Strips line breaks and caps length so attacker-controlled input can't forge log lines. */
    private static String sanitizeForLog(String s) {
        String cleaned = s.replaceAll("[\\r\\n\\t]", "_");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
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