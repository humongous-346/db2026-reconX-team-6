package com.dbtraining.reconx.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================================
 * TICKET-ADV073 — JwtAuthenticationFilter
 *
 * WHAT:    Reads `Authorization: Bearer <token>`, parses it via
 *          {@link JwtTokenProvider}, and sets the SecurityContext for the
 *          current request.
 * HOW:     Extends OncePerRequestFilter so it runs exactly once per request.
 *          On a bad / expired token the context is cleared (NOT a 401) —
 *          Spring's normal auth path turns the missing principal into a 401
 *          when a protected endpoint is hit.
 * WHY:     Stateless auth: every request carries its own credential.
 * OBSERVE: A request with a valid token populates SecurityContextHolder; the
 *          downstream controller can use @AuthenticationPrincipal etc.
 *
 * NOTE:    Always call chain.doFilter at the end — even on auth failure — so
 *          Spring's normal exception flow can produce a clean 401.
 * ============================================================================
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;

    public JwtAuthenticationFilter(JwtTokenProvider provider) { this.provider = provider; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // TODO(TICKET-ADV073): parse the Authorization header, populate the
        //                     SecurityContext, then call chain.doFilter.
        chain.doFilter(req, res);
    }
}
