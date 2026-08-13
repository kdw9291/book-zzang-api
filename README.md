# Bookzzang API

책짱의 Android 앱을 위한 Spring Boot API입니다. 공개 도서 검색은 Kakao Book Search를 사용하며, 저장 시 Google Books의 페이지 수와 실측 두께를 보강합니다.

## Run locally

1. Copy `.env.example` values into your IDE run configuration (never commit real keys).
2. Start PostgreSQL: `docker compose up -d postgres`
3. Run: `set GRADLE_USER_HOME=C:\workspace\bookzzang\.gradle-home` then `gradlew.bat bootRun`

`SUPABASE_JWT_ISSUER_URI` is required for member endpoints. Public endpoints work without it; member endpoints remain protected until it is configured.

## API surface (P6)

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/public/books?query=&size=` | guest | Kakao book search |
| GET | `/api/public/books/isbn/{isbn13}` | guest | book detail with physical metadata enrichment |
| POST | `/api/me/onboarding` | JWT | create internal user, identity link, and default shelf |
| POST | `/api/me/books` | JWT | register/update a book and its reading status |

The database tables are application-owned PostgreSQL tables. `auth_identities(provider, subject)` is the only authentication link, so moving Supabase PostgreSQL to AWS RDS does not change user, book, or shelf identifiers.
