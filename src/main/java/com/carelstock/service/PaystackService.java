package com.carelstock.service;

import com.carelstock.dao.OrderDAO;
import com.carelstock.dao.PendingCheckoutDAO;
import com.carelstock.dao.ProductDAO;
import com.carelstock.model.Order;
import com.carelstock.model.OrderItem;
import com.carelstock.model.PendingCheckout;
import com.carelstock.model.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/**
 * Integrates Paystack Transaction Initialize, Verify, and HMAC-SHA512 Webhooks (ZAR).
 */
public class PaystackService {

    private static final String PAYSTACK_API_BASE = "https://api.paystack.co";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record CustomerInput(String name, String email, String phone, String address) {}
    public record CartLineInput(int productId, int quantity) {}
    public record PaymentInitResult(String reference, String accessCode, String paymentUrl) {}
    public record ConfirmResult(String status, Integer orderId, BigDecimal totalAmount, String reference) {}

    private record PaystackConfig(String secretKey, String publicBaseUrl) {}

    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;
    private final PendingCheckoutDAO pendingCheckoutDAO;
    private final HttpClient httpClient;

    private volatile PaystackConfig cachedConfig;

    public PaystackService() {
        this.productDAO = new ProductDAO();
        this.orderDAO = new OrderDAO();
        this.pendingCheckoutDAO = new PendingCheckoutDAO();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public PaymentInitResult initiateCheckout(CustomerInput customer, List<CartLineInput> lines, String requestBaseUrl) {
        validateCustomer(customer);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        PaystackConfig cfg = getRequiredConfig();

        Map<Integer, Integer> qtyByProduct = new LinkedHashMap<>();
        for (CartLineInput line : lines) {
            if (line == null || line.productId() <= 0 || line.quantity() <= 0) {
                throw new IllegalArgumentException("Each cart item must have a valid product_id and quantity.");
            }
            qtyByProduct.merge(line.productId(), line.quantity(), Integer::sum);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> normalizedItems = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : qtyByProduct.entrySet()) {
            int pid = entry.getKey();
            int qty = entry.getValue();

            Product product = productDAO.getProductById(pid);
            if (product == null) {
                throw new IllegalArgumentException("Product #" + pid + " no longer exists.");
            }
            if (product.getStockQuantity() < qty) {
                throw new IllegalArgumentException("Only " + product.getStockQuantity() + " left in stock for " + product.getProductName() + ".");
            }

            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

            normalizedItems.add(Map.of(
                    "product_id", pid,
                    "quantity", qty,
                    "unit_price", unitPrice
            ));
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

        String reference = "FJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String itemsJson;
        try {
            itemsJson = MAPPER.writeValueAsString(normalizedItems);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize checkout items.", e);
        }

        PendingCheckout pc = new PendingCheckout();
        pc.setMerchantReference(reference);
        pc.setCustomerName(customer.name().trim());
        pc.setCustomerEmail(customer.email().trim());
        pc.setCustomerPhone(customer.phone().trim());
        pc.setShippingAddress(customer.address().trim());
        pc.setItemsJson(itemsJson);
        pc.setTotalAmount(totalAmount);
        pc.setStatus("PENDING");

        if (!pendingCheckoutDAO.create(pc)) {
            throw new RuntimeException("Unable to save pending checkout.");
        }

        String publicBase = (cfg.publicBaseUrl() != null) ? cfg.publicBaseUrl() : stripTrailingSlash(requestBaseUrl);
        String callbackUrl = publicBase + "/payment-result.html";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("customer_name", customer.name().trim());
        metadata.put("phone", customer.phone().trim());
        metadata.put("shipping_address", customer.address().trim());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", customer.email().trim());
        payload.put("amount", amountInCents);
        payload.put("currency", "ZAR");
        payload.put("reference", reference);
        payload.put("callback_url", callbackUrl);
        payload.put("metadata", metadata);

        try {
            String bodyJson = MAPPER.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PAYSTACK_API_BASE + "/transaction/initialize"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + cfg.secretKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                pendingCheckoutDAO.markStatus(reference, "FAILED");
                throw new IllegalStateException("Paystack initialize failed (" + resp.statusCode() + "): " + resp.body());
            }

            JsonNode root = MAPPER.readTree(resp.body());
            if (!root.path("status").asBoolean(false)) {
                pendingCheckoutDAO.markStatus(reference, "FAILED");
                throw new IllegalStateException("Paystack rejected transaction: " + root.path("message").asText());
            }

            JsonNode data = root.path("data");
            String authUrl = textOrNull(data, "authorization_url");
            String accessCode = textOrNull(data, "access_code");

            if (authUrl == null || authUrl.isBlank()) {
                pendingCheckoutDAO.markStatus(reference, "FAILED");
                throw new IllegalStateException("Paystack response missing authorization_url.");
            }

            if (accessCode != null) {
                pendingCheckoutDAO.updateGatewayReference(reference, accessCode);
            }

            return new PaymentInitResult(reference, accessCode, authUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingCheckoutDAO.markStatus(reference, "FAILED");
            throw new RuntimeException("Paystack request interrupted.", e);
        } catch (IOException e) {
            pendingCheckoutDAO.markStatus(reference, "FAILED");
            throw new RuntimeException("Error communicating with Paystack.", e);
        }
    }

    /**
     * Called when the shopper returns to payment-result.html?reference=FJ-...
     * Verifies directly with Paystack API before inserting into orders.
     */
    public ConfirmResult confirmByReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is required.");
        }

        PendingCheckout pc = pendingCheckoutDAO.findByMerchantReference(reference.trim());
        if (pc == null) {
            return null;
        }

        if ("COMPLETED".equalsIgnoreCase(pc.getStatus()) && pc.getOrderId() != null) {
            return new ConfirmResult("COMPLETED", pc.getOrderId(), pc.getTotalAmount(), pc.getMerchantReference());
        }

        PaystackConfig cfg = getRequiredConfig();
        try {
            String encodedRef = URLEncoder.encode(pc.getMerchantReference(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PAYSTACK_API_BASE + "/transaction/verify/" + encodedRef))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + cfg.secretKey())
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return new ConfirmResult(pc.getStatus(), pc.getOrderId(), pc.getTotalAmount(), pc.getMerchantReference());
            }

