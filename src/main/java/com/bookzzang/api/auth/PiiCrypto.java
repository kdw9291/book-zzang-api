package com.bookzzang.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class PiiCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] encryptionKey;
    private final byte[] lookupKey;

    PiiCrypto(@Value("${shelfie.auth.credentials.pii-encryption-key:}") String encryptionKey,
              @Value("${shelfie.auth.credentials.email-lookup-key:}") String lookupKey) {
        this.encryptionKey = decodeKey(encryptionKey, "PII_ENCRYPTION_KEY");
        this.lookupKey = decodeKey(lookupKey, "PII_EMAIL_LOOKUP_KEY");
    }

    String encrypt(String value) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length); System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) { throw new IllegalStateException("PII encryption failed", e); }
    }

    String emailLookupHash(String email) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(lookupKey, "HmacSHA256"));
            return hex(mac.doFinal(normalizeEmail(email).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("email lookup hash failed", e); }
    }

    String normalizeEmail(String email) { return email.trim().toLowerCase(java.util.Locale.ROOT); }
    String sha256(String value) {
        try { return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private byte[] decodeKey(String key, String name) {
        if (!StringUtils.hasText(key)) throw new IllegalStateException(name + " must be configured");
        try { byte[] decoded = Base64.getDecoder().decode(key.trim()); if (decoded.length < 32) throw new IllegalArgumentException(); return decoded; }
        catch (IllegalArgumentException e) { throw new IllegalStateException(name + " must be a Base64-encoded 32-byte key"); }
    }
    private String hex(byte[] bytes) { StringBuilder result = new StringBuilder(bytes.length * 2); for (byte b : bytes) result.append(String.format("%02x", b)); return result.toString(); }
}
