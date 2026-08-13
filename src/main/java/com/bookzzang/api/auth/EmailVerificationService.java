package com.bookzzang.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
class EmailVerificationService {
    private final EmailVerificationRepository repository;
    private final SesEmailSender sender;
    private final String secret;
    private final Duration ttl;
    private final Duration resendCooldown;
    private final SecureRandom random = new SecureRandom();

    EmailVerificationService(EmailVerificationRepository repository, SesEmailSender sender,
                             @Value("${shelfie.auth.email-verification.secret:}") String secret,
                             @Value("${shelfie.auth.email-verification.code-ttl-minutes}") long ttlMinutes,
                             @Value("${shelfie.auth.email-verification.resend-cooldown-seconds}") long resendCooldownSeconds) {
        this.repository = repository;
        this.sender = sender;
        this.secret = secret;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
    }

    void requestCode(String rawEmail) {
        ensureConfigured();
        String email = normalize(rawEmail);
        repository.find(email).ifPresent(record -> {
            if (record.sentAt().plus(resendCooldown).isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요.");
            }
        });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        repository.saveCode(email, hash(email + ":" + code), Instant.now().plus(ttl));
        sender.sendVerificationCode(email, code);
    }

    String confirmCode(String rawEmail, String code) {
        ensureConfigured();
        String email = normalize(rawEmail);
        if (!code.matches("\\d{6}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증코드 6자리를 입력해 주세요.");
        String token = UUID.randomUUID().toString();
        if (!repository.confirm(email, hash(email + ":" + code), hash(token))) {
            repository.addFailedAttempt(email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않거나 만료되었습니다.");
        }
        return token;
    }

    void requireVerified(String rawEmail, String token) {
        ensureConfigured();
        String email = normalize(rawEmail);
        if (!StringUtils.hasText(token) || !repository.isUsable(email, hash(token))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 인증을 완료해 주세요.");
        }
    }

    void consume(String rawEmail, String token) { repository.consume(normalize(rawEmail), hash(token)); }

    private void ensureConfigured() {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "email verification is not configured");
        }
    }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((value + ":" + secret).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
