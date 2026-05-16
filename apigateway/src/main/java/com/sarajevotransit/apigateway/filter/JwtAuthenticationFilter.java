package com.sarajevotransit.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarajevotransit.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "jti:blacklist:";
    private static final String USER_LOGOUT_ALL_KEY = "user:logout-all:";

    // Always public — no token needed regardless of HTTP method
    private static final List<String> PUBLIC_ALWAYS = List.of(
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/api/v1/discovery/**",
            "/api/discovery/**",
            "/api/v1/test/**"
    );

    // Public for GET requests only (public data reads)
    // Both exact paths and /** variants needed: AntPathMatcher does not match /foo/** against /foo (no trailing slash)
    private static final List<String> PUBLIC_GET = List.of(
            "/api/v1/lines",       "/api/v1/lines/**",
            "/api/v1/stations",    "/api/v1/stations/**",
            "/api/v1/timetables",  "/api/v1/timetables/**",
            "/api/v1/directions",  "/api/v1/directions/**",
            "/api/v1/route-points","/api/v1/route-points/**",
            "/api/v1/direction-stations", "/api/v1/direction-stations/**",
            "/api/v1/routes/**",
            "/api/vehicles",       "/api/vehicles/**",
            "/api/v1/reviews",     "/api/v1/reviews/**"
    );

    // Public for POST only (registration)
    private static final List<String> PUBLIC_POST = List.of(
            "/api/users",
            "/api/v1/users"
    );

    // ADMIN required for ANY method on these paths
    private static final List<String> ADMIN_ANY = List.of(
            "/api/v1/workflows", "/api/v1/workflows/**"
    );

    // ADMIN required for write methods (POST/PUT/DELETE/PATCH) on these paths
    private static final List<String> ADMIN_WRITE = List.of(
            "/api/v1/lines",       "/api/v1/lines/**",
            "/api/v1/stations",    "/api/v1/stations/**",
            "/api/v1/timetables",  "/api/v1/timetables/**",
            "/api/v1/directions",  "/api/v1/directions/**",
            "/api/v1/route-points","/api/v1/route-points/**",
            "/api/v1/direction-stations", "/api/v1/direction-stations/**",
            "/api/vehicles",       "/api/vehicles/**"
    );

    // ADMIN required for specific methods on feedback paths
    private static final List<String> ADMIN_PATCH = List.of(
            "/api/v1/reviews", "/api/v1/reviews/**"
    );

    private static final List<String> ADMIN_DELETE_OR_PATCH_REPORTS = List.of(
            "/api/v1/reports", "/api/v1/reports/**"
    );

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper,
                                   StringRedisTemplate redisTemplate) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase();

        // 1. Always public
        if (matchesAny(path, PUBLIC_ALWAYS)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Public GET
        if ("GET".equals(method) && matchesAny(path, PUBLIC_GET)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Public POST (registration)
        if ("POST".equals(method) && matchesExact(path, PUBLIC_POST)) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Validate JWT structure and signature
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            sendError(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Token is invalid or expired");
            return;
        }

        // 5. Check revocation via Redis
        Claims claims = jwtService.extractClaims(token);
        String jti = claims.getId();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))) {
            sendError(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Token has been revoked");
            return;
        }

        String userId = claims.getSubject();
        String fenceVal = redisTemplate.opsForValue().get(USER_LOGOUT_ALL_KEY + userId);
        if (fenceVal != null) {
            long issuedAt = claims.getIssuedAt().getTime() / 1000;
            if (issuedAt < Long.parseLong(fenceVal)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Token has been revoked");
                return;
            }
        }

        String role = claims.get("role", String.class);

        // 6. Role-based checks
        if (requiresAdmin(path, method) && !"ADMIN".equals(role)) {
            sendError(response, HttpStatus.FORBIDDEN, "forbidden", "Admin access required");
            return;
        }

        chain.doFilter(new HeaderEnrichedRequest(request, userId, role), response);
    }

    private boolean requiresAdmin(String path, String method) {
        if (matchesAny(path, ADMIN_ANY)) return true;
        if (WRITE_METHODS.contains(method) && matchesAny(path, ADMIN_WRITE)) return true;
        if ("PATCH".equals(method) && matchesAny(path, ADMIN_PATCH)) return true;
        if (("DELETE".equals(method) || "PATCH".equals(method)) && matchesAny(path, ADMIN_DELETE_OR_PATCH_REPORTS)) return true;
        return false;
    }

    private boolean matchesAny(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (matcher.match(pattern, path)) return true;
        }
        return false;
    }

    private boolean matchesExact(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (path.equals(pattern)) return true;
        }
        return false;
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static class HeaderEnrichedRequest extends HttpServletRequestWrapper {

        private final Map<String, String> extraHeaders;

        HeaderEnrichedRequest(HttpServletRequest request, String userId, String role) {
            super(request);
            extraHeaders = new HashMap<>();
            extraHeaders.put("X-User-Id", userId);
            extraHeaders.put("X-User-Role", role);
        }

        @Override
        public String getHeader(String name) {
            return extraHeaders.getOrDefault(name, super.getHeader(name));
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (extraHeaders.containsKey(name)) {
                return Collections.enumeration(List.of(extraHeaders.get(name)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
