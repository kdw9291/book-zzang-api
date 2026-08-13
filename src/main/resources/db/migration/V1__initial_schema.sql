create extension if not exists pgcrypto;

create type user_gender as enum ('FEMALE', 'MALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY');
create type reading_status as enum ('WANT_TO_READ', 'READING', 'READ');
create type shelf_orientation as enum ('VERTICAL', 'HORIZONTAL');
create type thickness_source as enum ('MEASURED', 'PAGE_ESTIMATED', 'DEFAULT', 'USER_CORRECTED');
create type data_confidence as enum ('HIGH', 'MEDIUM', 'LOW');

create table app_users (
  id uuid primary key default gen_random_uuid(), normalized_email text unique,
  display_name text not null, gender user_gender not null default 'PREFER_NOT_TO_SAY',
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  check (normalized_email is null or normalized_email = lower(normalized_email))
);
create table auth_identities (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references app_users(id) on delete cascade,
  provider text not null, subject text not null, linked_at timestamptz not null default now(),
  unique (provider, subject), unique (user_id, provider)
);
create table books (
  id uuid primary key default gen_random_uuid(), isbn13 varchar(13) unique check (isbn13 is null or isbn13 ~ '^[0-9]{13}$'),
  title text not null, authors jsonb not null default '[]'::jsonb, publisher text, published_date date,
  description text, cover_image_url text, source_provider text not null, source_reference text,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table book_physical_profiles (
  book_id uuid primary key references books(id) on delete cascade, page_count integer check (page_count is null or page_count > 0),
  physical_thickness_mm numeric(6,2) check (physical_thickness_mm is null or physical_thickness_mm > 0),
  height_mm numeric(6,2), width_mm numeric(6,2), thickness_source thickness_source not null default 'DEFAULT',
  confidence data_confidence not null default 'LOW', source_provider text, updated_at timestamptz not null default now()
);
create table shelves (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references app_users(id) on delete cascade,
  name text not null default '나의 책장', created_at timestamptz not null default now(), updated_at timestamptz not null default now(), unique (user_id, name)
);
create table user_books (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references app_users(id) on delete cascade,
  book_id uuid not null references books(id) on delete restrict, reading_status reading_status not null, is_favorite boolean not null default false,
  rating numeric(2,1) check (rating is null or (rating between 0.5 and 5.0 and rating * 2 = trunc(rating * 2))), review_text varchar(1000),
  started_on date, finished_on date, created_at timestamptz not null default now(), updated_at timestamptz not null default now(), unique (user_id, book_id)
);
create table shelf_items (
  id uuid primary key default gen_random_uuid(), shelf_id uuid not null references shelves(id) on delete cascade,
  user_book_id uuid not null unique references user_books(id) on delete cascade, orientation shelf_orientation not null default 'VERTICAL',
  sort_key numeric(20,10) not null, created_at timestamptz not null default now(), updated_at timestamptz not null default now(), unique (shelf_id, sort_key)
);
create index user_books_by_status_idx on user_books (user_id, reading_status, updated_at desc);
create index shelf_items_by_shelf_idx on shelf_items (shelf_id, sort_key);

create function set_updated_at() returns trigger language plpgsql as $$ begin new.updated_at = now(); return new; end; $$;
create trigger app_users_set_updated_at before update on app_users for each row execute function set_updated_at();
create trigger books_set_updated_at before update on books for each row execute function set_updated_at();
create trigger physical_profiles_set_updated_at before update on book_physical_profiles for each row execute function set_updated_at();
create trigger shelves_set_updated_at before update on shelves for each row execute function set_updated_at();
create trigger user_books_set_updated_at before update on user_books for each row execute function set_updated_at();
create trigger shelf_items_set_updated_at before update on shelf_items for each row execute function set_updated_at();
