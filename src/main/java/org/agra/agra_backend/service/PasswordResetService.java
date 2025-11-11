package org.agra.agra_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.dao.PasswordResetTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.PasswordResetToken;
import org.agra.agra_backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public void initiateReset(String emailRaw) throws MessagingException {
        if (emailRaw == null) return; // do nothing
        String email = emailRaw.toLowerCase().trim();

        User user = userRepository.findByEmail(email);
        if (user == null) {
            // Always respond the same for privacy
            return;
        }

        // Remove existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setTokenHash(tokenHash);
        prt.setUserId(user.getId());
        prt.setCreatedAt(new Date());
        prt.setExpirationDate(minutesFromNow(30));
        tokenRepository.save(prt);

        sendResetEmail(user.getEmail(), rawToken);
    }

    /**
     * Create a password reset token for the given user id and return the RAW token.
     * Does not send an email. Intended for immediate handoff to a trusted client flow
     * (e.g., new Google sign-in account completing password setup).
     */
    public String issueResetTokenForUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("User id is required");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Remove any existing tokens for this user
        tokenRepository.deleteByUserId(userId);

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setTokenHash(tokenHash);
        prt.setUserId(userId);
        prt.setCreatedAt(new Date());
        prt.setExpirationDate(minutesFromNow(30));
        tokenRepository.save(prt);

        log.debug("PasswordReset: Issued reset token for userId={}", userId);
        return rawToken;
    }

    private void sendResetEmail(String to, String rawToken) throws MessagingException {
        String resetUrl = frontendBaseUrl.replaceAll("/$", "") + "/reset-password?token=" + urlSafe(rawToken);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("Reset Your Password");

        String htmlContent = """
        <html>
          <body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: #2c7be5;">Password Reset Request</h2>
              <p>Hi there,</p>
              <p>We received a request to reset your password. You can create a new password by clicking the button below:</p>
              <p style="text-align: center;">
                <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2c7be5; color: white; text-decoration: none; border-radius: 5px;">
                  Reset Password
                </a>
              </p>
              <p>If the button doesn’t work, copy and paste this link into your browser:</p>
              <p><a href="%s">%s</a></p>
              <hr style="border: none; border-top: 1px solid #eee;" />
              <p style="font-size: 12px; color: #777;">If you didn’t request a password reset, you can safely ignore this email.</p>
              <p style="font-size: 12px; color: #777;">Thank you,<br>YEFFA Support Team</p>
            </div>
          </body>
        </html>
        """.formatted(resetUrl, resetUrl, resetUrl);

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }


    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);
        log.info("PasswordReset: Attempting password reset. tokenHash={}", tokenHash);
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenHash(tokenHash);
        if (opt.isEmpty()) {
            log.warn("PasswordReset: No reset token found for tokenHash={}", tokenHash);
            throw new RuntimeException("Invalid token");
        }
        PasswordResetToken resetToken = opt.get();

        if (resetToken.isExpired()) {
            log.warn("PasswordReset: Token expired for userId={} createdAt={} expiresAt={}",
                    resetToken.getUserId(), resetToken.getCreatedAt(), resetToken.getExpirationDate());
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token expired");
        }

        User user = userRepository.findById(resetToken.getUserId()).orElse(null);
        if (user == null) {
            log.error("PasswordReset: User not found for tokenHash={} userId={}", tokenHash, resetToken.getUserId());
            tokenRepository.delete(resetToken);
            throw new RuntimeException("User not found for token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("PasswordReset: Password updated for userId={}", user.getId());

        tokenRepository.delete(resetToken);
        log.debug("PasswordReset: Reset token deleted for userId={}", user.getId());
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private static Date minutesFromNow(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    private static String urlSafe(String token) {
        return token; // already URL-safe Base64 (URL variant)
    }
}
