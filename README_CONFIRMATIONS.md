# Confirmations & Locking Module

This module freezes Work Breakdown Structure (WBS) progress for a specific date so downstream entities (tasks, plan versions, resources, audits) can rely on immutable snapshots.

## Highlights

- **WBS confirmation & locking** stored in `confirmations` and `confirmation_locks`.
- **Business rule enforcement**
  - Rule 102: `BACKDATE_ALLOWED_AFTER_LOCK`
  - Rule 201: `START_DATE_CANNOT_BE_IN_FUTURE`
  - Rule 601: `CONFIRMATION_CANNOT_BE_EARLIER_THAN_TASK_START`
  - Rule 602: `CONFIRMATION_CANNOT_BE_EMPTY`
  - Rule 603: `CONFIRMATION_UNDO_WINDOW` (service-level guard)
- **Automatic quantities** – confirmed qty = sum of day-wise task updates for the frozen date.
- **Lock-aware services**
  - `TaskUpdateService` blocks edits inside confirmed window.
  - `PlanService` blocks deleting plan versions ≤ lock date.
- **Audit coverage** – service-level logs + Flyway triggers (`trg_confirmations_audit`, `trg_confirmation_locks_audit`).

## Database Artifacts

| File | Purpose |
|------|---------|
| `V033__create_confirmations.sql` | creates `confirmations` table |
| `V034__create_confirmation_locks.sql` | creates lock cache |
| `V035__confirmation_audit_triggers.sql` | trigger-based auditing |
| `V036__seed_sample_confirmations.sql` | demo record for WBS `1.0` |

## Backend API

| Endpoint | Description | Permission |
|----------|-------------|------------|
| `POST /api/confirmations/wbs/{wbsId}` | confirm & update lock | `PAGE_CONFIRMATION_EDIT` |
| `GET /api/confirmations/wbs/{wbsId}` | list history (desc) | `PAGE_CONFIRMATION_VIEW` |
| `GET /api/confirmations/wbs/{wbsId}/summary?previewDate=` | summary + optional preview actual qty | `PAGE_CONFIRMATION_VIEW` |
| `DELETE /api/confirmations/{confirmationId}` | undo (rule 603 bound) | `PAGE_CONFIRMATION_ADMIN` |

### Service Flow (`ConfirmationService`)
1. Load WBS, tenant, earliest task start, current lock.
2. Build `BusinessRuleContext` and invoke `BusinessRuleEngine`.
3. Aggregate actual qty (task updates) and persist `ConfirmationEntity`.
4. Upsert `ConfirmationLockEntity` and push audit entries.
5. Return `WbsConfirmationSummaryDTO` for UI refresh.
6. Undo deletes confirmation, recalculates lock, validates undo window.

## Frontend

New components live under `src/pages/projects/ConfirmationPage.jsx` and supporting components:

- `ConfirmationLockBanner` – color-coded indicator (yellow editable / red locked).
- `ConfirmationHistoryTable` – action-aware table (Undo column).
- `confirmationService.js` – API wrapper.

Integrations:

- `WbsHierarchy` rows show lock badges + “Confirmations” CTA.
- `Tasks` page displays summary card + quick link to confirmation page.
- Global `RuleViolationModal` automatically surfaces rule responses from the backend.

## Testing

### Backend
- `ConfirmationServiceTest` – covers happy path, rule failures, undo lock recalculation.
- Updated `TaskUpdateServiceTest` & `PlanService*Tests` to respect new lock repository.

### Frontend
- Cypress spec `cypress/e2e/confirmation.cy.js`
  - Summary rendering
  - Preview modal
  - Rule violation (future date)
  - Conditional undo attempt

Run commands:

```bash
# Backend
cd backend
mvn clean test -Dtest=ConfirmationServiceTest,TaskUpdateServiceTest,PlanServiceTest,PlanServiceAdditionalTest

# Frontend
cd frontend
npm run test          # unit tests (if configured)
npm run cypress:open  # interactive e2e
```

## API Examples

### Confirm WBS
```http
POST /api/confirmations/wbs/10
Authorization: Bearer <token>
Content-Type: application/json

{
  "confirmationDate": "2025-11-10",
  "remarks": "Freeze progress for milestone review"
}
```

**Response**
```json
{
  "wbsId": 10,
  "wbsCode": "1.0",
  "wbsName": "Site Mobilisation",
  "lastConfirmationDate": "2025-11-10",
  "lockDate": "2025-11-10",
  "plannedQty": 100,
  "actualQty": 40,
  "confirmedQtyToDate": 25,
  "variance": 15
}
```

### Undo Confirmation
```http
DELETE /api/confirmations/55
Authorization: Bearer <token-with-PAGE_CONFIRMATION_ADMIN>
```

Returns updated summary; fails with rule 603 if undo window exceeded.

## Notes

- Confirmation dates cannot precede earliest task start nor exceed today's date (unless business rules extended).
- Undo is intentionally gated; configure Business Rule 603 to specify allowed days (eg. rule value `7`).
- Locks apply to WBS and bubble down to tasks, task updates, plan versions, resource allocations, etc.

Refer to this file whenever integrating new downstream modules with the confirmation/locking lifecycle.


