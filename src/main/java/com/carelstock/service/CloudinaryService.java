package com.carelstock.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Handles uploading and deleting product images via Cloudinary.
 *
 * Configuration priority:
 *  1. Environment variable: CLOUDINARY_URL
 *  2. Environment variables: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
 *  3. Classpath resource: cloudinary.properties (cloudinary.url OR cloudinary.cloud_name/api_key/api_secret)
 */
public class CloudinaryService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public record UploadResult(String imageUrl, String publicId) {}

    private volatile Cloudinary cloudinary;

    public CloudinaryService() {
        // Initialized lazily so unit tests or non-image operations don't fail without credentials
    }

    // Constructor for testing
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    private Cloudinary getClient() {
        Cloudinary client = cloudinary;
        if (client == null) {
            synchronized (this) {
                client = cloudinary;
                if (client == null) {
                    client = createClient();
                    cloudinary = client;
                }
            }
        }
        return client;
    }

    private Cloudinary createClient() {
        // 1. Single CLOUDINARY_URL env var (or JVM system property)
        String cloudinaryUrl = trimToNull(System.getenv("CLOUDINARY_URL"));
        if (cloudinaryUrl == null) {
            cloudinaryUrl = trimToNull(System.getProperty("CLOUDINARY_URL"));
        }
        if (cloudinaryUrl != null) {
            Cloudinary client = new Cloudinary(cloudinaryUrl);
            client.config.secure = true;
            return client;
        }

        // 2. Separate environment variables
        String cloudName = trimToNull(System.getenv("CLOUDINARY_CLOUD_NAME"));
        String apiKey = trimToNull(System.getenv("CLOUDINARY_API_KEY"));
        String apiSecret = trimToNull(System.getenv("CLOUDINARY_API_SECRET"));
        if (cloudName != null && apiKey != null && apiSecret != null) {
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }

        // 3. Local fallback: src/main/resources/cloudinary.properties
        Properties props = loadProperties();
        String propUrl = trimToNull(props.getProperty("cloudinary.url"));
        if (propUrl != null) {
            Cloudinary client = new Cloudinary(propUrl);
            client.config.secure = true;
            return client;
        }

        String propCloudName = trimToNull(props.getProperty("cloudinary.cloud_name"));
        String propApiKey = trimToNull(props.getProperty("cloudinary.api_key"));
        String propApiSecret = trimToNull(props.getProperty("cloudinary.api_secret"));
        if (propCloudName != null && propApiKey != null && propApiSecret != null) {
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", propCloudName,
                    "api_key", propApiKey,
                    "api_secret", propApiSecret,
                    "secure", true
            ));
        }

        throw new IllegalStateException(
                "Cloudinary credentials not configured. Set CLOUDINARY_URL, CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET, or src/main/resources/cloudinary.properties."
        );
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = CloudinaryService.class.getClassLoader().getResourceAsStream("cloudinary.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Handled by caller's validation
        }
        return props;
    }

    public UploadResult uploadProductImage(byte[] imageBytes, String contentType) {
        validateImage(imageBytes, contentType);

        try {
            Map<?, ?> result = getClient().uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap(
                            "folder", "carelstock/products",
                            "resource_type", "image",
                            "unique_filename", true
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new UploadResult(secureUrl, publicId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload product image to Cloudinary.", e);
        }
    }

    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            getClient().uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            System.err.println("Warning: Failed to delete Cloudinary image (" + publicId + "): " + e.getMessage());
        }
    }

    private void validateImage(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image file cannot be empty.");
        }
        if (imageBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file exceeds the maximum allowed size of 5 MB.");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported image type. Allowed types: JPEG, PNG, WebP, GIF.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}