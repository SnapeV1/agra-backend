package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.RefreshTokenRepository;
import org.agra.agra_backend.model.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Test
    void createRefreshTokenRequiresUserId() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);

        assertThatThrownBy(() -> service.createRefreshToken(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createRefreshTokenPersistsToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshExpirationMinutes", 5);

        String token = service.createRefreshToken("user-1");

        assertThat(token).isNotBlank();
        verify(repository).deleteByUserId("user-1");
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void validateRefreshTokenRequiresToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);

        assertThatThrownBy(() -> service.validateRefreshToken(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateRefreshTokenRejectsUnknownToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        RefreshTokenService service = new RefreshTokenService(repository);

        assertThatThrownBy(() -> service.validateRefreshToken("token"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateRefreshTokenRejectsExpiredToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(new Date(System.currentTimeMillis() - 1000));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        RefreshTokenService service = new RefreshTokenService(repository);

        assertThatThrownBy(() -> service.validateRefreshToken("token"))
                .isInstanceOf(RuntimeException.class);
        verify(repository).delete(token);
    }

    @Test
    void validateRefreshTokenRejectsRevokedToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(new Date(System.currentTimeMillis() + 1000));
        token.setRevoked(true);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        RefreshTokenService service = new RefreshTokenService(repository);

        assertThatThrownBy(() -> service.validateRefreshToken("token"))
                .isInstanceOf(RuntimeException.class);
        verify(repository).delete(token);
    }

    @Test
    void rotateRefreshTokenRevokesAndCreatesNew() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = spy(new RefreshTokenService(repository));
        ReflectionTestUtils.setField(service, "refreshExpirationMinutes", 5);
        RefreshToken existing = new RefreshToken();
        existing.setUserId("user-1");

        String token = service.rotateRefreshToken(existing);

        assertThat(token).isNotBlank();
        verify(repository).save(existing);
        verify(repository).deleteByUserId("user-1");
    }

    @Test
    void revokeByTokenNoopsWhenBlank() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);

        service.revokeByToken(" ");

        verifyNoInteractions(repository);
    }

    @Test
    void revokeByTokenRevokesExisting() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshToken token = new RefreshToken();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        RefreshTokenService service = new RefreshTokenService(repository);

        service.revokeByToken("token");

        verify(repository).save(token);
    }

    @Test
    void revokeAllForUserNoopsWhenBlank() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);

        service.revokeAllForUser(" ");

        verifyNoInteractions(repository);
    }

    @Test
    void revokeAllForUserDeletesTokens() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository);

        service.revokeAllForUser("user-1");

        verify(repository).deleteByUserId("user-1");
    }
}
