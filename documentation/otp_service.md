# OTP service with Redis and SMTP

## What was added

The backend now has a dedicated OTP module with three endpoints:

- `POST /api/otp/send` sends a one-time code to email
- `POST /api/otp/verify` validates the code and invalidates it after successful verification
- `POST /api/otp/test-send` sends a stub email with fake code `000000`

Implementation details:

- OTP codes are numeric and generated with `SecureRandom`
- the code itself is not stored in plain text; Redis stores only a BCrypt hash
- every OTP has TTL, resend cooldown, and a verification attempt limit
- after successful verification the code is deleted from Redis immediately
- if email delivery fails, the code is also removed from Redis

## API contract

### Send OTP

`POST /api/otp/send`

```json
{
  "email": "user@example.com",
  "purpose": "registration"
}
```

Possible `purpose` values:

- `registration`
- `password_reset`

Successful response:

```json
{
  "status": "OTP_SENT",
  "message": "OTP code was sent to the email address",
  "email": "user@example.com",
  "purpose": "registration",
  "expiresAt": "2026-03-24T10:05:00Z",
  "retryAfterSeconds": null
}
```

### Verify OTP

`POST /api/otp/verify`

```json
{
  "email": "user@example.com",
  "purpose": "registration",
  "code": "123456"
}
```

### Send stub email

`POST /api/otp/test-send`

```json
{
  "email": "user@example.com",
  "purpose": "registration"
}
```

This endpoint:

- sends an email through the configured SMTP server
- uses the fixed fake code `000000`
- does not write anything to Redis
- can be disabled with `OTP_TEST_ENDPOINT_ENABLED=false`

Successful response:

```json
{
  "status": "OTP_TEST_SENT",
  "message": "Stub OTP email was sent with a fake code",
  "email": "user@example.com",
  "purpose": "registration",
  "expiresAt": "2026-03-25T10:05:00Z",
  "retryAfterSeconds": null
}
```

## How it works

1. Client calls `/api/otp/send`
2. Backend checks Redis cooldown key for the pair `email + purpose`
3. Backend generates a 6-digit code and hashes it with BCrypt
4. Backend stores the hash and attempt counter in Redis with TTL
5. Backend sends the code over SMTP
6. Client calls `/api/otp/verify` with the same email, purpose and code
7. Backend increments the attempt counter, compares the code with the stored BCrypt hash and returns the result
8. If the code is correct, Redis keys are deleted immediately

The test endpoint bypasses steps 2-8 and only checks that SMTP delivery works.

## Config structure

The project is split into two config layers:

- `src/main/resources/application.properties`: shared defaults that are safe to commit
- `application-local.properties`: developer-local overrides with secrets, loaded from the project root and ignored by Git

This keeps the repo runnable for everyone without sharing mail passwords.

## Local setup

### 1. Start PostgreSQL and Redis

```bash
docker compose up -d postgres redis
```

### 2. Configure SMTP for IntelliJ IDEA

Copy `application-local.properties.example` to `application-local.properties` in the project root.

Fill only these two lines:

```properties
OTP_MAIL_USERNAME=your-mailbox@yandex.ru
OTP_MAIL_PASSWORD=your-yandex-app-password
```

Everything else is already configured:

- `smtp.yandex.ru`
- port `465`
- `SSL=true`
- `STARTTLS=false`
- `OTP_MAIL_FROM=${OTP_MAIL_USERNAME}`
- `OTP_TEST_ENDPOINT_ENABLED=true` only in local example

The real file `application-local.properties` must not be committed.

### 3. Start the application

```bash
docker compose up -d postgres redis
```

Then start the backend from IntelliJ IDEA or with:

```bash
mvn spring-boot:run
```

## Why Redis is used here

Redis is a good fit for OTP because:

- codes are short-lived and do not need to be stored in PostgreSQL
- TTL is native and does not require cleanup jobs
- resend cooldowns and attempt counters are easy to maintain as separate ephemeral keys
- validation stays fast even under frequent requests

## Recommended defaults

Current defaults from `application.properties`:

- code lifetime: 300 seconds
- resend cooldown: 60 seconds
- verification attempts: 5
- code length: 6 digits

## Notes

- No frontend changes were added
- The module is backend-only and can be connected later to registration, password reset, or email confirmation flows
- Do not store real SMTP passwords in `application.properties`
- Keep secrets only in `application-local.properties`, CI/CD secret storage, or deployment environment variables
- `application-local.properties` is loaded automatically from the project root via `spring.config.import`
- `POST /api/otp/test-send` is disabled by default in shared config and enabled only in the local example
