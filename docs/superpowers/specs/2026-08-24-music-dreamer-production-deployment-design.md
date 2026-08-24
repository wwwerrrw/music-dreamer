# Music Dreamer Production Deployment Design

## Scope

This design adds a reproducible Docker-based deployment bundle for the
Music Dreamer application. It covers image builds, Compose orchestration,
Nginx public routing, database bootstrap, persistent storage, and local
static contract checks. It does not modify ECS, security-group rules, DNS,
TLS certificates, or application business behavior.

The deployment target is the existing Ubuntu 22.04 ECS with Docker 29 and
Docker Compose 5. The source revision is selected outside this design and is
passed to ECS through the Git checkout; the deployment bundle must never
embed credentials or machine-local runtime data.

## Architecture

The Compose project contains MySQL 8, Redis 7, Nacos 2.3.2, five Spring Boot
services and one Nginx edge image containing the user and admin production
builds. Only Nginx publishes a host port. All backend
and infrastructure ports remain on the private Compose network.

```text
browser -> nginx:80
            |-- default host -> music-web static files
            |-- admin host   -> admin-web static files
            `-- /api/*       -> gateway:8080

gateway -> nacos, redis
identity/catalog/interaction/media -> nacos, mysql
media -> music-dreamer-media volume
```

The first rollout is HTTP-only so the ECS IP can be used for a controlled
smoke test. The Nginx template accepts separate public and admin host names;
TLS and secure cookies are a later, explicit phase.

## Deployment files

- `deploy/compose.yaml` owns service images, private networking, health
  dependencies, restart policy, environment wiring, and named volumes.
- `deploy/backend.Dockerfile` builds one selected Spring Boot service from the
  repository root using Maven and Java 21, then runs it on a Java 21 JRE.
- `deploy/frontend.Dockerfile` builds both Vue applications with Node 24 and
  packages their `dist` output into the Nginx edge image.
- `deploy/nginx/default.conf.template` provides host-based SPA fallback and
  `/api/` proxying. It does not expose internal service ports.
- `deploy/mysql/init/01-init.sh` creates four schemas and four least-scope
  application users on the first MySQL volume initialization.
- `deploy/.env.example` documents required non-secret settings and safe
  placeholder values. The real `.env` is created only on ECS and is ignored.
- `.dockerignore` prevents Git metadata, local dependencies, runtime data,
  secrets, and generated test output entering build contexts.

## Data and secret boundaries

Named volumes preserve MySQL data, Redis data, Nacos data, and uploaded media.
The application users are scoped to their own schema. MySQL root credentials,
application passwords, host names, and cookie-security settings are supplied
through the ECS-only `.env` file. No secret is placed in Compose command
arguments, image layers, source files, or logs by design.

## Operational constraints

- `docker compose config` must pass before any first start.
- `docker compose up` is an ECS execution step and is not run as part of local
  static verification because Docker is not installed locally.
- Public exposure is limited to Nginx. Security-group changes are a separate
  user-confirmed operation.
- The deployment bundle does not claim SMTP, HTTPS, backups, or public-domain
  readiness until those later phases have fresh runtime evidence.

## Verification

The deployment contract test checks required files, service names, image build
contexts, private-port non-publication, volume declarations, secret-variable
references, Nginx proxy and SPA fallback, and MySQL bootstrap coverage. Local
Java/Maven and Node/npm builds remain separate checks from Compose runtime
verification.
