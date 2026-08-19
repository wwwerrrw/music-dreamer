# Music Dreamer P5 Accelerated Acceptance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This workspace must remain single-Agent and serial; do not dispatch subagents.

**Goal:** Complete P5 with one fresh backend regression, controlled lifecycle coverage for five services and two frontends, and one combined restart/persistence acceptance while reusing still-valid P4 evidence.

**Architecture:** Extend the existing manifest-driven PowerShell runtime engine instead of adding a second launcher. A dedicated application manifest starts executable Spring Boot JARs and the two frontend CLIs directly, injects database secrets only into child-process environments, records unique log paths, and verifies TCP/HTTP readiness. P5 evidence is split into fresh results, hash-validated P4 reuse, and explicitly out-of-scope work.

**Tech Stack:** PowerShell 7.6, Java 21, Maven 3.9.9, Spring Boot 3.2.4, Node 24.18, Vite, Vue CLI, MySQL 8.0, Redis 8.10, Nacos 2.3.2.

## Global Constraints

- Work only on `main` at `fc24b70d234490f53c4d161a884c8bf6f5983e30`; preserve all user and P1-P4 uncommitted changes.
- Do not commit, merge, push, publish, clean, roll back, or overwrite unrelated files.
- Keep a single Agent and execute tasks serially.
- Do not stop or restart MySQL, Redis, or Nacos.
- Do not exit or delete Redis sessions, especially sessions for users `15-18`.
- Read the MySQL password only from `runtime-data/identity-db.env` inside the process that needs it. Never print or persist passwords, bearer values, activation tokens, cookies, or session identifiers.
- Do not use `项目素材/` and do not generate images.
- Do not rerun frontend tests, frontend builds, Playwright, screenshots, or full P4 E2E unless the P4 reuse hash check fails at the affected boundary.
- Before Maven execution, report and obtain a new overwrite confirmation for the twelve reactor `target/**` directories and `E:\Zmkk\project\derby.log`.
- Before stopping or restarting the five services or two frontends, report exact PIDs, ports, impact, recovery path, and whether an elevated user command will be required; obtain a separate confirmation.
- If a frontend rebuild or new Playwright run becomes necessary, enter its original independent confirmation gate. P5 approval does not authorize either action.
- New behavior follows RED -> GREEN. Evidence commands may write only to a new timestamped `test-results/p5-.../` directory and declared runtime state/log paths.

## File Map

- Create `infra/config/application-runtime-manifest.psd1`: seven application process declarations, runtime paths, environment-file mappings, readiness, logs, mutex, and state path.
- Create `infra/tests/application-runtime-contract.Tests.ps1`: focused RED/GREEN coverage for working directories, environment mapping, TCP readiness, unique logs, state secrecy, and the seven-process manifest contract.
- Create `infra/tests/fixtures/application-runtime-probe.mjs`: deterministic Node probe used only by the focused lifecycle test.
- Modify `infra/scripts/runtime-common.ps1`: environment-file parsing, TCP readiness, per-process working-directory resolution, unique log-path generation, and safe status metadata.
- Modify `infra/scripts/start.ps1`: pass resolved working directory, child-only environment, and output/error log paths to `Start-Process`; persist paths but never environment values.
- Modify `infra/scripts/status.ps1`: expose owned process readiness and recorded log paths without reading log content.
- Modify `backend/pom.xml`: pin `spring-boot-maven-plugin` under `pluginManagement` with the `repackage` execution.
- Modify `backend/{gateway,identity,catalog,media,interaction}-service/pom.xml`: opt each application module into executable JAR repackaging.
- Create `docs/handoffs/2026-08-19-p5-main-acceptance-handoff.md`: final evidence index, reused evidence, results, residual risks, and out-of-scope list.
- Generate `test-results/p5-20260819-224207/**`: append-only acceptance evidence. Do not overwrite the P4 evidence directory.

---

### Task 1: Freeze The Reusable P4 Boundary

**Files:**
- Read: `test-results/p4-task11-12-20260819-195300/frontend-tests-after-media-fix.txt`
- Read: `test-results/p4-task11-12-20260819-195300/e2e-summary-attempt9.json`
- Create at execution: `test-results/p5-20260819-224207/baseline-reuse.json`
- Create at execution: `test-results/p5-20260819-224207/source-hashes-before.csv`

**Interfaces:**
- Consumes: P4 `41/41`, dual-build, attempt 9, Range `206`, and current Git metadata.
- Produces: an evidence directory path and a SHA-256 baseline for `frontend/{shared,music-web,admin-web}` source/config/tests plus backend `src/**` files that supported P4.

- [ ] **Step 1: Capture repository identity without changing Git**

