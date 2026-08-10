# GulfHire Backend — Environment Variables & Secrets

This document explains every environment variable the backend requires, how to
provide them, and the security rules that keep secrets out of the codebase.

> **Principle:** no secret is ever hardcoded. All credentials come from the
> environment. The application **refuses to start** when a required variable
> is missing or an example value is still in use.

---

## 1. Required variables

| Variable                 | Required | Purpose                                              | Example                            |
|--------------------------|----------|------------------------------------------------------|------------------------------------|
| `DB_USERNAME`            | ✅       | PostgreSQL user for `gulfhire_db`                    | `postgres`                         |
| `DB_PASSWORD`            | ✅       | PostgreSQL password for `gulfhire_db`                | *(your DB password)*               |
| `JWT_SECRET_KEY`         | ✅       | HMAC key signing/verifying access + refresh tokens   | *(random, ≥ 32 bytes)*             |
| `CLOUDINARY_CLOUD_NAME`  | ✅       | Cloudinary account cloud name                        | `your_cloud_name`                  |
| `CLOUDINARY_API_KEY`     | ✅       | Cloudinary API key                                   | `your_api_key`                     |
| `CLOUDINARY_API_SECRET`  | ✅       | Cloudinary API secret                                | `your_api_secret`                  |

## 2. Optional variables

| Variable            | Default                                    | Purpose                        |
|---------------------|--------------------------------------------|--------------------------------|
| `DB_URL`            | `jdbc:postgresql://localhost:5432/gulfhire_db` | PostgreSQL JDBC URL        |
| `JWT_EXPIRATION` ¹  | `86400000` (24 h)                          | Access-token lifetime (ms)     |
| `JWT_REFRESH_EXPIRATION` ¹ | `604800000` (7 d)                    | Refresh-token lifetime (ms)    |
| `MAIL_ENABLED`      | `false`                                    | Master switch for transactional email. When `false` (default), all email sends are no-ops and the app runs fine without an SMTP server. |
| `MAIL_HOST`         | `localhost`                                | SMTP host (e.g. `smtp.gmail.com`) |
| `MAIL_PORT`         | `587`                                      | SMTP port (587 STARTTLS, 465 SSL, 25 plain) |
| `MAIL_USERNAME`     | *(empty)*                                  | SMTP username / login email    |
| `MAIL_PASSWORD`     | *(empty)*                                  | SMTP password / app password   |
| `MAIL_FROM`         | `no-reply@gulfhire.com`                    | From address on outgoing emails |
| `MAIL_SMTP_AUTH`    | `true`                                     | Whether the SMTP server requires authentication |
| `MAIL_STARTTLS`     | `true`                                     | Enable STARTTLS on the SMTP connection |
| `MAIL_TIMEOUT`      | `5000`                                     | SMTP connect/read timeout (ms) |
| `FRONTEND_URL`      | `http://localhost:4200`                    | Base URL used to build links inside emails (password reset, verification) |

¹ Referenced in `application.properties` as fixed values; promote to variables
in the same `${NAME}` style if you need to tune them per environment.

### Email setup (optional feature)

Transactional emails (application accepted/rejected, new chat message, password
reset, email verification) are **disabled by default** so local development
never needs an SMTP server. To enable:

1. Set `MAIL_ENABLED=true` in `.env`.
2. Fill in `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
   - **Gmail example:** `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, and a
     Gmail *App Password* (2FA required) as `MAIL_PASSWORD`.
3. Keep `FRONTEND_URL` pointing at your deployed frontend so links in emails
   work in production.

Email sending is **asynchronous and fail-soft**: if the SMTP server is down or
credentials are wrong, the application keeps working and the failure is logged
as a warning — an email problem can never break an application accept, a chat
message, or a login.

---

## 3. How the app loads secrets

1. **`application.properties`** references each secret as a placeholder:
   - `spring.datasource.username=${DB_USERNAME}`
   - `spring.datasource.password=${DB_PASSWORD}`
   - `security.jwt.secret-key=${JWT_SECRET_KEY}`
   - `cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}`
   - `cloudinary.api-key=${CLOUDINARY_API_KEY}`
   - `cloudinary.api-secret=${CLOUDINARY_API_SECRET}`
