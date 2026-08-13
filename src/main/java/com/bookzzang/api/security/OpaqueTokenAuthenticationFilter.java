package com.bookzzang.api.security;

import com.bookzzang.api.auth.CredentialAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {
    private final CredentialAuthService credentials;
    OpaqueTokenAuthenticationFilter(CredentialAuthService credentials) { this.credentials = credentials; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) credentials.authenticatedUser(header.substring(7).trim()).ifPresent(userId -> {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, AuthorityUtils.NO_AUTHORITIES);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
        chain.doFilter(request, response);
    }
}
