# Resource Allocation Module

Comprehensive module for allocating manpower and equipment to WBS tasks in a multi-tenant environment. The module enforces business rules, prevents overlaps, calculates rate-based costs, and writes audit trails for every change.

---

## Architecture Overview

- **Entities:** `EmployeeEntity`, `EquipmentEntity`, `ManpowerAllocationEntity`, `EquipmentAllocationEntity`
- **Repositories:** Tenant-aware repositories with date-range filtering and overlap lookups
- **Service Layer:** `ResourceAllocationService` (allocation orchestration, validations, audit logging) and `ResourceCostCalculator` (shared cost engine)
- **Business Rules:** Enforced via `BusinessRuleEngine` with additional validators (`AllocationWindowRuleValidator`, `AllocationOverlapRuleValidator`)
- **API Controller:** `ResourceAllocationController` exposing CRUD, lookup, timeline, and cost endpoints under `/api/resources`
- **Database:** Liquibase change set `026-resource-allocation-module.xml` sources the SQL scripts `V029`–`V032` to create master/transaction tables, audit triggers, seed data, and resource-specific rules
- **Frontend:** `ResourceAllocationPage` with Tailwind UI, allocation modal, timeline, summary widgets, and Cypress coverage

---

## Database Objects

| Change Set / SQL | Description |
|------------------|-------------|
| `db/changelog/changes/026-resource-allocation-module.xml` | Liquibase wrapper that executes the SQL Server scripts below in order |
| `V029__create_employees_equipment.sql` | Employees & equipment master tables with tenant indexes |
| `V030__create_allocations.sql` | Manpower & equipment allocation tables + costing columns |
| `V031__create_allocation_audit_triggers.sql` | SQL Server triggers writing to `audit_logs` |
| `V032__seed_resources.sql` | Sample employees/equipment/allocations + business rules 601/602 |

Tables include audit columns (`created_by/on`, `updated_by/on`) and snapshot rate fields (`daily_rate`, `total_cost`) for historical accuracy.

### Applying the Liquibase change set

| Scenario | Command |
|----------|---------|
| Run as part of app startup (recommended) | `mvn spring-boot:run` — Liquibase automatically applies pending change sets using the datasource defined in `application.yml`. |
| Manual migration without launching the app | `mvn liquibase:update -Dliquibase.url="jdbc:sqlserver://localhost:1433;databaseName=elina_test;encrypt=false" -Dliquibase.username=sa -Dliquibase.password=****` (override the URL/credentials as needed). |

Ensure SQL Server is reachable before running either flow; Liquibase tracks executions in `DATABASECHANGELOG` (now listing 45 change sets after the resource module).

---

## Business Rule Mapping

| Rule # | Name | Validator | Description |
|--------|------|-----------|-------------|
| 101 | `BACKDATE_ALLOWED_TILL` | `BackdateRuleValidator` | Limits retroactive allocations |
| 102 | `BACKDATE_ALLOWED_AFTER_LOCK` | `BackdateRuleValidator` | Blocks allocations before lock date unless permitted |
| 501 | `ALLOCATION_START_END_DATE_MUST_BE_VALID` | `AllocationDateRuleValidator` | Ensures end ≥ start |
| 601 | `ALLOCATION_WITHIN_WBS_DATES_ONLY` | `AllocationWindowRuleValidator` | Allocation window must sit inside WBS dates |
| 602 | `ALLOCATION_CANNOT_OVERLAP_EXISTING` | `AllocationOverlapRuleValidator` | Prevents overlapping resource bookings per WBS |

All rules are enforced inside `ResourceAllocationService.validateAllocationRules(...)` before any persistence action.

---

## REST API Summary

### Manpower
- `POST /api/resources/manpower` – create allocation
- `PUT /api/resources/manpower/{id}` – update allocation
- `DELETE /api/resources/manpower/{id}` – delete allocation
- `GET /api/resources/manpower/wbs/{wbsId}` – list allocations for WBS
- `GET /api/resources/manpower/options` – dropdown data

### Equipment
- `POST /api/resources/equipment`
- `PUT /api/resources/equipment/{id}`
- `DELETE /api/resources/equipment/{id}`
- `GET /api/resources/equipment/wbs/{wbsId}`
- `GET /api/resources/equipment/options`

### Cost & Timeline
- `GET /api/resources/cost/wbs/{wbsId}` – aggregated manpower/equipment costs
- `GET /api/resources/cost/manpower/{employeeId}?startDate&endDate`
- `GET /api/resources/cost/equipment/{equipmentId}?startDate&endDate`
- `GET /api/resources/timeline/wbs/{wbsId}` – timeline rows for UI

All write operations require `PAGE_PROJECTS_EDIT` (or system admin). Reads require `PAGE_PROJECTS_VIEW` or higher.

---

## Service-Level Workflow

1. Resolve tenant ownership for WBS/resource IDs.
2. Build `BusinessRuleContext` with allocation + WBS metadata.
3. Execute rule chain (501, 601, 101, 102, 602).
4. Calculate `durationDays`, snapshot rates, and `totalCost`.
5. Persist allocation (tenant-aware) and emit audit log via `AuditLogService`.
6. Update timeline/cost summary caches through repository aggregations.

`ResourceCostCalculator` can be injected anywhere to obtain `{ totalDays, ratePerDay, totalCost }` for reporting.

---

## Frontend Deliverables

- `ResourceAllocationPage.jsx`: tabbed view for manpower/equipment, summary widget, timeline, and inline actions.
- `AllocationModal.jsx`: shared modal with resource selector, date pickers, hours, remarks, and real-time cost preview.
- `ResourceSummaryWidget.jsx`: KPI cards for manpower/equipment/total cost.
- `AllocationTimeline.jsx`: simple Gantt-style visualization mapped to WBS duration.
- `resourceService.js`: Axios helpers covering every backend endpoint.
- `Cypress e2e/resource_allocation.cy.js`: mocks API responses and tests allocation flow (load, preview cost, create allocation).

---

## Testing

### Backend
- `ResourceCostCalculatorTest` – validates rate × duration math & missing resource handling.
- `AllocationOverlapRuleValidatorTest` – ensures overlaps raise `BusinessRuleException`.
- `ResourceAllocationServiceTest` – verifies rule chain invocation and cost snapshot logic.

Run: `mvn -pl backend test -Dtest=ResourceAllocation*`

### Frontend
- `npm run test:e2e -- resource_allocation.cy.js` (requires Cypress config) – mocked happy path for allocation creation and preview.

---

## Seed Data Highlights

`V032__seed_resources.sql` inserts:
- 10 employees (Ram Kumar, Suresh Das, etc.) with varied skills/rates.
- 5 equipment records (JCB Excavator, Concrete Mixer, etc.).
- Sample allocations for WBS 2.1 (or first available WBS) covering 10-day manpower + 5-day equipment usage.

These seeds allow immediate UI verification after Flyway migration.

---

## Operational Notes

- All repositories derive tenant ID from `TenantContext`; never bypass `ResourceAllocationService` for writes.
- Audit triggers mirror service-level logging, guaranteeing database-level traceability even for ad-hoc SQL updates.
- Frontend listens for `businessRuleViolation` events (raised by Axios interceptor) and shows `RuleViolationModal`.
- Cost summary relies on persisted `total_cost`; manual DB edits must update snapshot columns to keep UI accurate.


