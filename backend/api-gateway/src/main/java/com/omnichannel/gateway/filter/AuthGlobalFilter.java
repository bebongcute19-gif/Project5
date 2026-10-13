package com.omnichannel.gateway.filter;

import com.omnichannel.gateway.security.JwtValidator;
import com.omnichannel.gateway.security.RouteValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;
    private final RouteValidator routeValidator;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Sanitize incoming request by stripping client-supplied identity headers
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("X-User-Id");
                    httpHeaders.remove("X-User-Roles");
                    httpHeaders.remove("X-User-Email");
                    httpHeaders.remove("X-User-Username");
                });

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        boolean isPublic = routeValidator.isPublic(request);

        // 2. Handle Public routes
        if (isPublic) {
            if (StringUtils.hasText(token) && jwtValidator.validateToken(token)) {
                // If valid token is supplied on public endpoint, still propagate verified context
                injectUserHeaders(requestBuilder, token);
            }
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // 3. Handle Protected routes
        if (!StringUtils.hasText(token) || !jwtValidator.validateToken(token)) {
            return onError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Missing, invalid or expired Bearer token",
                    "https://omnichannel.com/errors/unauthorized"
            );
        }

        // 4. Check Role Based Access Control
        List<String> roles = jwtValidator.getRoles(token);
        if (routeValidator.isAdminOnly(request)) {
            boolean isAdmin = roles.stream().anyMatch(role ->
                    "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)
            );
            if (!isAdmin) {
                return onError(
                        exchange,
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "Access denied: Administrator role required",
                        "https://omnichannel.com/errors/forbidden"
                );
            }
        }

        // 5. Inject verified identity headers downstream
        injectUserHeaders(requestBuilder, token);

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    private void injectUserHeaders(ServerHttpRequest.Builder requestBuilder, String token) {
        String userId = jwtValidator.getUserId(token);
        String username = jwtValidator.getUsername(token);
        String email = jwtValidator.getEmail(token);
        List<String> roles = jwtValidator.getRoles(token);

        if (userId != null) {
            requestBuilder.header("X-User-Id", userId);
        }
        if (username != null) {
            requestBuilder.header("X-User-Username", username);
        }
        if (email != null) {
            requestBuilder.header("X-User-Email", email);
        }
        if (roles != null && !roles.isEmpty()) {
            requestBuilder.header("X-User-Roles", String.join(",", roles));
        }
    }

    private Mono<Void> onError(
            ServerWebExchange exchange,
            HttpStatus status,
            String title,
            String detail,
            String errorType) {

        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = String.format(
                "{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\"}",
                errorType, title, status.value(), detail, path
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
