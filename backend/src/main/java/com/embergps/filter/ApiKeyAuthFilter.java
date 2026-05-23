package com.embergps.filter;

import com.embergps.config.AppConfig;
import com.embergps.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Servlet filter that validates API keys on every request to /api/**.
 *
 * <ul>
 *   <li>Requests to /api/v1/gps/ingest  require a device API key in {@code X-API-Key}.
 *   <li>Requests to /api/v1/admin/**    require the admin key in {@code X-Admin-Key}.
 *   <li>Requests to /api/v1/gps/latest and /api/v1/gps/history/** require either key.
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter implements Filter {

    static final String DEVICE_KEY_HEADER = "X-API-Key";
    static final String ADMIN_KEY_HEADER  = "X-Admin-Key";

    private final DeviceRepository deviceRepository;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // Actuator endpoints and OPTIONS pre-flight are open
        if (path.startsWith("/actuator") || "OPTIONS".equals(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Admin endpoints — require admin key
        if (path.startsWith("/api/v1/admin")) {
            String adminKey = req.getHeader(ADMIN_KEY_HEADER);
            if (!isValidAdminKey(adminKey)) {
                reject(resp, "Missing or invalid admin API key");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Ingest endpoint — require a valid device API key
        if (path.startsWith("/api/v1/gps/ingest")) {
            String deviceKey = req.getHeader(DEVICE_KEY_HEADER);
            if (deviceKey == null || !isRegisteredDeviceKey(deviceKey)) {
                reject(resp, "Missing or invalid device API key");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Query endpoints — require either key
        if (path.startsWith("/api/v1/gps")) {
            String deviceKey = req.getHeader(DEVICE_KEY_HEADER);
            String adminKey  = req.getHeader(ADMIN_KEY_HEADER);
            if ((deviceKey == null || !isRegisteredDeviceKey(deviceKey))
                    && !isValidAdminKey(adminKey)) {
                reject(resp, "Authentication required");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------

    private boolean isValidAdminKey(String key) {
        if (key == null || key.isBlank()) return false;
        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8),
                appConfig.getAdminApiKey().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isRegisteredDeviceKey(String key) {
        if (key == null || key.isBlank()) return false;
        try {
            String hash = sha256Hex(key);
            return deviceRepository.findByApiKeyHash(hash)
                    .map(d -> d.isActive())
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Error verifying device API key: {}", e.getMessage());
            return false;
        }
    }

    /** Compute SHA-256 hex digest of the given string. */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void reject(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(resp.getWriter(),
                Map.of("status", 401, "error", "Unauthorized", "message", message));
    }
}
