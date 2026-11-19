package com.carelstock.controller;

import com.carelstock.service.PaystackService;
import com.carelstock.service.PaystackService.CartLineInput;
import com.carelstock.service.PaystackService.ConfirmResult;
import com.carelstock.service.PaystackService.CustomerInput;
import com.carelstock.service.PaystackService.PaymentInitResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guest Paystack checkout endpoints:
 *   POST /api/checkout/paystack          -> validate cart & start Paystack session (returns payment_url)
 *   GET  /api/checkout/paystack/status   -> verify ?reference= with Paystack & finalize order if paid
 *   POST /api/checkout/paystack/webhook  -> Paystack HMAC-SHA512 webhook (charge.success)
 */
@WebServlet("/api/checkout/paystack/*")
public class PaystackCheckoutServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PaystackCheckoutServlet.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public record CheckoutCustomer(String name, String email, String phone, String address) {}
    public record CheckoutItem(Integer productId, Integer quantity) {}
    public record CheckoutRequest(CheckoutCustomer customer, List<CheckoutItem> items) {}

    private PaystackService paystackService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.paystackService = new PaystackService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (!"/status".equals(pathInfo)) {
            sendJson(resp, HttpServletResponse.SC_NOT_FOUND, Map.of("error", "Endpoint not found."));
            return;
        }

        try {
            String reference = req.getParameter("reference");
            if (reference == null || reference.isBlank()) {
                reference = req.getParameter("trxref");
            }

            ConfirmResult result = paystackService.confirmByReference(reference);
            if (result == null) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND, Map.of("error", "Payment reference not found."));
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("status", result.status());
            body.put("reference", result.reference());
            body.put("total_amount", result.totalAmount());
            if (result.orderId() != null) {
                body.put("order_id", result.orderId());
            }
            sendJson(resp, HttpServletResponse.SC_OK, body);
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if ("/webhook".equals(pathInfo)) {
            handleWebhook(req, resp);
            return;
        }

        if (pathInfo != null && !pathInfo.isEmpty() && !"/".equals(pathInfo)) {
            sendJson(resp, HttpServletResponse.SC_NOT_FOUND, Map.of("error", "Endpoint not found."));
            return;
        }

        try {
            CheckoutRequest body = MAPPER.readValue(req.getReader(), CheckoutRequest.class);
            if (body == null || body.customer() == null) {
                throw new IllegalArgumentException("Customer details are required.");
            }
            if (body.items() == null || body.items().isEmpty()) {
                throw new IllegalArgumentException("Your cart is empty.");
            }

            CustomerInput customer = new CustomerInput(
                    body.customer().name(),
                    body.customer().email(),
                    body.customer().phone(),
                    body.customer().address()
            );

            List<CartLineInput> lines = new ArrayList<>();
            for (CheckoutItem item : body.items()) {
                if (item == null || item.productId() == null || item.quantity() == null) {
                    throw new IllegalArgumentException("Each cart item needs product_id and quantity.");
                }
                lines.add(new CartLineInput(item.productId(), item.quantity()));
            }

            String requestBaseUrl = buildRequestBaseUrl(req);
            PaymentInitResult init = paystackService.initiateCheckout(customer, lines, requestBaseUrl);

            sendJson(resp, HttpServletResponse.SC_OK, Map.of(
                    "reference", init.reference(),
                    "payment_url", init.paymentUrl()
            ));
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    private void handleWebhook(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String rawBody = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String signature = req.getHeader("x-paystack-signature");

            paystackService.processWebhook(rawBody, signature);
            sendJson(resp, HttpServletResponse.SC_OK, Map.of("status", "ok"));
        } catch (SecurityException se) {
            sendJson(resp, HttpServletResponse.SC_UNAUTHORIZED, Map.of("error", "Invalid signature."));
        } catch (Exception e) {
            handleException(req, resp, e);
        }
    }

    private static String buildRequestBaseUrl(HttpServletRequest req) {
        String forwardedProto = req.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null && !forwardedProto.isBlank())
                ? forwardedProto.split(",")[0].trim()
                : req.getScheme();

        String forwardedHost = req.getHeader("X-Forwarded-Host");
        String host = (forwardedHost != null && !forwardedHost.isBlank())
                ? forwardedHost.split(",")[0].trim()
                : req.getHeader("Host");

        if (host == null || host.isBlank()) {
            int port = req.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
            host = req.getServerName() + (defaultPort ? "" : ":" + port);
        }

        return scheme + "://" + host + req.getContextPath();
    }

    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(json);
    }

    private void handleException(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof JsonProcessingException) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Malformed or invalid JSON body."));
        } else if (e instanceof IllegalArgumentException) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", String.valueOf(e.getMessage())));
        } else {
            LOG.log(Level.SEVERE, "Unhandled error on " + req.getMethod() + " " + req.getRequestURI(), e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Map.of("error", "Checkout service error. Check server logs."));
        }
    }
}