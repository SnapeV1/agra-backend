package org.agra.agra_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import org.agra.agra_backend.dao.RefreshTokenRepository;
import org.agra.agra_backend.model.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.refresh.expirationMinutes:43200}") // default 30 days
    private int refreshExpirationMinutes;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("User id is required");
        }
        refreshTokenRepository.deleteByUserId(userId);
        String rawToken = generateSecureToken();
        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(sha256(rawToken));
        rt.setUserId(userId);
        rt.setCreatedAt(new Date());
        rt.setExpiresAt(minutesFromNow(refreshExpirationMinutes));
        refreshTokenRepository.save(rt);
        return rawToken;
    }

    public RefreshToken validateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Refresh token is required");
        }
        String hash = sha256(rawToken);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHash(hash);
        if (opt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }
        RefreshToken token = opt.get();
        if (token.isExpired() || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired or revoked");
        }
        return token;
    }

    public String rotateRefreshToken(RefreshToken existing) {
        existing.setRevoked(true);
        existing.setRotatedAt(new Date());
        refreshTokenRepository.save(existing);
        return createRefreshToken(existing.getUserId());
    }

    public void revokeByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRotatedAt(new Date());
            refreshTokenRepository.save(token);
        });
    }

    public void revokeAllForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Date minutesFromNow(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
