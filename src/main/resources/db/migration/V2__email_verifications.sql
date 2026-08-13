create table email_verifications (
  normalized_email text primary key check (normalized_email = lower(normalized_email)),
  code_hash text not null,
  verification_token_hash text,
  expires_at timestamptz not null,
  sent_at timestamptz not null default now(),
  verified_at timestamptz,
  consumed_at timestamptz,
  send_count integer not null default 1,
  attempt_count integer not null default 0,
  created_at timestamptz not null default now()
);

create index email_verifications_expiry_idx on email_verifications (expires_at);
