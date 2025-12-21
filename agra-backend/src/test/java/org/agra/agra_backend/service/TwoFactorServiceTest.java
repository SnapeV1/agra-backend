package org.agra.agra_backend.service;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorServiceTest {

    private final TwoFactorService service = new TwoFactorService();

    @Test
    void generateSecretReturnsBase32() {
        String secret = service.generateSecret();

        assertThat(secret).isNotBlank();
        assertThat(secret).doesNotContain("=");
    }

    @Test
    void generateRecoveryCodesReturnsEightValues() {
        List<String> codes = service.generateRecoveryCodes();

        assertThat(codes).hasSize(8);
        assertThat(codes.get(0)).isNotBlank();
    }

    @Test
    void buildOtpAuthUrlUsesDefaultUser() {
        String url = service.buildOtpAuthUrl("SECRET", null);

        assertThat(url).contains("admin");
        assertThat(url).contains("secret=SECRET");
    }

    @Test
    void verifyCodeRejectsBlank() {
        assertThat(service.verifyCode(null, "123")).isFalse();
        assertThat(service.verifyCode("SECRET", " ")).isFalse();
    }

    @Test
    void verifyCodeAcceptsGeneratedCode() throws Exception {
        String secret = "JBSWY3DPEHPK3PXP";
        String code = generateCode(secret, Instant.now().getEpochSecond() / 30);

        assertThat(service.verifyCode(secret, code)).isTrue();
    }

    private static String generateCode(String base32Secret, long timeStep) throws Exception {
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
        int otp = binary % (int) Math.pow(10, 6);
        return String.format("%06d", otp);
    }
}
