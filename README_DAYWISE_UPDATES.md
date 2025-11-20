# Day-Wise Updates Module

Complete implementation of the Day-Wise Updates Module for the Elina multi-tenant project management system.

## Overview

This module enables daily progress updates (plan_qty + actual_qty) for each task, with comprehensive business rule validation, confirmation lock integration, and audit logging.

## Features

### Backend (Spring Boot)

1. **Entities & Repositories**
   - `TaskUpdateEntity` - Maps to `task_updates` table
   - `TaskUpdateRepository` - Tenant-aware repository with methods:
     - `findByTaskIdAndUpdateDate()` - Find update for specific date
     - `findByTaskIdOrderByUpdateDate()` - Get all updates ordered by date
     - `findForTenant()` - Get all updates for current tenant

2. **Services**
   - `TaskUpdateService` with methods:
     - `getUpdatesForTask(taskId)` - Merges plan lines with existing updates
     - `saveOrUpdateDayWise(taskId, updates[])` - Bulk save/update with validation
     - `deleteTaskUpdate(updateId)` - Delete with business rule validation
     - `getDailySummary(taskId, dateRange)` - Summary for reporting

3. **Business Rule Integration**
   - Rule 101: BACKDATE_ALLOWED_TILL - Validates backdate restrictions
   - Rule 102: BACKDATE_ALLOWED_AFTER_LOCK - Validates updates on locked dates
   - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY - Prevents actual > plan
   - Date range validation: Update date must be within task.start_date → task.end_date

4. **Confirmation Locks Integration**
   - Checks `confirmation_locks` table for WBS-level locks
   - Blocks updates on dates <= lock_date (unless Rule 102 allows)

5. **Audit Logging**
   - Every insert/update/delete generates audit log entry
   - Database trigger `trg_task_updates_audit` automatically logs changes
   - Audit logs stored in `audit_logs` table

6. **API Endpoints**
   - `GET /api/task-updates/task/{taskId}` - Get unified day-wise updates (merges plan lines)
   - `GET /api/task-updates/task/{taskId}/list` - Get simple list of updates
   - `POST /api/task-updates/task/{taskId}` - Bulk save/update day-wise entries
   - `POST /api/task-updates` - Single update (legacy endpoint)
   - `DELETE /api/task-updates/{updateId}` - Delete update
   - `GET /api/task-updates/task/{taskId}/summary?from=&to=` - Get daily summary

### Frontend (React + Tailwind)

1. **DayWiseGrid Component**
   - Excel-like grid interface
   - Columns: Date, Planned Qty, Actual Qty (editable), Variance, Remarks
   - Auto-calculates variance on change
   - Color indicators:
     - Green: actual <= plan
     - Red: actual > plan (prevents submission)
   - Bulk update mode for date range selection
   - Shows locked dates (non-editable)
   - Real-time validation

2. **TaskUpdatePage**
   - Task summary header with key information
   - Plan version information display
   - Integrated DayWiseGrid component
   - Save button with validation
   - Rule violation modal integration

3. **RuleViolationModal Integration**
   - Displays business rule violations
   - Shows rule number, message, and hint
   - Appears automatically on rule violations

## Database Schema

### task_updates Table

```sql
- update_id (PK, BIGINT, IDENTITY)
- tenant_id (FK to tenants)
- task_id (FK to tasks)
- update_date (DATE, NOT NULL)
- planned_qty (DECIMAL(18,2))
- actual_qty (DECIMAL(18,2), NOT NULL)
- remarks (VARCHAR(1000))
- created_by (BIGINT)
- created_on (DATETIME2, NOT NULL)
- updated_by (BIGINT)
- updated_on (DATETIME2)

Unique Constraint: (tenant_id, task_id, update_date)
```

### Indexes

- `idx_task_updates_tenant_id` - Tenant filtering
- `idx_task_updates_task_id` - Task filtering
- `idx_task_updates_date` - Date queries (unique)
- `idx_task_updates_date_range` - Date range queries
- `idx_task_updates_summary` - Summary/reporting queries

### Audit Trigger

- `trg_task_updates_audit` - Automatically logs INSERT, UPDATE, DELETE operations

## Business Rules

