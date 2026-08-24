# Music Dreamer Production Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reproducible Docker, Compose, Nginx, and MySQL deployment bundle for Music Dreamer without changing application behavior or touching ECS.

**Architecture:** Build one selected Spring Boot service per backend image and both Vue applications into the Nginx edge image. Compose runs MySQL, Redis, Nacos, five backend services, and Nginx on one private network; only Nginx publishes port 80. Named volumes preserve databases, Nacos state, Redis state, and uploaded media.

**Tech Stack:** Docker multi-stage builds, Maven/Java 21, Node 24/npm 11, Vue 2 production builds, Nginx, MySQL 8, Redis 7, Nacos 2.3.2, Node test runner.

**Spec:** `docs/superpowers/specs/2026-08-24-music-dreamer-production-deployment-design.md`

## Global Constraints

- Do not install Docker locally; local verification must work without Docker.
- Do not connect to ECS, start containers, modify security groups, or modify DNS in this phase.
- Never commit real passwords, tokens, `.env`, database data, media data, or generated runtime output.
- Only Nginx may publish a host port in Compose; MySQL, Redis, Nacos, and application ports stay private.
- Preserve four independent database schemas and service ownership boundaries.
- Treat `docker compose config` and ECS runtime smoke tests as separate evidence from static source checks.

---

### Task 1: Add the failing deployment contract test

**Files:**
- Create: `infra/tests/deployment-contract.test.mjs`

- [ ] Read the approved design and enumerate the required Compose services,
  Dockerfiles, Nginx rules, volumes, and secret references.
- [ ] Write assertions for required deployment files and service names.
- [ ] Assert that only Nginx has a `ports:` mapping and internal ports are not
  published.
- [ ] Assert that Compose references ECS-only variables rather than literal
  passwords and that the MySQL init script covers all four schemas.
- [ ] Run `node --test infra/tests/deployment-contract.test.mjs` and record the
  expected RED failure because deployment files do not exist yet.

### Task 2: Add build-context and image definitions

**Files:**
- Create: `.dockerignore`
- Create: `deploy/backend.Dockerfile`
- Create: `deploy/frontend.Dockerfile`

- [ ] Exclude `.git`, `node_modules`, Maven `target`, runtime data, media data,
  secrets, reports, and local worktrees from build contexts.
- [ ] Implement Java 21/Maven multi-stage packaging with a `SERVICE` build arg.
- [ ] Implement Node 24/npm 11 workspace installation and both frontend builds.
- [ ] Package both `dist` trees into an Nginx static image.

### Task 3: Add Compose orchestration and database bootstrap

**Files:**
- Create: `deploy/compose.yaml`
- Create: `deploy/.env.example`
- Create: `deploy/mysql/init/01-init.sh`

- [ ] Define MySQL, Redis, Nacos, five backend services, and the frontend-serving Nginx edge.
- [ ] Add private network, restart policies, infrastructure health checks, and
  startup dependencies without publishing internal ports.
- [ ] Wire MySQL JDBC URLs, Redis, Nacos, media storage, and secure-cookie
  settings through environment variables.
- [ ] Define named volumes for MySQL, Redis, Nacos, and media files.
- [ ] Create schema-specific MySQL users without embedding credentials in SQL.

### Task 4: Add Nginx public routing and operator documentation

**Files:**
- Create: `deploy/nginx/default.conf.template`
- Create: `deploy/README.md`

- [ ] Route the default host to the music SPA and the configured admin host to
  the admin SPA.
- [ ] Add history-mode fallback and `/api/` proxying to Gateway.
- [ ] Document ECS-only `.env` creation, first `docker compose config` check,
  first start, logs, status, smoke checks, and the later HTTPS/security-group
  phase.

### Task 5: Run local verification

- [ ] Run the deployment contract test and confirm GREEN.
- [ ] Run frontend tests and both production builds with the existing Node/npm
  toolchain.
- [ ] Run the full backend Maven verification with the existing Java/Maven
  toolchain, capturing any environment-only failure separately.
- [ ] Run static checks for secret literals, ignored `.env`, and required file
  modes.
- [ ] Run `docker compose config` only where Docker is available; otherwise
  report it as an ECS-required check rather than claiming it passed.
- [ ] Review the full diff and leave ECS, Git push, and security-group changes
  for a separate confirmed phase.
