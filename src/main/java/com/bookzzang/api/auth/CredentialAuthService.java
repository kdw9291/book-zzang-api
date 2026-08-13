package com.bookzzang.api.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CredentialAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private final CredentialRepository repository;
    private final PiiCrypto crypto;
    private final PasswordEncoder passwords = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    CredentialAuthService(CredentialRepository repository, PiiCrypto crypto) { this.repository = repository; this.crypto = crypto; }

    boolean emailAvailable(String email) { return !repository.emailExists(crypto.emailLookupHash(email)); }

    @Transactional void signUp(String email, String password, String nickname, String gender, Integer ageGroup) {
        String emailHash = crypto.emailLookupHash(email);
        if (repository.emailExists(emailHash)) throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        UUID userId = UUID.randomUUID();
        repository.createUser(userId, crypto.encrypt(crypto.normalizeEmail(email)), crypto.encrypt(nickname.trim()), emailHash,
                gender == null ? "PREFER_NOT_TO_SAY" : gender, ageGroup, passwords.encode(password));
    }
    @Transactional AuthSession login(String email, String password) {
        CredentialRepository.Credential credential = repository.findCredential(crypto.emailLookupHash(email)).orElseThrow(this::invalidCredentials);
        if (!passwords.matches(password, credential.passwordHash())) throw invalidCredentials();
        return issueSession(credential.userId());
    }
    @Transactional AuthSession refresh(String refreshToken) {
        UUID userId = repository.findUserByRefreshToken(crypto.sha256(refreshToken)).orElseThrow(this::invalidCredentials);
        repository.revokeByRefreshToken(crypto.sha256(refreshToken)); return issueSession(userId);
    }
    void logout(String refreshToken) { repository.revokeByRefreshToken(crypto.sha256(refreshToken)); }
    public java.util.Optional<UUID> authenticatedUser(String accessToken) { return repository.findUserByAccessToken(crypto.sha256(accessToken)); }
    private AuthSession issueSession(UUID userId) {
        String access = randomToken(); String refresh = randomToken(); Instant now = Instant.now();
        repository.createSession(UUID.randomUUID(), userId, crypto.sha256(access), now.plus(ACCESS_TTL), crypto.sha256(refresh), now.plus(REFRESH_TTL));
        return new AuthSession(access, refresh, ACCESS_TTL.toSeconds());
    }
    private String randomToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private ResponseStatusException invalidCredentials() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"); }
    record AuthSession(String accessToken, String refreshToken, long expiresIn) { }
}
