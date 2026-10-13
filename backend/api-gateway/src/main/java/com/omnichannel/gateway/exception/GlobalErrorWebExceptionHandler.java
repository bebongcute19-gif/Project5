package com.omnichannel.gateway.exception;

import io.netty.channel.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String title = "Internal Server Error";
        String detail = "An unexpected error occurred in API Gateway.";
        String errorType = "https://omnichannel.com/errors/internal-server-error";

        if (ex instanceof ConnectException || ex instanceof ConnectTimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            title = "Service Unavailable";
            detail = "The downstream service is currently unavailable or unreachable.";
            errorType = "https://omnichannel.com/errors/service-unavailable";
            log.warn("Downstream connection failure for path {}: {}", exchange.getRequest().getURI().getPath(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            title = status.getReasonPhrase();
            detail = rse.getReason() != null ? rse.getReason() : "Request failed with status " + status.value();
            if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                errorType = "https://omnichannel.com/errors/service-unavailable";
            } else if (status == HttpStatus.NOT_FOUND) {
                errorType = "https://omnichannel.com/errors/not-found";
            } else if (status == HttpStatus.UNAUTHORIZED) {
                errorType = "https://omnichannel.com/errors/unauthorized";
            } else if (status == HttpStatus.FORBIDDEN) {
                errorType = "https://omnichannel.com/errors/forbidden";
            }
        } else {
            log.error("Unhandled gateway error for path {}", exchange.getRequest().getURI().getPath(), ex);
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