Run `git branch --show-current`, `git rev-parse HEAD`, and `git status --short`; write sanitized results to the new P5 evidence directory. Expected: branch `main`, HEAD `fc24b70d234490f53c4d161a884c8bf6f5983e30`, and the existing dirty tree remains intact.

- [ ] **Step 2: Hash the reusable evidence and relevant sources**

Use `Get-FileHash -Algorithm SHA256` on the two named P4 evidence files and on relevant source/config/test files, excluding `node_modules/**`, `dist/**`, `target/**`, runtime data, and secrets. Serialize only paths, hashes, sizes, and UTC timestamps.

- [ ] **Step 3: Record reuse decisions**

Write `baseline-reuse.json` with these exact categories: `frontendTests`, `frontendBuilds`, `playwrightAttempt9`, `mediaRange`, `invalidatedScopes`, and `decision`. Expected: `invalidatedScopes` is empty before P5 changes and `decision` is `reuse`.

### Task 2: Add Focused Lifecycle Tests And Prove RED

**Files:**
- Create: `infra/tests/application-runtime-contract.Tests.ps1`
- Create: `infra/tests/fixtures/application-runtime-probe.mjs`
- Test: `infra/tests/application-runtime-contract.Tests.ps1`

**Interfaces:**
- Consumes: existing `Import-RuntimeManifest`, `Start-RuntimeProcess` flow through `start.ps1`, and state ownership checks.
- Produces: contracts for `Get-RuntimeProcessEnvironment`, `New-RuntimeLogPaths`, TCP readiness, explicit working directories, log metadata, and application manifest shape.

- [ ] **Step 1: Write a probe that exposes only test markers**

The Node fixture must read `MUSIC_DREAMER_TEST_PORT`, `MUSIC_DREAMER_TEST_OUTPUT`, and `MUSIC_DREAMER_TEST_MARKER`, write JSON containing its current working directory and the non-secret test marker, then hold an HTTP listener on `127.0.0.1`. It must never inspect production environment names.

- [ ] **Step 2: Write the focused PowerShell assertions**

The test must use a unique directory under `test-results/`, leave its artifacts for review, choose an ephemeral TCP port, and assert:

```powershell
Get-RuntimeProcessEnvironment -EnvironmentFiles $spec.EnvironmentFiles
New-RuntimeLogPaths -ProcessName 'Probe' -LogDirectory $spec.LogDirectory
Test-RuntimeProcessReadiness @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = $port; RequestTimeoutSeconds = 1 }
```

It must also run `start.ps1` against a generated one-process manifest, verify the probe's working directory/environment, confirm stdout/stderr paths are unique and recorded in state, confirm the marker value is absent from the state JSON, and stop only the PID created by that test. No file cleanup is performed.

- [ ] **Step 3: Add the static seven-process contract**

Assert that `application-runtime-manifest.psd1` declares exactly `Identity`, `Catalog`, `Media`, `Interaction`, `Gateway`, `MusicWeb`, and `AdminWeb`; ports are `8081`, `8082`, `8083`, `8084`, `8080`, `5173`, and `5174`; backend readiness is TCP; frontend readiness is HTTP; four database services map their target password variable from `MUSIC_DREAMER_IDENTITY_DB_PASSWORD`; and no password value appears in the manifest.

- [ ] **Step 4: Run RED**

Run:

```powershell
pwsh -NoProfile -File infra/tests/application-runtime-contract.Tests.ps1
```

Expected: nonzero exit because the new runtime functions and application manifest do not yet exist. Save sanitized output as `lifecycle-red.txt`.

### Task 3: Implement The Application Runtime And Prove GREEN

**Files:**
- Modify: `infra/scripts/runtime-common.ps1`
- Modify: `infra/scripts/start.ps1`
- Modify: `infra/scripts/status.ps1`
- Create: `infra/config/application-runtime-manifest.psd1`
- Modify: `backend/pom.xml`
- Modify: `backend/gateway-service/pom.xml`
- Modify: `backend/identity-service/pom.xml`
- Modify: `backend/catalog-service/pom.xml`
- Modify: `backend/media-service/pom.xml`
- Modify: `backend/interaction-service/pom.xml`
- Test: `infra/tests/application-runtime-contract.Tests.ps1`

**Interfaces:**
- Consumes: manifest keys `WorkingDirectory`, `EnvironmentFiles`, `LogDirectory`, and `Readiness`.
- Produces: `Get-RuntimeProcessEnvironment([array]) -> [hashtable]`, `New-RuntimeLogPaths(string,string) -> PSCustomObject`, TCP readiness, executable `*-0.1.0.jar` files after Maven, and `runtime-data/application-processes.json`.

- [ ] **Step 1: Implement strict child-process environment mapping**

`Get-RuntimeProcessEnvironment` must parse nonblank, noncomment `NAME=VALUE` lines, reject malformed/duplicate keys, require every declared source name, and return a hashtable whose keys are target names. The manifest mapping direction is target to source:

