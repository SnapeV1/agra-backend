package org.agra.agra_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.dao.EmailVerificationTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.EmailVerificationToken;
import org.agra.agra_backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int EXPIRATION_MINUTES = 60; // 1 hour

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Resend a verification email using the user's email address. No-op (but returns OK)
     * for unknown emails to preserve privacy.
     */
    public void resendVerification(String emailRaw) {
        if (emailRaw == null || emailRaw.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        String email = emailRaw.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.info("Resend verification requested for non-existent email={}", email);
            return;
        }
        User user = userOpt.get();
        sendVerificationEmail(user);
    }

    /**
     * Issue a new verification token and email the verification link.
     */
    public void sendVerificationEmail(User user) {
        if (user == null || user.getEmail() == null || user.getId() == null) {
            log.warn("Email verification skipped: missing user/id/email");
            return;
        }
        if (Boolean.TRUE.equals(user.getVerified())) {
            log.debug("Email verification skipped: user already verified id={}", user.getId());
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setTokenHash(tokenHash);
        evt.setUserId(user.getId());
        evt.setCreatedAt(new Date());
        evt.setExpirationDate(minutesFromNow(EXPIRATION_MINUTES));
        tokenRepository.save(evt);

        try {
            sendEmail(user.getEmail(), rawToken);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            // Do not propagate to avoid blocking signup; token remains valid for manual resend if needed.
        }
    }

    /**
     * Verify the provided token and mark the associated user as verified.
     */
    public void verifyToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Verification token is required");
        }
        String tokenHash = sha256(rawToken);
        Optional<EmailVerificationToken> opt = tokenRepository.findByTokenHash(tokenHash);
        if (opt.isEmpty()) {
            throw new RuntimeException("Invalid verification token");
        }

        EmailVerificationToken token = opt.get();
        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new RuntimeException("Verification token expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for token"));

        user.setVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        log.info("Email verified for userId={}", user.getId());
    }

    private void sendEmail(String to, String rawToken) throws MessagingException {
        String verificationUrl = frontendBaseUrl.replaceAll("/$", "") + "/verify-email?token=" + urlSafe(rawToken);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("Verify your email address");

        String htmlContent = """
        <html>
          <body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: #2c7be5;">Confirm your email</h2>
              <p>Welcome! Please confirm your email address to secure your account.</p>
              <p style="text-align: center;">
                <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2c7be5; color: white; text-decoration: none; border-radius: 5px;">
                  Verify Email
                </a>
              </p>
              <p>If the button doesn't work, copy and paste this link into your browser:</p>
              <p><a href="%s">%s</a></p>
              <p style="font-size: 12px; color: #777;">This link expires in 1 hour.</p>
              <p style="font-size: 12px; color: #777;">If you didn't create an account, you can ignore this email.</p>
            </div>
          </body>
        </html>
        """.formatted(verificationUrl, verificationUrl, verificationUrl);

        helper.setText(htmlContent, true);
        mailSender.send(message);
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
        return token;
    }
}