            JsonNode root = MAPPER.readTree(resp.body());
            if (!root.path("status").asBoolean(false)) {
                return new ConfirmResult(pc.getStatus(), pc.getOrderId(), pc.getTotalAmount(), pc.getMerchantReference());
            }

            JsonNode data = root.path("data");
            String txStatus = textOrNull(data, "status");
            String currency = textOrNull(data, "currency");
            long paidCents = data.path("amount").asLong(-1L);
            long expectedCents = pc.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

            if ("success".equalsIgnoreCase(txStatus)
                    && "ZAR".equalsIgnoreCase(currency)
                    && paidCents == expectedCents) {
                return finalizePaidCheckout(pc);
            }

            if ("failed".equalsIgnoreCase(txStatus) || "abandoned".equalsIgnoreCase(txStatus)) {
                pendingCheckoutDAO.markStatus(pc.getMerchantReference(), txStatus.toUpperCase());
                return new ConfirmResult(txStatus.toUpperCase(), null, pc.getTotalAmount(), pc.getMerchantReference());
            }

            return new ConfirmResult(pc.getStatus(), pc.getOrderId(), pc.getTotalAmount(), pc.getMerchantReference());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while verifying Paystack transaction.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to verify Paystack transaction.", e);
        }
    }

    /**
     * Verifies Paystack x-paystack-signature (HMAC-SHA512) and finalizes charge.success webhooks.
     */
    public void processWebhook(String rawBody, String signatureHeader) {
        PaystackConfig cfg = getRequiredConfig();
        if (!verifyHmacSha512(rawBody, signatureHeader, cfg.secretKey())) {
            throw new SecurityException("Invalid Paystack webhook signature.");
        }

        try {
            JsonNode root = MAPPER.readTree(rawBody);
            String event = textOrNull(root, "event");
            if (!"charge.success".equalsIgnoreCase(event)) {
                return;
            }

            JsonNode data = root.path("data");
            String reference = textOrNull(data, "reference");
            String status = textOrNull(data, "status");
            String currency = textOrNull(data, "currency");
            long paidCents = data.path("amount").asLong(-1L);

            if (reference == null) return;

            PendingCheckout pc = pendingCheckoutDAO.findByMerchantReference(reference);
            if (pc == null) return;

            long expectedCents = pc.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            if ("success".equalsIgnoreCase(status)
                    && "ZAR".equalsIgnoreCase(currency)
                    && paidCents == expectedCents) {
                finalizePaidCheckout(pc);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed webhook JSON payload.", e);
        }
    }

    private ConfirmResult finalizePaidCheckout(PendingCheckout pc) {
        if (pendingCheckoutDAO.tryLockForCompletion(pc.getMerchantReference())) {
            try {
                List<Map<String, Object>> parsedItems = MAPPER.readValue(
                        pc.getItemsJson(), new TypeReference<List<Map<String, Object>>>() {});

                List<OrderItem> orderItems = new ArrayList<>();
                for (Map<String, Object> row : parsedItems) {
                    int pid = ((Number) row.get("product_id")).intValue();
                    int qty = ((Number) row.get("quantity")).intValue();
                    BigDecimal unitPrice = new BigDecimal(String.valueOf(row.get("unit_price")));

                    OrderItem oi = new OrderItem();
                    oi.setProductId(pid);
                    oi.setQuantity(qty);
                    oi.setUnitPrice(unitPrice);
                    orderItems.add(oi);
                }

                Order order = new Order();
                order.setCustomerName(pc.getCustomerName());
                order.setCustomerEmail(pc.getCustomerEmail());
                order.setCustomerPhone(pc.getCustomerPhone());
                order.setShippingAddress(pc.getShippingAddress());
                order.setPaymentReference(pc.getMerchantReference());
                order.setTotalAmount(pc.getTotalAmount());
                order.setStatus("COMPLETED");
                order.setItems(orderItems);

                if (orderDAO.createOrder(order)) {
                    pendingCheckoutDAO.markCompleted(pc.getMerchantReference(), order.getOrderId());
                    return new ConfirmResult("COMPLETED", order.getOrderId(), pc.getTotalAmount(), pc.getMerchantReference());
                } else {
                    pendingCheckoutDAO.markStatus(pc.getMerchantReference(), "STOCK_CONFLICT");
                    return new ConfirmResult("STOCK_CONFLICT", null, pc.getTotalAmount(), pc.getMerchantReference());
                }
            } catch (Exception e) {
                pendingCheckoutDAO.markStatus(pc.getMerchantReference(), "ERROR");
                throw new RuntimeException("Failed to finalize paid order.", e);
            }
        }

        PendingCheckout latest = pendingCheckoutDAO.findByMerchantReference(pc.getMerchantReference());
        return new ConfirmResult(latest.getStatus(), latest.getOrderId(), latest.getTotalAmount(), latest.getMerchantReference());
    }

    private static boolean verifyHmacSha512(String rawBody, String signatureHeader, String secretKey) {
        if (rawBody == null || signatureHeader == null || secretKey == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expectedHex = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static void validateCustomer(CustomerInput c) {
        if (c == null || isBlank(c.name()) || isBlank(c.email()) || isBlank(c.phone()) || isBlank(c.address())) {
            throw new IllegalArgumentException("Full name, email, phone, and delivery address are required.");
        }
    }

    private PaystackConfig getRequiredConfig() {
        PaystackConfig cfg = loadConfig();
        if (isBlank(cfg.secretKey())) {
            throw new IllegalStateException(
                    "Paystack secret key not configured. Set PAYSTACK_SECRET_KEY or src/main/resources/paystack.properties.");
        }
        return cfg;
    }

    private PaystackConfig loadConfig() {
        if (cachedConfig != null) return cachedConfig;

        Properties props = new Properties();
        try (InputStream in = PaystackService.class.getClassLoader().getResourceAsStream("paystack.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {}

        String secretKey = firstNonBlank(System.getenv("PAYSTACK_SECRET_KEY"), props.getProperty("paystack.secret_key"), null);
        String publicBaseUrl = firstNonBlank(System.getenv("PAYSTACK_PUBLIC_BASE_URL"), props.getProperty("paystack.public_base_url"), null);

        cachedConfig = new PaystackConfig(
                secretKey,
                publicBaseUrl != null ? stripTrailingSlash(publicBaseUrl) : null
        );
        return cachedConfig;
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (!isBlank(a)) return a.trim();
        if (!isBlank(b)) return b.trim();
        return fallback;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}