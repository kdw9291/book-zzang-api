-- Verification codes are short-lived. Discard any old plaintext-address records before renaming the key column.
truncate table email_verifications;
alter table email_verifications rename column normalized_email to email_lookup_hash;
alter table email_verifications drop constraint if exists email_verifications_normalized_email_check;
