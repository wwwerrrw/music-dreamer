# Music Dreamer ECS Deployment

This bundle runs the application on the prepared Ubuntu ECS with Docker
Compose. It is intentionally HTTP-only for the first controlled smoke test.
It does not configure DNS, TLS, or Alibaba Cloud security-group rules.

## First setup on ECS

Run as `root` from `/opt/music-dreamer` after pulling the release commit:

```bash
cd /opt/music-dreamer
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
nano deploy/.env
docker compose --env-file deploy/.env -f deploy/compose.yaml config
```

Replace every `CHANGE_ME` value with a unique random value. Keep `.env` on
ECS only. Do not paste it into chat or commit it.

## First start

The following command builds images and starts all services. It is an ECS
execution step and should only be run after the config check succeeds:

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.yaml ps
docker compose --env-file deploy/.env -f deploy/compose.yaml logs --tail=100 nginx gateway identity catalog interaction media
```

The first MySQL start creates four schemas and four schema-owned accounts.
The initialization script runs only when the MySQL data volume is empty.
Nacos authentication is disabled for this first internal rollout because the
current services do not provide Nacos client credentials. Nacos is not
published to the host; enable Nacos authentication and wire client credentials
before treating the stack as shared or public production.

## Controlled smoke checks

```bash
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/login
curl -H 'Host: admin.localhost' -I http://127.0.0.1/login
curl -H 'Accept: application/json' -i http://127.0.0.1/api/identity/sessions
```

Do not treat `docker ps` alone as application acceptance. Check Nacos service
registration, API response status, browser login/activation, media upload and
Range playback, then restart the stack and verify data remains.

## Data and rollback

The named volumes `mysql-data`, `redis-data`, `nacos-data`, `nacos-logs`, and
`media-data` are persistent. Never run `docker compose down -v` on production
unless data deletion has been separately reviewed and confirmed. To roll back,
check out the previous approved Git commit, rebuild, and restart without
deleting volumes.

## Later public phase

Before calling the service public production, configure DNS and TLS, set
`MUSIC_DREAMER_IDENTITY_MEDIA_COOKIE_SECURE=true`, expose only 80/443 at the security
group, and restrict SSH 22 to a trusted source. These are separate changes with
their own confirmation and fresh external verification.
