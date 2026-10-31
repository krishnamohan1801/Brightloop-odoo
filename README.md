# Brightloop Authentication & RBAC

Starter authentication module for Brightloop using Java 17, Spring Boot, Spring Security, JWT, JDBC/JdbcTemplate, PostgreSQL, and Gmail SMTP.

## Included

- Customer self-registration (public registration cannot assign ADMIN or STAFF).
- Email/password login.
- BCrypt password hashing.
- JWT bearer-token authentication.
- Backend role-based endpoint protection for ADMIN, STAFF, and CUSTOMER.
- Forgot-password email through Gmail SMTP.
- Single-use password-reset token with 15-minute expiry.
- Reset token stored as a SHA-256 hash, not raw token.
- Optional first-admin bootstrap through environment variables.
- JDBC/JdbcTemplate database access; no JPA, Hibernate, or ORM.

## Requirements

- Java 17+
- Maven 3.9+
- PostgreSQL
- Gmail account with 2-Step Verification and an available Google App Password

## 1. Create database

```sql
CREATE DATABASE brightloop;
```

On startup, Spring initializes `users` from `src/main/resources/schema.sql`.

## 2. Configure environment

Copy `.env.example` as a reference. Spring Boot does not automatically load `.env` without extra configuration. Set the values in your terminal or IDE run configuration.

Required:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_APP_PASSWORD`
- `FRONTEND_URL`

Generate a Base64 JWT secret with at least 32 random bytes:

```bash
openssl rand -base64 32
```

Never commit real secrets, `.env`, Gmail App Passwords, database passwords, or JWT secrets.

## 3. Gmail App Password setup

1. Enable 2-Step Verification on the Google account.
2. Open Google Account Security settings.
3. Create an App Password for the application.
4. Set `SPRING_MAIL_USERNAME` to the full Gmail address.
5. Set `SPRING_MAIL_APP_PASSWORD` to the generated App Password.

Use the Google-generated App Password, not your normal Gmail password. Some Google Workspace or managed accounts may not offer App Passwords.

SMTP is configured for `smtp.gmail.com`, port `587`, authentication and STARTTLS.

## 4. Create the first administrator

For initial local setup, set:
- `BRIGHTLOOP_ADMIN_NAME`
- `BRIGHTLOOP_ADMIN_EMAIL`
- `BRIGHTLOOP_ADMIN_PASSWORD`

The application creates an ADMIN on startup only if that email does not exist. Clear these variables after first use. Do not commit these credentials.

Public registration always creates a CUSTOMER. The client cannot submit a role to obtain ADMIN or STAFF privileges. An authenticated ADMIN can create STAFF accounts using `POST /api/admin/staff` with `fullName`, `email`, and `temporaryPassword`.

## 5. Run

```bash
mvn spring-boot:run
```

Backend base URL: `http://localhost:8080`

## Endpoints

### Register customer

`POST /api/auth/register`

```json
{
  "fullName": "Demo Customer",
  "email": "customer@example.com",
  "password": "CustomerPass123!"
}
```

### Login

`POST /api/auth/login`

```json
{
  "email": "customer@example.com",
  "password": "CustomerPass123!"
}
```

Successful response:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "email": "customer@example.com",
  "fullName": "Demo Customer",
  "role": "CUSTOMER"
}
```

Send the token with protected requests:

```http
Authorization: Bearer <JWT>
```

### Forgot password

`POST /api/auth/forgot-password`

```json
{ "email": "customer@example.com" }
```

Response is intentionally generic whether the email exists or not. For a registered account, an email is sent with a 15-minute reset link.

### Reset password

`POST /api/auth/reset-password`

```json
{
  "token": "<token-from-email-link>",
  "newPassword": "NewCustomerPass123!"
}
```

Token is single-use. Its SHA-256 hash is stored in PostgreSQL; successful reset clears the token and expiry.

## RBAC route rules

| Route | Access |
|---|---|
| `/api/auth/**` | Public |
| `/api/admin/**` | ADMIN |
| `/api/staff/**` | STAFF or ADMIN |
| `/api/customer/**` | CUSTOMER |
| Other routes | Authenticated user |

Admin staff provisioning:

`POST /api/admin/staff` (ADMIN only)

```json
{
  "fullName": "Demo Staff",
  "email": "staff@example.com",
  "temporaryPassword": "StaffPass123!"
}
```

Demo access-check routes:
- `GET /api/admin/access-check`
- `GET /api/staff/access-check`
- `GET /api/customer/access-check`
- `GET /api/me`

The access-check routes are starter examples. Replace them with the actual Brightloop module endpoints during integration.

## React integration

See `FRONTEND_INTEGRATION.md` for Axios examples and the login, forgot-password, reset-password, and role-navigation flow.

The frontend may hide navigation items based on the returned role, but frontend checks are not security. Spring Security must enforce authorization on every protected backend endpoint.

## Integration and security notes

1. This module uses JDBC/JdbcTemplate only. It does not use JPA, Hibernate, or ORM.
2. It uses a single `users` table as a starter. Agree on user/customer relationships and IDs before merging into the full Brightloop database.
3. This starter does not yet implement organization membership or tenant isolation.
4. JWTs remain valid until expiry. Password reset does not revoke already-issued JWTs in this starter. Add token-version checking or a revocation store if immediate revocation is required.
5. Logout in this stateless JWT starter means the frontend discards its token; it is not server-side token revocation.
6. Use HTTPS outside local development.
7. Update the CORS allowed origin in `SecurityConfig` for the deployed frontend.
8. Add rate limiting to login and forgot-password endpoints before production use.
9. The sample reset method sends mail within the transaction. For a production deployment, consider an after-commit mail event/outbox so an SMTP outage does not hold up a database transaction.
10. No real payment functionality is included in this module.

## GitHub checklist

- `.env` is not tracked.
- Gmail App Password, database password, and JWT secret are not committed.
- Bootstrap admin credentials are not committed.
- CORS origins are reviewed.
- Shared database and API contracts are agreed with the team.
- Package names and schema are reconciled with the existing Brightloop backend before merging.
