package com.omnichannel.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = Map.of(
                "service", "api-gateway",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
        return Mono.just(ResponseEntity.ok(healthInfo));
    }
}
