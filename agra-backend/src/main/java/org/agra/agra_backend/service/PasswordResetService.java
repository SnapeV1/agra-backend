package org.agra.agra_backend.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.dao.PasswordResetTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.PasswordResetToken;
import org.agra.agra_backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${twilio.accountSid:}")
    private String twilioAccountSid;

    @Value("${twilio.authToken:}")
    private String twilioAuthToken;

    @Value("${twilio.verifyServiceSid:}")
    private String twilioVerifyServiceSid;

    public void initiateReset(String emailRaw) throws MessagingException {
        if (emailRaw == null) return; // do nothing
        String email = emailRaw.toLowerCase().trim();
        Locale locale = LocaleContextHolder.getLocale();

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

        sendResetEmail(user.getEmail(), rawToken, locale);
    }

    public void sendResetCodeSms(String phoneRaw) {
        if (phoneRaw == null || phoneRaw.isBlank()) {
            log.info("PasswordReset: SMS reset requested with empty phone");
            return;
        }
        String phone = phoneRaw.trim();

        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            // Preserve privacy: behave as if it succeeded
            log.info("PasswordReset: SMS reset requested for non-existent phone - no Twilio call made");
            return;
        }

        ensureTwilioConfigured();
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            Verification.creator(twilioVerifyServiceSid, phone, "sms").create();
            log.info("PasswordReset: Sent SMS reset code to phone={}", phone);
        } catch (ApiException e) {
            // Handle gracefully without noisy stack traces
            log.warn("PasswordReset: Twilio send failed for phone={} msg={}", phone, e.getMessage());
            throw new RuntimeException(msg("error.reset.send.sms"));
        }
    }

    /**
     * Create a password reset token for the given user id and return the RAW token.
     * Does not send an email. Intended for immediate handoff to a trusted client flow
     * (e.g., new Google sign-in account completing password setup).
     */
    public String issueResetTokenForUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException(msg("error.user.id.required"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException(msg("error.user.notfound"));
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

    private void sendResetEmail(String to, String rawToken, Locale locale) throws MessagingException {
        String resetUrl = frontendBaseUrl.replaceAll("/$", "") + "/reset-password?token=" + urlSafe(rawToken);
        String subject = messageSource.getMessage("email.reset.subject", null, locale);
        String heading = messageSource.getMessage("email.reset.heading", null, locale);
        String greeting = messageSource.getMessage("email.reset.greeting", null, locale);
        String intro = messageSource.getMessage("email.reset.intro", null, locale);
        String buttonText = messageSource.getMessage("email.reset.button", null, locale);
        String fallback = messageSource.getMessage("email.reset.fallback", null, locale);
        String ignore = messageSource.getMessage("email.reset.ignore", null, locale);
        String signature = messageSource.getMessage("email.reset.signature", null, locale).replace("\\n", "<br>");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);

        String htmlContent = """
        <html>
          <body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: #2c7be5;">%s</h2>
              <p>%s</p>
              <p>%s</p>
              <p style="text-align: center;">
                <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2c7be5; color: white; text-decoration: none; border-radius: 5px;">
                  %s
                </a>
              </p>
              <p>%s</p>
              <p><a href="%s">%s</a></p>
              <hr style="border: none; border-top: 1px solid #eee;" />
              <p style="font-size: 12px; color: #777;">%s</p>
              <p style="font-size: 12px; color: #777;">%s</p>
            </div>
          </body>
        </html>
        """.formatted(
                heading,
                greeting,
                intro,
                resetUrl,
                buttonText,
                fallback,
                resetUrl,
                resetUrl,
                ignore,
                signature
        );

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }


    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);
        log.info("PasswordReset: Attempting password reset. tokenHash={}", tokenHash);
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenHash(tokenHash);
        if (opt.isEmpty()) {
            log.warn("PasswordReset: No reset token found for tokenHash={}", tokenHash);
            throw new RuntimeException(msg("error.invalid.token"));
        }
        PasswordResetToken resetToken = opt.get();

        if (resetToken.isExpired()) {
            log.warn("PasswordReset: Token expired for userId={} createdAt={} expiresAt={}",
                    resetToken.getUserId(), resetToken.getCreatedAt(), resetToken.getExpirationDate());
            tokenRepository.delete(resetToken);
            throw new RuntimeException(msg("error.token.expired"));
        }

        User user = userRepository.findById(resetToken.getUserId()).orElse(null);
        if (user == null) {
            log.error("PasswordReset: User not found for tokenHash={} userId={}", tokenHash, resetToken.getUserId());
            tokenRepository.delete(resetToken);
            throw new RuntimeException(msg("error.user.notfound.token"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("PasswordReset: Password updated for userId={}", user.getId());

        tokenRepository.delete(resetToken);
        log.debug("PasswordReset: Reset token deleted for userId={}", user.getId());
    }

    public String createResetTokenWithSmsCode(String phoneRaw, String code) {
        if (phoneRaw == null || phoneRaw.isBlank()) {
            throw new RuntimeException(msg("error.phone.required"));
        }
        if (code == null || code.isBlank()) {
            throw new RuntimeException(msg("error.code.required"));
        }

        String phone = phoneRaw.trim();
        ensureTwilioConfigured();
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            VerificationCheck check = VerificationCheck.creator(twilioVerifyServiceSid)
                    .setTo(phone)
                    .setCode(code)
                    .create();

            if (!"approved".equalsIgnoreCase(check.getStatus())) {
                log.warn("PasswordReset: SMS code not approved for phone={} status={}", phone, check.getStatus());
                throw new RuntimeException(msg("error.invalid.verification.code"));
            }
        } catch (ApiException e) {
            // Handle gracefully without noisy stack traces
            log.warn("PasswordReset: Twilio verification failed for phone={} msg={}", phone, e.getMessage());
            throw new RuntimeException(msg("error.verify.failed"));
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException(msg("error.user.notfound.phone")));

        // Remove old tokens and issue a fresh reset token (same flow as email)
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setTokenHash(tokenHash);
        prt.setUserId(user.getId());
        prt.setCreatedAt(new Date());
        prt.setExpirationDate(minutesFromNow(30));
        tokenRepository.save(prt);

        log.info("PasswordReset: Issued reset token via SMS verification for userId={}", user.getId());
        return rawToken;
    }

    public String testTwilioConnection() {
        ensureTwilioConfigured();
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            com.twilio.rest.verify.v2.Service svc = com.twilio.rest.verify.v2.Service.fetcher(twilioVerifyServiceSid).fetch();
            log.info("PasswordReset: Twilio Verify service fetch successful name={} sid={}", svc.getFriendlyName(), svc.getSid());
            return svc.getFriendlyName();
        } catch (ApiException e) {
            log.error("PasswordReset: Twilio connectivity test failed - {}", e.getMessage());
            throw new RuntimeException(messageSource.getMessage("error.twilio.connectivity", new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()));
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(msg("error.hash.failed"));
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

    private void ensureTwilioConfigured() {
        if (twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()
                || twilioVerifyServiceSid == null || twilioVerifyServiceSid.isBlank()) {
            throw new RuntimeException(msg("error.twilio.not.configured"));
        }
    }

    private String msg(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

}
