# P7-B 인증·개인정보 암호화 설정

## 새 환경 변수

API 프로젝트의 `.env`에 아래 두 값을 추가한다. 두 값은 서로 다른 무작위 32바이트 값을 Base64로 인코딩한 값이어야 한다.

```text
PII_ENCRYPTION_KEY=
PII_EMAIL_LOOKUP_KEY=
```

PowerShell에서 값만 생성하려면 다음을 각각 두 번 실행한다. 출력값은 `.env`에만 넣고 Git·채팅·스크린샷에 공유하지 않는다.

```powershell
[byte[]]$piiKeyBytes = New-Object byte[] 32
$piiRng = New-Object Security.Cryptography.RNGCryptoServiceProvider
$piiRng.GetBytes($piiKeyBytes)
$piiRng.Dispose()
[Convert]::ToBase64String($piiKeyBytes)
```

`PII_ENCRYPTION_KEY`를 잃으면 기존 성명·이메일을 복호화할 수 없다. 운영 배포 전에는 두 키를 AWS Secrets Manager에 보관하고, EC2 인스턴스 역할로 읽도록 전환한다.

## 동작 변경

- 비밀번호: Argon2id 해시만 `user_credentials`에 저장한다.
- 이메일·성명: AES-256-GCM 암호문만 `app_users`에 저장한다.
- 이메일 중복·인증코드 조회: HMAC-SHA-256 조회값만 저장한다.
- 로그인·토큰 갱신·로그아웃: `bookzzang-api`가 처리하며 Android는 Supabase Auth에 직접 연결하지 않는다.
- 자동 로그인: Android Keystore로 암호화한 갱신 토큰을 사용한다.

## 첫 재기동 시 영향

Flyway V3/V4가 적용된다. 기존 `app_users`의 평문 이메일·성명은 암호문으로 변환한 뒤 `null`로 지운다. 이전에 발송한 인증코드는 보안을 위해 폐기되므로, 회원가입 화면에서 새 코드를 발송해야 한다. 기존 Supabase 테스트 계정은 새 비밀번호 해시가 없으므로 재가입해야 한다.
