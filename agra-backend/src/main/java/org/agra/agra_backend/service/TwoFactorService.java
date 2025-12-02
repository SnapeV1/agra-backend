package org.agra.agra_backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);
    private static final int TIME_STEP_SECONDS = 30;
    private static final int WINDOW = 1; // allow +/- 1 step
    private static final int CODE_DIGITS = 6;
    private static final String ISSUER = "YEFFA";
    private final SecureRandom random = new SecureRandom();

    public String generateSecret() {
        byte[] buffer = new byte[20];
        random.nextBytes(buffer);
        return new Base32().encodeToString(buffer).replace("=", "");
    }

    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            byte[] buf = new byte[9];
            random.nextBytes(buf);
            codes.add(Base64.getUrlEncoder().withoutPadding().encodeToString(buf));
        }
        return codes;
    }

    public String buildOtpAuthUrl(String secret, String email) {
        String user = email == null ? "admin" : email;
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                ISSUER, user, secret, ISSUER, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.trim().isEmpty()) {
            return false;
        }
        String normalizedCode = code.trim();
        try {
            long currentInterval = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
            for (int i = -WINDOW; i <= WINDOW; i++) {
                String candidate = generateCode(secret, currentInterval + i);
                if (candidate.equals(normalizedCode)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Failed to verify TOTP code: {}", e.getMessage());
        }
        return false;
    }

    private String generateCode(String base32Secret, long timeStep) throws Exception {
        Base32 codec = new Base32();
        byte[] key = codec.decode(base32Secret);
        byte[] data = new byte[8];
        long value = timeStep;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }
}
