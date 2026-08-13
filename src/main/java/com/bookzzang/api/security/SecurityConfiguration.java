package com.bookzzang.api.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OpaqueTokenAuthenticationFilter tokenFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) -> {
                    String uri = context.getRequest().getRequestURI();
                    if (context.getRequest().getDispatcherType() == DispatcherType.ERROR || uri.equals("/actuator/health") || uri.startsWith("/api/public/")) return new AuthorizationDecision(true);
                    var current = authentication.get();
                    return new AuthorizationDecision(current != null && current.isAuthenticated() && !(current instanceof AnonymousAuthenticationToken));
                }));
        return http.build();
    }
}
