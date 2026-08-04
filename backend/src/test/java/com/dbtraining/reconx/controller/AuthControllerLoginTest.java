package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.LoginRequest;
import com.dbtraining.reconx.dto.LoginResponse;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.AppUserRepository;
import com.dbtraining.reconx.repository.entity.AppUser;
import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** TICKET-ADV072 — POST /api/auth/login. */
@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock private AppUserRepository users;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtTokenProvider jwt;
    @Mock private AppUser user;

    @Test
    void login_validCredentials_returnsToken() {
        AuthController controller = new AuthController(users, encoder, jwt);
        when(users.findByEmail("trader@db.com")).thenReturn(Optional.of(user));
        when(user.getEnabled()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("hashed");
        when(encoder.matches("trader123", "hashed")).thenReturn(true);
        when(user.getEmail()).thenReturn("trader@db.com");
        when(user.getRole()).thenReturn("TRADER");
        when(jwt.generate("trader@db.com", "TRADER")).thenReturn("a.b.c");
        when(jwt.expirationSeconds()).thenReturn(900L);

        ResponseEntity<LoginResponse> response = controller.login(new LoginRequest("trader@db.com", "trader123"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().token()).isEqualTo("a.b.c");
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().role()).isEqualTo("TRADER");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        AuthController controller = new AuthController(users, encoder, jwt);
        when(users.findByEmail("trader@db.com")).thenReturn(Optional.of(user));
        when(user.getEnabled()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("hashed");
        when(encoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(new LoginRequest("trader@db.com", "wrong")))
                .isInstanceOf(InvalidTradeException.class);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        AuthController controller = new AuthController(users, encoder, jwt);
        when(users.findByEmail("nobody@db.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(new LoginRequest("nobody@db.com", "whatever")))
                .isInstanceOf(InvalidTradeException.class);
    }
}
