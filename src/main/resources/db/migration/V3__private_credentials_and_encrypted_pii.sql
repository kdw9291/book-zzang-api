alter table app_users add column if not exists email_ciphertext text;
alter table app_users add column if not exists display_name_ciphertext text;
alter table app_users add column if not exists email_lookup_hash varchar(64);
alter table app_users alter column display_name drop not null;

create unique index if not exists app_users_email_lookup_hash_uq on app_users (email_lookup_hash) where email_lookup_hash is not null;

create table user_credentials (
  user_id uuid primary key references app_users(id) on delete cascade,
  password_hash text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table auth_sessions (
  id uuid primary key,
  user_id uuid not null references app_users(id) on delete cascade,
  access_token_hash varchar(64) not null unique,
  access_expires_at timestamptz not null,
  refresh_token_hash varchar(64) not null unique,
  refresh_expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index auth_sessions_access_idx on auth_sessions (access_token_hash, access_expires_at) where revoked_at is null;
create index auth_sessions_refresh_idx on auth_sessions (refresh_token_hash, refresh_expires_at) where revoked_at is null;
create trigger user_credentials_set_updated_at before update on user_credentials for each row execute function set_updated_at();
create trigger auth_sessions_set_updated_at before update on auth_sessions for each row execute function set_updated_at();