2. **Local `.env` file (optional):** a file named `.env` is loaded
   automatically from either the current working directory (`./.env`) or the
   backend folder (`./gulfhire-backend/.env`), so it works whether you launch
   from the IDE (project root) or from `gulfhire-backend/`:
   `spring.config.import=optional:file:./.env[.properties],optional:file:./gulfhire-backend/.env[.properties]`
3. **Real environment variables** (shell, IDE run configuration, container,
   CI/CD) always take **precedence** over the `.env` file.

If any required variable is missing, the application **fails fast at startup**
with a clear error instead of booting with an insecure/empty secret.

---

## 4. Setup — run locally

```bash
# 1. Copy the template and fill in YOUR values
cd gulfhire-backend
cp .env.example .env
#    edit .env  (DB_PASSWORD, JWT_SECRET_KEY, CLOUDINARY_*)

# 2. Generate a strong JWT secret (>= 32 bytes / 256 bits)
openssl rand -base64 64

# 3. Run
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows (PowerShell)
```

Not using a `.env` file? Export the variables directly:

```bash
# bash / zsh
export DB_USERNAME=postgres
export DB_PASSWORD='...'
export JWT_SECRET_KEY="$(openssl rand -base64 64)"
export CLOUDINARY_CLOUD_NAME='...'
export CLOUDINARY_API_KEY='...'
export CLOUDINARY_API_SECRET='...'
./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="..."
$env:JWT_SECRET_KEY="..."
$env:CLOUDINARY_CLOUD_NAME="..."
$env:CLOUDINARY_API_KEY="..."
$env:CLOUDINARY_API_SECRET="..."
.\mvnw.cmd spring-boot:run
```

**IntelliJ IDEA:** Run/Debug Configurations → *Environment variables* →
`DB_USERNAME=postgres;DB_PASSWORD=...;JWT_SECRET_KEY=...;...`

> 💡 The `.env` file is found automatically from the project root OR the
> `gulfhire-backend/` folder, so the working directory no longer matters.
> Tip: if your IDE reports a placeholder like `${JWT_SECRET_KEY}` unresolved,
> the variable is missing from `.env` — every line in `.env.example` must be
> present with a real value.

---

## 5. Security rules

- **Never commit secrets.** `.env` and `.env.*` are git-ignored
  (`.env.example` is the only exception — it contains placeholders only).
- **Rotate the leaked credentials.** The previous committed values
  (PostgreSQL password, Cloudinary API key/secret, JWT placeholder) were
  exposed in version history. Reset the DB password, generate a new Cloudinary
  API key (and delete the old one), and generate a fresh JWT secret.
- **JWT secret strength is enforced.** The backend rejects secrets that are
  missing, shorter than 32 bytes, or still the example/placeholder value.
- **Never log secrets.** All log statements were audited — none print
  passwords, tokens, or API secrets. Do not add logging of configuration,
  request bodies, or `Authorization` headers.
- **Special characters in `.env` values.** The `.env` file is parsed as a Java
  properties file. A value containing `#` (comment) or an unescaped `:` / `=`
  will be truncated or break parsing. Prefer generated secrets that avoid
  these characters (e.g. `openssl rand -base64 64` output is safe: it only
  contains letters, digits, `+`, `/` and `=`), or set such variables directly
  as OS environment variables instead of via the `.env` file.
- **Frontend holds no secrets.** `gulfhire-frontend/src/environments/`
  contains only `apiUrl` and `wsUrl` (non-secret). JWT tokens are issued to
  the browser at login and are not part of any config file.
- **Old demo credential files:** `login.md` (repo root) contains plaintext
  demo-account passwords. Delete it and/or rotate those account passwords
  before making the repository public.

---

## 6. Production checklist

- Inject variables via your deployment platform (env vars / secret manager),
  never via committed files.
- Use a **unique JWT secret per environment** (dev / staging / prod).
- Force HTTPS and serve the frontend over `https://` so tokens are never sent
  in clear text.
- Consider storing the DB password via a managed secret (e.g. Docker secrets,
  AWS Secrets Manager, platform-specific env secrets).