### Rule 101: BACKDATE_ALLOWED_TILL
- Validates that update_date is not older than allowed backdate period
- Context: tenantId, userId, updateDate

### Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
- Validates updates on dates that are locked by confirmation
- If lock_date >= update_date, updates are blocked unless rule allows
- Context: tenantId, userId, updateDate, confirmationDate

### Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY
- Validates that actual_qty <= plan_qty
- Context: tenantId, userId, plannedQty, actualQty

### Date Range Validation
- Update date must be within task.start_date → task.end_date
- Validated in service layer before business rules

## Usage Examples

### Backend API Usage

#### Get Day-Wise Updates (Merged with Plan Lines)
```bash
GET /api/task-updates/task/123
Authorization: Bearer <token>

Response: [
  {
    "updateDate": "2025-11-05",
    "planQty": 10.00,
    "actualQty": 10.00,
    "variance": 0.00,
    "updateId": 1,
    "remarks": "On track",
    "isLocked": false,
    "canEdit": true
  },
  ...
]
```

#### Bulk Save Day-Wise Updates
```bash
POST /api/task-updates/task/123
Authorization: Bearer <token>
Content-Type: application/json

{
  "taskId": 123,
  "updates": [
    {
      "updateDate": "2025-11-05",
      "planQty": 10.00,
      "actualQty": 10.00,
      "remarks": "Day 1 complete"
    },
    {
      "updateDate": "2025-11-06",
      "planQty": 10.00,
      "actualQty": 8.00,
      "remarks": "Slight delay"
    }
  ]
}
```

#### Get Daily Summary
```bash
GET /api/task-updates/task/123/summary?from=2025-11-05&to=2025-11-14
Authorization: Bearer <token>

Response: [
  {
    "date": "2025-11-05",
    "planQty": 10.00,
    "actualQty": 10.00,
    "variance": 0.00
  },
  ...
]
```

### Frontend Usage

```jsx
import DayWiseGrid from '../components/DayWiseGrid'

function TaskUpdatePage() {
  const taskId = 123
  
  return (
    <DayWiseGrid
      taskId={taskId}
      onSave={() => console.log('Saved!')}
      onError={(error) => console.error(error)}
    />
  )
}
```

## Permissions

- `PAGE_TASK_UPDATE_VIEW` - View day-wise updates
- `PAGE_TASK_UPDATE_EDIT` - Create/edit/delete day-wise updates
- `PAGE_PROJECTS_VIEW` - Alternative permission for viewing
- `PAGE_PROJECTS_EDIT` - Alternative permission for editing

## Seed Data

Migration `V028__seed_task_updates.sql` includes sample data for tenant `TNT_DEMO`:
- Task: "Excavation Zone A – Trench Digging"
- Date Range: 2025-11-05 to 2025-11-14
- Day 1-3: actual = plan
- Day 4-5: actual < plan
- Day 6: Should be blocked by Rule 401 (actual > plan)
- Day 7-10: Random values <= plan
- Day 10: Locked by confirmation (no updates allowed)

## Testing

### Backend Tests

Unit tests should cover:
- Rule 101 violation (backdate too old)
- Rule 102 violation (update on locked date)
- Rule 401 violation (actual > plan)
- Date range validation (outside task range)
- Confirmation lock blocking
- Audit log generation

### Frontend Tests

Cypress tests should cover:
- Enter valid day-wise updates
- Trigger rule violation (actual > plan)
- Confirm lock error display
- Save success flow
- Bulk update mode

## Migration Files

1. `V026__task_update_indexes.sql` - Additional indexes for performance
2. `V027__task_update_audit_trigger.sql` - Audit trigger for automatic logging
3. `V028__seed_task_updates.sql` - Sample seed data

## Notes

- All operations are tenant-aware (filtered by tenant_id from JWT)
- Date/time values are serialized as JSON using standard LocalDate/LocalDateTime
- Business rules are validated before any database operations
- Audit logs are written both programmatically and via database trigger
- Plan quantities are merged from active plan version's plan_lines
- For date gaps, plan_qty comes from plan_lines, actual_qty = 0

## Future Enhancements

- Export to Excel functionality
- Import from Excel functionality
- Variance trend charts
- Real-time collaboration (WebSocket updates)
- Mobile-responsive grid improvements
- Advanced filtering and sorting

