package com.dbtraining.reconx.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** TICKET-ADV073 — JwtAuthenticationFilter reads Bearer token and populates SecurityContext. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider provider;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private Claims claims;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_populatesSecurityContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token.value");
        when(provider.parse("good.token.value")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("trader@db.com");
        when(claims.get("role")).thenReturn("TRADER");

        new JwtAuthenticationFilter(provider).doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("trader@db.com");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_TRADER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_clearsContextAndStillContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer garbage");
        when(provider.parse("garbage")).thenThrow(new JwtException("bad signature"));

        new JwtAuthenticationFilter(provider).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_stillContinuesChainWithoutAuthenticating() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        new JwtAuthenticationFilter(provider).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(provider);
    }
}
