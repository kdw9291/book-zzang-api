package com.bookzzang.api.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CredentialRepository {
    private final NamedParameterJdbcTemplate jdbc;
    CredentialRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    boolean emailExists(String emailHash) { return jdbc.queryForObject("select exists(select 1 from app_users where email_lookup_hash=:hash)", Map.of("hash", emailHash), Boolean.class); }
    Optional<Credential> findCredential(String emailHash) {
        return jdbc.query("select u.id, c.password_hash from app_users u join user_credentials c on c.user_id=u.id where u.email_lookup_hash=:hash", Map.of("hash", emailHash),
                (rs, row) -> new Credential(UUID.fromString(rs.getString("id")), rs.getString("password_hash"))).stream().findFirst();
    }
    void createUser(UUID userId, String emailCiphertext, String nicknameCiphertext, String emailHash, String gender, Integer ageGroup, String passwordHash) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId); params.put("email", emailCiphertext); params.put("nickname", nicknameCiphertext);
        params.put("lookup", emailHash); params.put("gender", gender); params.put("ageGroup", ageGroup);
        jdbc.update("insert into app_users (id,email_ciphertext,display_name_ciphertext,email_lookup_hash,gender,age_group) values (:id,:email,:nickname,:lookup,cast(:gender as user_gender),:ageGroup)", params);
        jdbc.update("insert into user_credentials (user_id,password_hash) values (:id,:passwordHash)", Map.of("id", userId, "passwordHash", passwordHash));
        jdbc.update("insert into shelves (user_id,name) values (:id,:name)", Map.of("id", userId, "name", "나의 책장"));
    }
    Optional<UUID> findUserByAccessToken(String accessHash) {
        return jdbc.query("select user_id from auth_sessions where access_token_hash=:hash and access_expires_at > now() and revoked_at is null", Map.of("hash", accessHash),
                (rs, row) -> UUID.fromString(rs.getString("user_id"))).stream().findFirst();
    }
    Optional<UUID> findUserByRefreshToken(String refreshHash) {
        return jdbc.query("select user_id from auth_sessions where refresh_token_hash=:hash and refresh_expires_at > now() and revoked_at is null", Map.of("hash", refreshHash),
                (rs, row) -> UUID.fromString(rs.getString("user_id"))).stream().findFirst();
    }
    void createSession(UUID sessionId, UUID userId, String accessHash, Instant accessExpiry, String refreshHash, Instant refreshExpiry) {
        jdbc.update("insert into auth_sessions (id,user_id,access_token_hash,access_expires_at,refresh_token_hash,refresh_expires_at) values (:id,:user,:access,:accessExpiry,:refresh,:refreshExpiry)",
                Map.of("id", sessionId, "user", userId, "access", accessHash, "accessExpiry", Timestamp.from(accessExpiry), "refresh", refreshHash, "refreshExpiry", Timestamp.from(refreshExpiry)));
    }
    void revokeByRefreshToken(String refreshHash) { jdbc.update("update auth_sessions set revoked_at=now() where refresh_token_hash=:hash and revoked_at is null", Map.of("hash", refreshHash)); }
    List<LegacyUser> legacyUsers() {
        return jdbc.query("select id, normalized_email, display_name from app_users where email_ciphertext is null and normalized_email is not null", Map.of(),
                (rs, row) -> new LegacyUser(UUID.fromString(rs.getString("id")), rs.getString("normalized_email"), rs.getString("display_name")));
    }
    void encryptLegacyUser(UUID id, String emailCiphertext, String nameCiphertext, String emailHash) {
        jdbc.update("update app_users set email_ciphertext=:email, display_name_ciphertext=:name, email_lookup_hash=:lookup, normalized_email=null, display_name=null where id=:id",
                Map.of("id", id, "email", emailCiphertext, "name", nameCiphertext, "lookup", emailHash));
    }
    record Credential(UUID userId, String passwordHash) { }
    record LegacyUser(UUID id, String email, String name) { }
}
