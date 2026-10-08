# Edupage Notify Backend

Push bridge between EduPage polling and Firebase Cloud Messaging for the
Edupage2 Android app. Clients register with their school credentials, the
server polls EduPage for new timeline events/grades and pushes data-only
FCM messages which the app renders locally.

## Quick start (Docker, recommended)

```bash
cp .env.example .env
# edit .env: FCM_PROJECT_ID, FCM_SERVICE_ACCOUNT_JSON or FCM_SERVICE_ACCOUNT, SERVER_API_KEY
docker compose up -d --build
docker compose logs -f
```

Health: `GET http://localhost:4580/api/health` (no auth required).

Data (sqlite, RSA keys, AES master key) lives in `./data` via the compose
volume. Back it up — losing `master.key` makes stored passwords unreadable.

## Manual start

Requires Node 22+.

```bash
npm ci
cp .env.example .env   # then edit
npm start               # or: npm run dev (watch mode)
```

## Configuration (.env)

| Variable | Required | Description |
|---|---|---|
| `FCM_PROJECT_ID` | yes | Firebase project id |
| `FCM_SERVICE_ACCOUNT` | one of the two | Absolute path to the service-account JSON file |
| `FCM_SERVICE_ACCOUNT_JSON` | one of the two | Inline service-account JSON (handy for Docker env) |
| `SERVER_API_KEY` | yes | Long random Bearer token, must match the app's backend key |
| `PORT` | no | HTTP port, default `4580` |
| `DATABASE_PATH` | no | sqlite path, default `./data/edupage-notify.sqlite` |
| `ENCRYPTION_KEY_PATH` | no | AES master key path, default `./data/master.key` (auto-created) |
| `RSA_KEYS_DIR` | no | RSA keypair dir, default `./data/keys` (auto-created) |
| `POLL_INTERVAL_SECONDS` | no | Per-user poll cadence, min 15, default `60` |
| `FIRST_RUN_NOTIFY` | no | Notify for pre-existing events on first run, default `false` |
| `SEED_GRADES_ON_START` | no | Seed known grade ids on boot, default `true` |
| `MESSAGE_PREVIEW_CHARS` | no | Preview length clamp (min 20), default `140` |
| `DEBUG_LOG` | no | Verbose logging, default `false` |

On first boot the server generates the RSA keypair and AES master key if
absent, migrates plain-text passwords to AES-256-GCM, then polls every
enabled user in sequence.

## API

All `/api/*` routes except `/api/health` and `/api/public-key` require
`Authorization: Bearer <SERVER_API_KEY>`.

- `GET /api/health` → `{ ok, uptimeSec, users }`
- `GET /api/public-key` → `{ publicKey }` (RSA, for password encryption)
- `POST /api/register` — login check + upsert user/device
- `POST /api/unregister` — disable one device
- `POST /api/messages/read|mark-read|sync` — read-state exchange
- `POST /api/disable-user`, `POST /api/delete-all-data`

## Reverse proxy

Terminate TLS in front (Caddy/Nginx) and forward plain HTTP. The app's
"own server" backend mode just needs the base URL + API key.
