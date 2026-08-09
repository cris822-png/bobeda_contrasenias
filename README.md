# Bóveda de Contraseñas

A self-hosted personal password vault with end-to-end encryption.

| Component | Stack |
|---|---|
| Backend | Java 25 · Spring Boot 3.5 · Spring Security · Flyway |
| Client | Flutter 3.44 · Dart 3.12 · Provider |
| Database | PostgreSQL 18 |
| Crypto | Argon2id (KDF) · AES-256-GCM (entry encryption) |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Flutter Client  (Linux / Windows / Android)        │
│  • Master password entered locally                  │
│  • JWT stored in memory only (never on disk)        │
└────────────────┬────────────────────────────────────┘
                 │  HTTPS REST API
┌────────────────▼────────────────────────────────────┐
│  Spring Boot API  (local / self-hosted)             │
│  • POST /auth/register  POST /auth/unlock           │
│  • GET/POST/PUT/DELETE /vault/entries               │
│  • DEK cached in memory (TTL = JWT expiry)          │
│  • DEK cleared on /auth/lock                        │
└────────────────┬────────────────────────────────────┘
                 │  JDBC / SSL
┌────────────────▼────────────────────────────────────┐
│  PostgreSQL (Database)                              │
│  • users  (salt, kdf_params, dek_wrapped, dek_iv)  │
│  • vault_entries  (encrypted ciphertext + iv)       │
└─────────────────────────────────────────────────────┘
```

### Crypto model

1. **Registration**: A random 32-byte DEK is generated. A KEK is derived from the master password using Argon2id (t=3, m=64 MiB, p=4). The DEK is wrapped with AES-256-GCM(KEK). Only the salt, KDF parameters, and wrapped DEK are stored — never the master password or KEK.

2. **Unlock**: The same Argon2id derivation is performed. If `AES-GCM.decrypt(wrappedDEK, KEK)` succeeds (authentication tag verifies), the DEK is cached in memory and a 15-minute JWT is returned.

3. **Vault operations**: Each entry's password is encrypted with `AES-256-GCM(DEK, randomIV)`. The ciphertext and IV are stored in PostgreSQL.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 21 + |
| Maven Wrapper | bundled (`./mvnw`) |
| Flutter | 3.44+ |
| PostgreSQL | 15+ |

---

## 1 — Database setup

```bash
# Create the database and user
sudo -u postgres psql <<'SQL'
CREATE USER user WITH PASSWORD 'your_strong_password'; -- pragma: allowlist secret
CREATE DATABASE databse OWNER user;
\q
SQL

# Flyway will create the tables automatically on first startup.
```

---

## 2 — Backend setup

```bash
cd backend

# Copy the env example and fill in your values
cp .env.example .env
$EDITOR .env     # Set DB_PASSWORD, JWT_SECRET (run: openssl rand -base64 64)

# Run (Flyway migration runs automatically)
./mvnw spring-boot:run

# Or build a fat JAR:
./mvnw clean package -DskipTests
java -jar target/vault-api-0.0.1-SNAPSHOT.jar
```

The API listens on the port set in `API_PORT` (default `8443`).

> **TLS**: For production, configure `server.ssl.*` in `application.yml` and point it at a certificate. For local development, plain HTTP on localhost is fine.

---

## 3 — Flutter client setup

```bash
cd client

# Create the .env asset file
cp .env.example assets/.env
# Edit assets/.env — set API_BASE_URL to http://localhost:8443 (or your server)

# Fetch dependencies
flutter pub get

# Run on Linux desktop
flutter run -d linux

# Run on a connected Android device
flutter run -d android

# Build release (Linux)
flutter build linux --release
```

---

## 4 — First-time use

1. Start the backend and client.
2. On the lock screen, tap **"¿Primer uso? Crear cuenta"**.
3. Enter a strong master password (≥ 12 characters). **This password cannot be recovered.**
4. **Copy and save your User ID** — you need it every time you unlock.
5. Go back to the unlock screen and authenticate.

---

## Environment variables reference

### Backend (`backend/.env`)

| Variable | Description | Example |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `database` |
| `DB_USER` | DB user | `user` |
| `DB_PASSWORD` | DB password | `your_strong_password` |
| `DB_SSL_MODE` | SSL mode (`prefer`, `require`, …) | `prefer` |
| `JWT_SECRET` | HMAC-SHA256 signing key (≥ 32 chars) | *(generate with openssl)* |
| `JWT_EXPIRY_MINUTES` | Token lifetime in minutes | `15` |
| `API_PORT` | HTTP port | `8443` |

### Client (`client/assets/.env`)

| Variable | Description | Example |
|---|---|---|
| `API_BASE_URL` | Backend base URL (no trailing slash) | `http://localhost:8443` |

---

## Security notes

- The master password is **never stored**. If lost, the vault data is unrecoverable.
- The DEK lives in server memory only — never written to disk. It is zeroed on logout and on TTL expiry.
- All intermediate key material (`char[]`/`byte[]`) is explicitly zeroed after use.
- Logging is configured to prevent crypto classes from emitting sensitive data.
- For production: enable TLS, set `DB_SSL_MODE=require`, restrict heap dump access.

---

## Project structure

```
.
├── backend/          Java / Spring Boot REST API
│   ├── mvnw          Maven wrapper (no global Maven needed)
│   ├── .env.example
│   └── src/main/java/com/vault/api/
│       ├── config/   EnvConfig, SecurityConfig, JwtConfig
│       ├── controller/  AuthController, VaultController
│       ├── crypto/   KdfService, AesGcmService, DekManager
│       ├── model/    User, VaultEntry, DTOs, exceptions
│       ├── repository/  UserRepository, VaultEntryRepository
│       ├── security/ JwtTokenProvider, JwtAuthFilter
│       └── service/  AuthService, VaultService
└── client/           Flutter multi-platform client
    ├── .env.example
    └── lib/
        ├── main.dart
        ├── models/   VaultEntry, AuthState
        ├── screens/  unlock, register, vault_list, entry_form
        ├── services/ ApiClient, SecureStorageService
        └── widgets/  EntryCard, PasswordField
```
