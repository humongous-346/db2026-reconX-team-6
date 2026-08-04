package com.dbtraining.reconx.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV074 — SecurityConfig's BCrypt PasswordEncoder bean. */
class SecurityConfigTest {

    @Test
    void passwordEncoder_hashesAndMatchesRoundTrip() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        String hash = encoder.encode("trader123");

        assertThat(hash).isNotEqualTo("trader123");
        assertThat(encoder.matches("trader123", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }
}
