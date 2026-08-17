package com.autohub.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the real client IP address from an HTTP request,
 * accounting for Cloudflare and common reverse-proxy headers.
 */
public final class ClientIpUtil {

    private ClientIpUtil() {
        // Utility class — no instantiation
    }

    /**
     * Extracts the client IP from the request using the following priority:
     * <ol>
     *   <li>{@code CF-Connecting-IP} (Cloudflare)</li>
     *   <li>{@code X-Forwarded-For} (first IP in the chain)</li>
     *   <li>{@code X-Real-IP} (Nginx / load balancers)</li>
     *   <li>{@code request.getRemoteAddr()} (fallback)</li>
     * </ol>
     *
     * @param request the incoming HTTP request
     * @return the resolved client IP address
     */
    public static String getClientIp(HttpServletRequest request) {

        // 1. Cloudflare header
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValid(ip)) {
            return ip.trim();
        }

        // 2. X-Forwarded-For — may contain a chain like "client, proxy1, proxy2"
        ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            // Take the first (original client) IP
            return ip.split(",")[0].trim();
        }

        // 3. X-Real-IP
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return ip.trim();
        }

        // 4. Fallback to servlet container's remote address
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
