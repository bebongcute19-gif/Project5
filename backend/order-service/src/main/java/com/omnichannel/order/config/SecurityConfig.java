package com.omnichannel.order.config;

import com.omnichannel.order.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .exceptionHandling()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(authz -> authz
                        // Allow health checks and actuator endpoints without authentication
                        .requestMatchers("/health", "/health/**", "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/**").permitAll()
                        // Allow public read access to inventory by SKU (GET). Other inventory mutating APIs remain protected.
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/admin/**").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/*").permitAll()
                                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