```powershell
EnvironmentFiles = @(
    @{
        Path = 'runtime-data/identity-db.env'
        Variables = @{
            MUSIC_DREAMER_CATALOG_DB_PASSWORD = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
        }
    }
)
```

Do not add environment names or values to process state, command arguments, console output, or logs.

- [ ] **Step 2: Implement working directories, logs, and TCP readiness**

Resolve each `WorkingDirectory` under the project root. Generate stdout and stderr names from process name, UTC timestamp, and a GUID under the declared `LogDirectory`. Add TCP readiness with `System.Net.Sockets.TcpClient` and a bounded per-attempt timeout. Preserve existing HTTP and Redis behavior.

- [ ] **Step 3: Wire the start and status paths**

Pass `-WorkingDirectory`, `-Environment`, `-RedirectStandardOutput`, and `-RedirectStandardError` to `Start-Process`. Persist `WorkingDirectory`, `StandardOutputPath`, and `StandardErrorPath` alongside PID/start/marker ownership fields. Status JSON may return those paths, but no log content or environment data.

- [ ] **Step 4: Add executable Spring Boot packaging**

In the parent POM, configure `org.springframework.boot:spring-boot-maven-plugin:${spring-boot.version}` with a `repackage` execution under `pluginManagement`. Declare that plugin in exactly the five application service POMs; do not apply it to contracts, security, migrations, or database baseline modules.

- [ ] **Step 5: Add the seven-process application manifest**

Use Java `E:\.minecraft\runtime\java-runtime-delta\bin\java.exe` with the five arguments `-jar backend/identity-service/target/identity-service-0.1.0.jar`, `-jar backend/catalog-service/target/catalog-service-0.1.0.jar`, `-jar backend/media-service/target/media-service-0.1.0.jar`, `-jar backend/interaction-service/target/interaction-service-0.1.0.jar`, and `-jar backend/gateway-service/target/gateway-service-0.1.0.jar`. Use Node `E:\nodejs-24.18.0\node.exe` with `../node_modules/vite/bin/vite.js` from `frontend/music-web` and `../node_modules/@vue/cli-service/bin/vue-cli-service.js` from `frontend/admin-web`. Store logs below `runtime-data/logs/applications`, use `runtime-data/application-processes.json`, and use a mutex distinct from the Redis/Nacos manifest.

- [ ] **Step 6: Run GREEN**

Run the same focused PowerShell test. Expected: exit `0`, all assertions print `PASS`, the probe is stopped by verified PID ownership, retained test artifacts contain no production secret, and output is saved as `lifecycle-green.txt`.

### Task 4: Run One Full Maven Verify Behind The Overwrite Gate

**Files overwritten by Maven:**
- `backend/platform-contracts/target/**`
- `backend/platform-security/target/**`
- `database/identity/target/**`
- `database/catalog/target/**`
- `database/interaction/target/**`
- `database/media/target/**`
- `backend/gateway-service/target/**`
- `backend/identity-service/target/**`
- `backend/catalog-service/target/**`
- `backend/interaction-service/target/**`
- `backend/media-service/target/**`
- `backend/database-baseline-tests/target/**`
- `derby.log`

**Interfaces:**
- Consumes: Task 3 POM changes and existing offline Maven repository.
- Produces: a fresh twelve-module regression result and five executable Boot JARs.

- [ ] **Step 1: Stop at the overwrite boundary**

Report the exact paths above, state that Maven may replace their generated contents, explain that Codex can run the command directly after confirmation, and request a new explicit confirmation. Do not run Maven before it arrives.

- [ ] **Step 2: Run the single full regression**

```powershell
mvn -o -gs infra/maven/global-settings.xml `
  -s infra/maven/user-settings.xml `
  "-Dmaven.repo.local=E:/Zmkk/project/infra/runtime/maven-repository" `
  -f backend/pom.xml verify
