package com.bookzzang.api.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class EmailVerificationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final PiiCrypto crypto;
    EmailVerificationRepository(NamedParameterJdbcTemplate jdbc, PiiCrypto crypto) { this.jdbc = jdbc; this.crypto = crypto; }
    Optional<VerificationRecord> find(String email) {
        return jdbc.query("select code_hash, verification_token_hash, expires_at, sent_at, verified_at, consumed_at, attempt_count from email_verifications where email_lookup_hash=:email", params(email),
                (rs, row) -> new VerificationRecord(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant(), rs.getTimestamp(4).toInstant(), rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toInstant(), rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toInstant(), rs.getInt(7))).stream().findFirst();
    }
    void saveCode(String email, String codeHash, Instant expiresAt) {
        Map<String, Object> values = params(email); values.put("hash", codeHash); values.put("expires", Timestamp.from(expiresAt));
        jdbc.update("insert into email_verifications (email_lookup_hash,code_hash,expires_at,sent_at,verification_token_hash,verified_at,consumed_at,send_count,attempt_count) values (:email,:hash,:expires,now(),null,null,null,1,0) on conflict (email_lookup_hash) do update set code_hash=excluded.code_hash,expires_at=excluded.expires_at,sent_at=now(),verification_token_hash=null,verified_at=null,consumed_at=null,send_count=email_verifications.send_count+1,attempt_count=0", values);
    }
    boolean confirm(String email, String codeHash, String tokenHash) { Map<String, Object> values = params(email); values.put("code", codeHash); values.put("token", tokenHash); return jdbc.update("update email_verifications set verified_at=now(),verification_token_hash=:token where email_lookup_hash=:email and code_hash=:code and expires_at > now() and verified_at is null and attempt_count < 5", values) == 1; }
    void addFailedAttempt(String email) { jdbc.update("update email_verifications set attempt_count=attempt_count+1 where email_lookup_hash=:email", params(email)); }
    boolean isUsable(String email, String tokenHash) { Map<String, Object> values = params(email); values.put("token", tokenHash); return jdbc.queryForObject("select exists(select 1 from email_verifications where email_lookup_hash=:email and verification_token_hash=:token and verified_at is not null and consumed_at is null and expires_at > now())", values, Boolean.class); }
    void consume(String email, String tokenHash) { Map<String, Object> values = params(email); values.put("token", tokenHash); jdbc.update("update email_verifications set consumed_at=now() where email_lookup_hash=:email and verification_token_hash=:token and consumed_at is null", values); }
    private Map<String, Object> params(String email) { return new java.util.HashMap<>(Map.of("email", crypto.emailLookupHash(email))); }
    record VerificationRecord(String codeHash, String verificationTokenHash, Instant expiresAt, Instant sentAt, Instant verifiedAt, Instant consumedAt, int attemptCount) { }
}
