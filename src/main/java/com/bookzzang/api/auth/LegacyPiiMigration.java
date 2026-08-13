package com.bookzzang.api.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class LegacyPiiMigration implements ApplicationRunner {
    private final CredentialRepository repository;
    private final PiiCrypto crypto;
    LegacyPiiMigration(CredentialRepository repository, PiiCrypto crypto) { this.repository = repository; this.crypto = crypto; }
    @Override @Transactional public void run(ApplicationArguments args) {
        repository.legacyUsers().forEach(user -> repository.encryptLegacyUser(user.id(), crypto.encrypt(crypto.normalizeEmail(user.email())), crypto.encrypt(user.name()), crypto.emailLookupHash(user.email())));
    }
}
