package com.carelstock.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Front door for every /api/* request.
 *
 *  1. Adds security headers to every response.
 *  2. CORS: OFF by default (the JSP views are served from the same origin, so none is needed).
 *     To enable it for a separate dev frontend, set the environment variable
 *     CORS_ALLOWED_ORIGIN=http://localhost:3000. Leave it unset in production.
 *  3. POST/PUT must send Content-Type: application/json (415 otherwise), except explicitly
 *     exempt bodyless POSTs and multipart product image upload POSTs. Together with
 *     SameSite=Lax session cookies this blocks cross-site HTML form posts (CSRF).
 *  4. Everything except the public paths needs a logged-in ADMIN session:
 *       no session        -> 401
 *       not an ADMIN      -> 403
 *
 * Paths are matched against servletPath + pathInfo, which the container has already decoded and
 * normalised. getRequestURI() is NOT used for security decisions: it is the raw path the client
 * sent, so something like /api/products/../orders would contain "/api/products" while actually
 * being routed to /api/orders.
 *
 * @author squid
 */
@WebFilter("/api/*")
public class SecurityFilter implements Filter {

    /** Reachable without a session. Exact matches only. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/checkout/paystack",
            "/api/checkout/paystack/webhook"
    );

    /** POST-only paths reachable without a session (visitor subscribe & contact forms). */
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/subscribe",
            "/api/contact"
    );

    /**
     * GET-only paths reachable without a session. Empty on purpose: this is an admin inventory
     * system, and these endpoints expose prices and stock levels. To publish a read-only catalogue,
     * add "/api/products" and "/api/categories" here (exact matches, GET only).
     */
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/products",
            "/api/categories",
            "/api/checkout/paystack/status"
    );

    /** POSTs that legitimately carry no body, so they are exempt from the JSON content-type rule. */
    private static final Set<String> BODYLESS_POST_PATHS = Set.of("/api/auth/logout");

    /** POSTs allowed to send multipart/form-data (e.g. product creation/update with image upload). */
    private static final Set<String> MULTIPART_POST_PATHS = Set.of("/api/products");

    private String corsAllowedOrigin; // null = CORS disabled

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String origin = System.getenv("CORS_ALLOWED_ORIGIN");
        this.corsAllowedOrigin = (origin == null || origin.isBlank()) ? null : origin.trim();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 1. Security headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        String method = req.getMethod();

        // 2. CORS (only when explicitly configured, and only for the one allowed origin)
        if (applyCors(req, resp) && "OPTIONS".equals(method)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // preflight answered
            return;
        }

        String path = normalisedPath(req);

        // 3. JSON-only bodies (except exempt bodyless POSTs and multipart upload POSTs)
        boolean allowedMultipart = "POST".equals(method)
                && MULTIPART_POST_PATHS.contains(path)
                && hasMultipartContentType(req);

        if (("POST".equals(method) || "PUT".equals(method))
                && !BODYLESS_POST_PATHS.contains(path)
                && !hasJsonContentType(req)
                && !allowedMultipart) {
            writeError(resp, 415, "Content-Type must be application/json.");
            return;
        }

        // 4. Public endpoints
        if (PUBLIC_PATHS.contains(path)
                || ("GET".equals(method) && PUBLIC_GET_PATHS.contains(path))
                || ("POST".equals(method) && PUBLIC_POST_PATHS.contains(path))) {
            chain.doFilter(request, response);
            return;
        }

        // 5. Everything else: logged-in ADMIN required
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to clean up
    }

    // ---------------------------------------------------------------------

    /** Adds CORS headers if the request's Origin is exactly the configured one. Returns true if it was. */
    private boolean applyCors(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (corsAllowedOrigin == null || origin == null || !origin.equals(corsAllowedOrigin)) {
            return false;
        }
        resp.setHeader("Access-Control-Allow-Origin", origin);
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.addHeader("Vary", "Origin");
        return true;
    }

    private static String normalisedPath(HttpServletRequest req) {
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static boolean hasJsonContentType(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("application/json");
    }

    private static boolean hasMultipartContentType(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    /** Messages are constants, so no JSON escaping is needed. */
    private static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}