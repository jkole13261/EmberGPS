# 🔥 EmberGPS

A self-hosted GPS tracking server for **Cradlepoint R980** routers (and any device that can HTTP-POST JSON).

```
Cradlepoint R980 (NCOS SDK script)
        │
        │  POST /api/v1/gps/ingest
        │  X-API-Key: emb_xxxxx
        ▼
   [Nginx / HTTPS]
        │
        ▼
 [Spring Boot :8080]  ──→  [PostgreSQL 16]
        │
        ▼
   [Leaflet Map UI]
```

## Features

| Feature | Detail |
|---------|--------|
| **Ingest endpoint** | `POST /api/v1/gps/ingest` — authenticated, idempotent, rate-limited |
| **Latest positions** | `GET /api/v1/gps/latest` — all devices or one |
| **History** | `GET /api/v1/gps/history/{id}` — paginated, filterable by time range |
| **Device management** | Admin API to register / revoke / rotate device keys |
| **Map UI** | Leaflet + OpenStreetMap — markers, directional arrows, route polylines |
| **Rate limiting** | Bucket4j token bucket per device (default 120 req/min) |
| **Data retention** | Nightly purge of positions older than N days (configurable) |
| **Auth** | SHA-256-hashed per-device API keys + separate admin key |
| **HTTPS** | Nginx + Let's Encrypt (TLS 1.2/1.3) |
| **Deployment** | Docker Compose (Postgres + Spring Boot + Nginx) |

---

## Quick Start (Development)

### Prerequisites

* Docker >= 24 and Docker Compose >= 2
* Java 17+ and Maven 3.9+ (for local development only)

### 1 - Clone and configure

```bash
git clone https://github.com/your-org/embergps.git
cd embergps
cp .env.example .env
# Edit .env - set DB_PASSWORD and ADMIN_API_KEY
```

### 2 - Start services

```bash
docker compose up -d
```

This starts:
* **PostgreSQL 16** on `localhost:5432` (internal only)
* **Spring Boot** on `localhost:8080`
* **Nginx** on ports 80 and 443

### 3 - Register your first device

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/devices \
  -H "X-Admin-Key: change-me-before-deploy" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "CP12345678", "name": "Truck 1"}'
```

The response includes an `apiKey` field — **save it now**, it is shown only once:

```json
{
  "deviceId": "CP12345678",
  "name": "Truck 1",
  "active": true,
  "apiKey": "emb_a1b2c3d4..."
}
```

### 4 - Open the map

Navigate to `http://localhost` (or `https://your-domain` in production).
Enter your admin or device API key in the header bar and click **Refresh**.

---

## Cradlepoint R980 Setup

### NCOS SDK Script

1. Copy `cradlepoint/gps_forwarder.py` to the router via **NetCloud Manager**:
   - **Router > Configuration > System > SDK > Applications > Upload**

2. Set the following SDK environment variables in NetCloud Manager:

   | Variable | Value |
   |----------|-------|
   | `SERVER_URL` | `https://your-server.com/api/v1/gps/ingest` |
   | `API_KEY` | the `apiKey` returned in step 3 above |
   | `INTERVAL` | `30` (seconds between GPS reports) |

3. Enable and start the application.

The router will POST GPS data every `INTERVAL` seconds once it has a GPS fix.

### Expected Payload

```json
{
  "device_id": "CP12345678",
  "timestamp": "2024-01-15T10:30:00Z",
  "latitude":  37.774929,
  "longitude": -122.419415,
  "altitude":  52.1,
  "speed":     12.5,
  "heading":   180.0,
  "fix_type":  3,
  "hdop":      1.2,
  "satellites": 8
}
```

---

## API Reference

### Authentication

| Endpoint group | Required header |
|---------------|-----------------|
| `POST /api/v1/gps/ingest` | `X-API-Key: <device-key>` |
| `GET  /api/v1/gps/**` | `X-API-Key` or `X-Admin-Key` |
| `*    /api/v1/admin/**` | `X-Admin-Key: <admin-key>` |

### GPS Endpoints

```
POST /api/v1/gps/ingest                             Receive GPS position from device

GET  /api/v1/gps/latest                             Latest position for all devices
GET  /api/v1/gps/latest/{deviceId}                  Latest position for one device

GET  /api/v1/gps/history/{deviceId}                 Paginated history
     ?from=2024-01-01T00:00:00Z
     &to=2024-01-31T23:59:59Z
     &page=0
     &size=200
```

### Device Management (Admin)

```
POST   /api/v1/admin/devices                        Register new device
GET    /api/v1/admin/devices                        List all devices
GET    /api/v1/admin/devices/{deviceId}             Get device
DELETE /api/v1/admin/devices/{deviceId}             Deactivate device
POST   /api/v1/admin/devices/{deviceId}/regenerate-key  New API key
```

---

## Production Deployment

### TLS with Let's Encrypt

```bash
certbot certonly --standalone -d your-domain.com
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem   nginx/ssl/key.pem
sed -i 's/YOUR_DOMAIN/your-domain.com/g' nginx/nginx.conf
docker compose up -d
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PASSWORD` | `embergps` | PostgreSQL password |
| `ADMIN_API_KEY` | `change-me-before-deploy` | Admin API key |
| `DATA_RETENTION_DAYS` | `90` | Days to keep GPS history (0 = forever) |
| `RATE_LIMIT_INGEST_PER_MINUTE` | `120` | Max ingest requests per device per minute |

### Registering Multiple Devices

```bash
for SERIAL in CP001 CP002 CP003 CP004 CP005; do
  curl -s -X POST https://your-server.com/api/v1/admin/devices \
    -H "X-Admin-Key: $ADMIN_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"deviceId\": \"$SERIAL\", \"name\": \"Router $SERIAL\"}"
done
```

---

## Architecture

```
backend/
  src/main/java/com/embergps/
    EmberGpsApplication.java        Spring Boot entry point
    config/                         CORS, rate limiter, Jackson
    controller/                     REST controllers
      GpsIngestController           POST /api/v1/gps/ingest
      GpsQueryController            GET  /api/v1/gps/**
      DeviceController              Admin device CRUD
    dto/                            Request/response objects
    exception/                      Custom exceptions + global handler
    filter/                         ApiKeyAuthFilter
    model/                          JPA entities: Device, GpsPosition
    repository/                     Spring Data JPA repositories
    service/                        Business logic + data retention
  src/main/resources/
    application.yml
    db/migration/V1__init.sql       Flyway schema

frontend/
  index.html                        Single-page map UI
  map.js                            Leaflet logic
  style.css

cradlepoint/
  gps_forwarder.py                  NCOS SDK script for the R980

nginx/nginx.conf                    HTTPS reverse proxy
docker-compose.yml                  Postgres + App + Nginx
```

## Security Notes

- Device API keys are stored as SHA-256 hashes — the plain key is shown only once at creation.
- Comparisons use `MessageDigest.isEqual` (constant-time) to prevent timing attacks.
- HTTPS is enforced in production via Nginx with TLS 1.2/1.3.
- Nginx adds HSTS, X-Frame-Options, and X-Content-Type-Options headers.
- PostgreSQL is exposed only on `127.0.0.1`.
- GPS ingest is rate-limited per device.
