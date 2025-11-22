# Elina Multi-Tenant Project Management System

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green) ![React](https://img.shields.io/badge/React-18-blue) ![Status](https://img.shields.io/badge/Status-Production%20Ready-success)

Full-stack reference implementation for Elina’s construction/project-management platform. The workspace contains:

- **backend/** – Spring Boot 3 + MS SQL Server (Liquibase, multi-tenant security, business rules)
- **frontend/** – React 18 + Vite + Tailwind + Cypress

This README explains how to run everything locally and points to module-level docs.

---

## Repo Structure

```
elina_test/
├── backend/                     # Spring Boot application (Authorization + Projects modules)
│   ├── src/main/java/com/elina  # Code by domain (authorization, projects, resources…)
│   ├── src/main/resources       # Liquibase changelog + SQL scripts + config
│   ├── README_*                 # Module-specific documentation (planning, resources, etc.)
│   └── pom.xml
├── frontend/                    # React + Tailwind (Vite)
│   ├── src/components           # Shared UI components
│   ├── src/pages                # Route-level pages (projects, resources…)
│   ├── cypress/e2e              # Cypress integration tests
│   └── package.json
├── README_DAYWISE_UPDATES.md    # Backend/frontend coverage for day-wise updates module
├── FRONTEND_COMPLETION_STATUS.md# Snapshot of React deliverables
└── README.md                    # You are here
```

Module-specific documentation:

| Module | Doc |
|--------|-----|
| Planning | `backend/README_PLANNING.md` |
| Business Rules | `backend/README_BUSINESS_RULES.md` |
| Master Data | `backend/README_MASTER_DATA.md` |
| Resource Allocation | `backend/README_RESOURCES.md` |
| Day-Wise Updates | `README_DAYWISE_UPDATES.md` + `FRONTEND_COMPLETION_STATUS.md` |

---

## Quick Start (TL;DR)

1. **Start SQL Server** and create the `elina_test` database (defaults in `application.yml`).
2. **Backend**
   ```bash
   cd backend
   mvn clean spring-boot:run
   ```
   Liquibase auto-migrates the schema (45 change sets, including resource allocations).
3. **Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
4. Visit `http://localhost:5173`, sign in with a seeded account (see “Default Credentials”), and navigate to the Projects → Resource Allocation screen.

---

## Prerequisites

| Tool | Version / Notes |
|------|-----------------|
| Java | 17.x (Temurin, OpenJDK, etc.) |
| Maven | 3.9+ |
| Node.js | 18+ (ships with npm 9+) |
| SQL Server | 2019+ (developer/express OK) |
| Redis (optional) | Local Redis for cache layer; can be disabled via Spring config |

> **Tip:** On Windows, ensure `sqlcmd` or Azure Data Studio is available to verify the database quickly.

---

## Environment Variables

### Backend

| Variable | Default | Notes |
|----------|---------|-------|
| `DB_USERNAME` | `sa` | SQL Server login |
| `DB_PASSWORD` | `elina@123` | Change in production |
| `DB_URL` (optional) | (from `application.yml`) | Override the JDBC URL entirely if needed |
| `JWT_SECRET` | placeholder | Provide a 256-bit key for real deployments |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Set if using Redis |
| `MASTER_DATA_CACHE_ENABLED` | `true` | Toggle caches quickly |

### Frontend

| Variable | Default | Notes |
|----------|---------|-------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Set in `.env` for prod/staging |
| `VITE_TENANT_CODE` (optional) | `DEFAULT` | Used by auth bootstrap if multi-tenant selection is needed |

Create a `.env` (frontend) or rely on `application.yml` (backend) for overrides. For container deployments, use environment variables exclusively.

---

## Database Setup (Liquibase)

1. Create a SQL Server database named `elina_test`.
2. Ensure the login in `backend/src/main/resources/application.yml` has db_owner rights:
   ```yaml
   spring:
     datasource:
       url: jdbc:sqlserver://localhost:1433;databaseName=elina_test;encrypt=false;trustServerCertificate=true
       username: sa
       password: elina@123
   ```
   Override via env vars `DB_USERNAME` / `DB_PASSWORD` as needed.
3. Run the backend once (see next section) or execute Liquibase manually to bootstrap the schema. The resource allocation change set is registered in `db/changelog/changes/026-resource-allocation-module.xml`, which calls SQL scripts `V029__`–`V032__`.

Manual Liquibase run (optional):
```bash
cd backend
mvn liquibase:update \
  -Dliquibase.url="jdbc:sqlserver://localhost:1433;databaseName=elina_test;encrypt=false;trustServerCertificate=true" \
  -Dliquibase.username=sa \
  -Dliquibase.password=yourPassword
```

Liquibase tracks execution in `DATABASECHANGELOG` (should show 45 change sets after the resource module).

---

## Default Credentials (Sample Data)

Liquibase seeds the following users (see `db/changelog/changes/010-seed-data.xml`):

| Username | Password | Role |
|----------|----------|------|
| `admin@elina.com` | `Admin@123` | System Admin |
| `project.manager@elina.com` | `Manager@123` | Project Manager |
| `site.engineer@elina.com` | `Engineer@123` | Site Engineer |

Use these to explore the UI. Update passwords immediately in real environments.

---

## Backend – Spring Boot

```bash
cd backend
mvn clean spring-boot:run
# or package
mvn clean package
java -jar target/authorization-module-1.0.0.jar
```

Key runtime env vars:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_USERNAME`, `DB_PASSWORD` | `sa` / `elina@123` | SQL Server credentials |
| `REDIS_HOST`, `REDIS_PORT` | `localhost` / `6379` | Redis cache |
| `JWT_SECRET` | placeholder | Replace in production |

Backend entry point: `com.elina.AuthorizationModuleApplication`.

### Tests

```bash
cd backend
mvn test
# or limit to resource module
mvn -pl backend test -Dtest=ResourceAllocation*
```

---

## Frontend – React + Vite

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build
npm run preview
```

Configure the API base URL in `frontend/src/services/api.js` (defaults to `http://localhost:8080`).

### Cypress E2E

```bash
cd frontend
npm run test:e2e -- resource_allocation.cy.js
```

---

## Verifying the Resource Allocation Module

1. Start backend + frontend.
2. Log in via frontend (default seed users defined in Liquibase change sets).
3. Navigate to a project → WBS → “Resource Allocation”.
4. Use “Allocate Resource” to test manpower/equipment flows. Cost preview should call:
   - `GET /api/resources/cost/manpower/{employeeId}`
   - `GET /api/resources/cost/equipment/{equipmentId}`
5. Confirm timeline + summary widgets update automatically.

If you run into rule violations, the backend returns structured errors (`BusinessRuleException`) and the frontend displays `RuleViolationModal`.

---

## Common Issues

| Symptom | Fix |
|---------|-----|
| `Schema-validation: missing table [employees]` | Ensure Liquibase change set 026 ran (start backend once or run `mvn liquibase:update`). |
| Cannot connect to SQL Server | Verify instance listening on `localhost:1433`, `encrypt=false` or provide certificates, ensure login/password correct. |
| Redis not running | Either start Redis or disable the cache via `MASTER_DATA_CACHE_ENABLED=false`. |
| CORS errors in frontend | Update `spring.web.cors` configuration or use the same origin. |

---

## Deployment Notes

- Backend is a single Spring Boot jar; configure environment variables and use the built-in Liquibase runner for schema migrations.
- Frontend build is static (Vite). Serve via Nginx, Azure Static Web Apps, etc.
- Keep `JWT_SECRET` and DB credentials in secure vaults for production.
- For containerization, create:
  - Backend Dockerfile using Azul Temurin 17 base, copy `target/authorization-module-1.0.0.jar`, expose `8080`.
  - Frontend Dockerfile using `node:18` for build stage + `nginx:alpine` for runtime (copy `dist/` to `/usr/share/nginx/html`).
  - Compose file linking SQL Server, Redis, backend, frontend; ensure health checks wait for Liquibase to finish before exposing the API.

---

## Contributing / Next Steps

1. Fork this repo on GitHub.
2. Add your feature branch.
3. Follow existing coding standards (Lombok, DTOs, TenantContext usage).
4. Update relevant module README + root README for any new environment steps.

For questions about specific modules, open the module README listed above.

Happy building! 🚀

