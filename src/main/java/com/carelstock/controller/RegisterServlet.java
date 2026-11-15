package com.carelstock.controller;

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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Creates a new administrator account. ADMIN ONLY.
 *
 *   POST /api/auth/register
 *   JSON body: { "username": "newadmin", "password": "at-least-8-chars" }
 *
 *   201 -> created
 *   400 -> validation failed / malformed JSON
 *   401 -> not logged in
 *   403 -> logged in but not an ADMIN
 *   409 -> username already exists
 *
 * This endpoint used to be public, which let anyone on the internet create an ADMIN.
 * The very first admin must be inserted directly in the database (seed script).
 *
 * @author squid
 */
@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(RegisterServlet.class.getName());

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores everything past 72 bytes

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Shape of the JSON body accepted by POST. */
    public record RegisterRequest(String username, String password) {}

    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 1. Authorisation first: don't even parse the body for callers who aren't allowed in.
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                sendJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        Map.of("error", "Authentication required."));
                return;
            }
            if (!"ADMIN".equals(session.getAttribute("role"))) {
                sendJson(resp, HttpServletResponse.SC_FORBIDDEN,
                        Map.of("error", "Administrator access required."));
                return;
            }

            // 2. Parse and validate. (Read bytes, not a Reader, so non-ASCII text isn't corrupted.)
            RegisterRequest body = MAPPER.readValue(req.getInputStream(), RegisterRequest.class);
            String validationError = validate(body);
            if (validationError != null) {
                sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", validationError));
                return;
            }

            String username = body.username().trim();

            // 3. Create. The role is fixed server-side; the client can never choose it.
            if (userService.registerUser(username, body.password(), "ADMIN")) {
                LOG.info("Admin '" + session.getAttribute("username") + "' created administrator account '" + username + "'");
                sendJson(resp, HttpServletResponse.SC_CREATED,
                        Map.of("message", "Administrator account created successfully."));
            } else {
                sendJson(resp, HttpServletResponse.SC_CONFLICT,
                        Map.of("error", "Username already exists or invalid data provided."));
            }

        } catch (JsonProcessingException e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "Malformed or invalid JSON body."));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error during registration", e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Internal server error."));
        }
    }

    /** Returns a client-safe error message, or null if the request is valid. */
    private static String validate(RegisterRequest r) {
        if (r == null) {
            return "Request body is required.";
        }
        if (r.username() == null || !USERNAME_PATTERN.matcher(r.username().trim()).matches()) {
            return "Username must be 3 to 50 characters: letters, digits, '.', '_' or '-'.";
        }
        if (r.password() == null || r.password().length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        if (r.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "Password must be at most " + MAX_PASSWORD_BYTES + " bytes long.";
        }
        return null;
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