```

Capture output to the P5 evidence directory without exposing secrets. Expected: reactor `BUILD SUCCESS` and all twelve modules successful.

- [ ] **Step 3: Verify executable artifacts**

For each of the five service JARs, confirm ZIP entries contain `BOOT-INF/` and `org/springframework/boot/loader/`, and record SHA-256/size. Do not start applications in this task.

### Task 5: Combine Controlled Restart And Persistence Acceptance

**Files:**
- Read: `runtime-data/application-processes.json`
- Write: `runtime-data/application-processes.json`
- Write: `runtime-data/logs/applications/**`
- Create evidence: `test-results/p5-20260819-224207/runtime-*.json`
- Create evidence: `test-results/p5-20260819-224207/persistence-*.json`

**Interfaces:**
- Consumes: five executable JARs, the application manifest, existing MySQL/Redis/Nacos state, and existing objects user `47`, song `10`, playlist `6`, and message `6` when still present.
- Produces: one verified stop/start lifecycle, final seven-process ownership state, and post-restart persistence/security evidence.

- [ ] **Step 1: Perform a read-only process preflight**

Resolve exact listeners/PIDs/executable paths for `5173`, `5174`, and `8080-8084`; also confirm `3306`, `6380`, and `8848` remain out of the stop scope. Detect whether current PIDs are managed by the new state file and whether Codex has permission to stop them.

- [ ] **Step 2: Stop at the process-impact boundary**

Report exact seven-process targets, temporary UI/API outage, preserved infrastructure, recovery command, and any PID-specific elevated PowerShell command the user must run if access is denied. Request a separate explicit confirmation. Do not stop or start anything before it arrives.

- [ ] **Step 3: Replace only the seven old application processes**

Stop the confirmed old PIDs without touching MySQL, Redis, Nacos, or any Redis session. Then run:

```powershell
pwsh -NoProfile -File infra/scripts/start.ps1 `
  -ManifestPath infra/config/application-runtime-manifest.psd1 -Json
```

Expected: seven `Ready` records with owned PIDs and unique stdout/stderr paths. If an old PID requires UAC, pause and provide only the exact validated manual command; do not retry mechanically or bypass ownership controls.

- [ ] **Step 4: Prove controlled status, stop, and restart once**

Run `status.ps1`, then `stop.ps1`, then `start.ps1` with the application manifest. Confirm only recorded owned PIDs are stopped, a second stop reports `AlreadyStopped`, and the restart creates new owned PIDs. This is the single P5 application restart used by the persistence matrix.

- [ ] **Step 5: Verify persistence and infrastructure after restart**

Read the database password in-process from `runtime-data/identity-db.env`, expose it only to the MySQL child process, and save query results without credentials. Verify four schemas/Flyway history, user `47` remains `ADMIN/ACTIVE`, song `10` remains published/searchable, playlist `6` plus associated favorite/history/message data remain, the media object remains readable, and an HTTP Range request returns `206`. If an object was legitimately changed, record that and select another existing object rather than mutating data.

- [ ] **Step 6: Verify minimal security and routing boundaries**

Confirm Redis `PING`, five healthy Nacos service instances, Gateway routing, both frontend URL/title pairs, an unauthenticated Gateway request rejection, forged trusted headers cannot elevate access, one normal-user admin request returns `403`, and one invalid media upload is rejected without a usable media object. Keep cookies/session IDs only in process memory and redact request headers from evidence.

- [ ] **Step 7: Recheck P4 evidence validity**

Recompute the Task 1 source hash set. If only `infra/**` and the declared POM packaging changed, keep P4 frontend tests/builds/attempt 9 as reused evidence. If a frontend/API behavior source changed, stop and enter the relevant frontend build or Playwright confirmation gate before doing any additional work.

### Task 6: Produce The P5 Acceptance Handoff

**Files:**
- Create: `docs/handoffs/2026-08-19-p5-main-acceptance-handoff.md`
- Read: all P5 evidence files and reused P4 evidence

**Interfaces:**
- Consumes: Tasks 1-5 evidence.
- Produces: a reviewable P5 completion record without secrets.

- [ ] **Step 1: Separate evidence classes**

List `Fresh P5 Evidence`, `Hash-Validated P4 Reuse`, `Known Non-Blocking Issues`, and `Out Of Scope` as separate sections. Include exact commands, timestamps, exit results, artifact paths/hashes, and any manual elevated step; never include secret values or authentication headers.

- [ ] **Step 2: Reconcile the completion criteria**

Mark each of the seven design completion criteria `PASS` or `GAP` with a direct evidence path. Do not convert a missing runtime check into a pass based on source existence or an old handoff.

- [ ] **Step 3: Run final read-only checks**

Confirm branch/HEAD, preserve the dirty worktree, verify final listeners and URL/title pairs, and scan P5 evidence for prohibited secret field names or cookie/header dumps. Report remaining risk rather than starting plan-out work.

## Planned-Out Unless Triggered

- New full Playwright, screenshots, or a second three-role data set.
- Frontend rebuilds or rerunning the P4 `41/41` tests when their source hashes remain valid.
- Repeated focused backend tests after a successful full Maven regression.
- MySQL, Redis, or Nacos restart.
- Bundle splitting, dependency upgrades, UI polish, broad device matrices, and exhaustive role/endpoint permutations.
- JWT, recommendation expansion, comments/likes, playlist sharing/following, reliable outbox, CodeArts, SMTP, public deployment, course report, and defense materials.
- Git commit, merge, push, publication, worktree cleanup, artifact cleanup, session cleanup, or database cleanup.
