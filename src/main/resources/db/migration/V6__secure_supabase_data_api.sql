-- Bookzzang accesses PostgreSQL only through the Spring Boot JDBC connection.
-- Keep public-schema tables private from Supabase Data API roles.

-- flyway_schema_history is excluded here because Flyway locks its own history
-- table while running migrations. Its Data API privileges are still revoked
-- below. Enable RLS on that table separately in managed Supabase environments.
alter table public.app_users enable row level security;
alter table public.auth_identities enable row level security;
alter table public.books enable row level security;
alter table public.book_physical_profiles enable row level security;
alter table public.shelves enable row level security;
alter table public.user_books enable row level security;
alter table public.shelf_items enable row level security;
alter table public.email_verifications enable row level security;
alter table public.user_credentials enable row level security;
alter table public.auth_sessions enable row level security;

revoke all privileges on all tables in schema public
from anon, authenticated, service_role;

revoke all privileges on all sequences in schema public
from anon, authenticated, service_role;

revoke execute on all functions in schema public
from public, anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke select, insert, update, delete, truncate, references, trigger, maintain
  on tables from anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke usage, select, update
  on sequences from anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke execute on functions from public, anon, authenticated, service_role;

revoke create on schema public from public, anon, authenticated, service_role;

alter function public.set_updated_at()
  set search_path = pg_catalog, public;
