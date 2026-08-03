package com.dbtraining.reconx.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV072 — JwtTokenProvider generate/parse round trip. */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256-256bit";

    private JwtTokenProvider provider() {
        return new JwtTokenProvider(SECRET, 15, "reconx");
    }

    @Test
    void generate_thenParse_roundTripsSubjectAndRole() {
        JwtTokenProvider provider = provider();

        String token = provider.generate("trader@db.com", "TRADER");
        Claims claims = provider.parse(token);

        assertThat(claims.getSubject()).isEqualTo("trader@db.com");
        assertThat(claims.get("role")).isEqualTo("TRADER");
        assertThat(claims.getIssuer()).isEqualTo("reconx");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void expirationSeconds_matchesConfiguredMinutes() {
        assertThat(provider().expirationSeconds()).isEqualTo(15 * 60);
    }
}
