# Planning Module - Complete Documentation

## Overview

The Planning Module allows planners to define and manage plan versions with daily planned quantities for tasks. It supports versioning, revisions, validation against WBS/Task date ranges, and enforcement of business rules.

## Database Schema

### Tables

#### `plan_versions`
- `plan_version_id` (BIGINT, PK) - Primary key
- `tenant_id` (BIGINT, FK) - Tenant reference
- `task_id` (BIGINT, FK) - Task reference
- `version_no` (INT) - Version number (1, 2, 3...)
- `version_date` (DATE) - Version creation date
- `description` (VARCHAR(500)) - Optional description
- `is_active` (BIT) - Whether this version is currently active
- `activate_flag` (BIT) - Soft delete flag
- `created_by` (BIGINT) - User who created
- `created_on` (DATETIME2) - Creation timestamp
- `updated_by` (BIGINT) - User who last updated
- `updated_on` (DATETIME2) - Last update timestamp

**Constraints:**
- Unique constraint: `(task_id, version_no)` - Each task can only have one version with a given number
- Index: `(tenant_id, task_id, version_no)` for performance

#### `plan_lines`
- `plan_line_id` (BIGINT, PK) - Primary key
- `tenant_id` (BIGINT, FK) - Tenant reference
- `plan_version_id` (BIGINT, FK) - Plan version reference
- `task_id` (BIGINT, FK) - Task reference
- `line_number` (INT) - Line sequence number
- `work_date` (DATE) - Date for which quantity is planned
- `planned_qty` (DECIMAL(18,2)) - Planned quantity for the date
- `description` (VARCHAR(500)) - Optional description
- `activate_flag` (BIT) - Soft delete flag
- `created_by` (BIGINT) - User who created
- `created_on` (DATETIME2) - Creation timestamp
- `updated_by` (BIGINT) - User who last updated
- `updated_on` (DATETIME2) - Last update timestamp

**Constraints:**
- Unique constraint: `(plan_version_id, work_date)` - Each version can only have one line per date
- Index: `(plan_version_id, work_date)` for performance
- Business rule: `work_date` must be within `task.start_date` → `task.end_date`

## Business Rules Enforced

### Rule 101: BACKDATE_ALLOWED_TILL
- Validates if backdated plan lines are allowed based on tenant configuration
- Throws `BusinessRuleException` if backdating is not allowed

### Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
- Plan lines cannot be created for future dates if rule enforces
- Validates against today's date

### Rule 202: END_DATE_CANNOT_BE_BEFORE_START_DATE
- Plan date ranges must always be valid
- Also validates that plan line dates are within task date range

### Rule 301: CONFIRMED_TASK_CANNOT_BE_MODIFIED
- Plan versions cannot be deleted if task is confirmed
- Checked before deletion

### Rule 402: PLAN_VERSION_DATE_VALIDATION
- Plan version date cannot be in the future
- Validated when creating or reverting versions

## API Endpoints

### GET `/api/plans/task/{taskId}`
List all plan versions for a task.

**Response:** `List<PlanVersionDTO>`

**Authorization:** `PAGE_PLAN_VIEW` or `PAGE_PLAN_EDIT`

### POST `/api/plans/task/{taskId}`
Create a new plan version with lines.

**Request Body:** `PlanVersionCreateDTO`
```json
{
  "taskId": 1,
  "versionDate": "2025-11-05",
  "description": "Initial plan",
  "lines": [
    {
      "lineNumber": 1,
      "workDate": "2025-11-05",
      "plannedQty": 10.00,
      "description": "Day 1"
    }
  ]
}
```

**Response:** `PlanVersionDTO`

**Authorization:** `PAGE_PLAN_EDIT`

### PUT `/api/plans/{planVersionId}/activate`
Set a plan version as active (deactivates all other versions for the task).

**Response:** `PlanVersionDTO`

**Authorization:** `PAGE_PLAN_EDIT`

### GET `/api/plans/{planVersionId}`
Get plan version details.

**Response:** `PlanVersionDTO`

**Authorization:** `PAGE_PLAN_VIEW` or `PAGE_PLAN_EDIT`

### GET `/api/plans/{planVersionId}/lines`
Get all plan lines for a version (sorted by work_date).

**Response:** `List<PlanLineDTO>`

**Authorization:** `PAGE_PLAN_VIEW` or `PAGE_PLAN_EDIT`

### PUT `/api/plans/{planVersionId}/lines`
Create or update plan lines for a version.

**Request Body:** `List<PlanLineCreateDTO>`

**Response:** `PlanVersionDTO`

**Authorization:** `PAGE_PLAN_EDIT`

### DELETE `/api/plans/{planVersionId}`
Delete a plan version (only if no confirmations applied).

**Response:** `204 No Content`

**Authorization:** `PAGE_PLAN_EDIT`

### POST `/api/plans/create-with-mode`
Create plan version using one of three modes.

**Request Body:** `PlanCreationModeDTO`

**Modes:**
1. **DAILY_ENTRY** - Manual entry of daily plan lines
2. **DATE_RANGE_SPLIT** - Split a date range with quantity distribution
3. **SINGLE_LINE_QUICK** - Quick single-line plan

**Authorization:** `PAGE_PLAN_EDIT`

### GET `/api/plans/compare/{versionId1}/{versionId2}`
Compare two plan versions side-by-side.

**Response:** `PlanVersionComparisonDTO`

**Authorization:** `PAGE_PLAN_VIEW` or `PAGE_PLAN_EDIT`

## Plan Creation Modes

### 1. Daily Entry Mode
Manually enter plan lines for each day:
```json
{
  "mode": "DAILY_ENTRY",
  "dailyLines": [
    {"plannedDate": "2025-11-05", "plannedQty": 10.00},
    {"plannedDate": "2025-11-06", "plannedQty": 12.00}
  ]
}
```

### 2. Date Range Split Mode
Automatically split quantity across a date range:
```json
{
  "mode": "DATE_RANGE_SPLIT",
  "rangeSplit": {
    "startDate": "2025-11-05",
    "endDate": "2025-11-14",
    "totalQty": 100.00,
    "splitType": "EQUAL_SPLIT"
  }
}
```

**Split Types:**
- `EQUAL_SPLIT` - Split equally across all days
- `WEEKLY_SPLIT` - Split by weeks
- `MONTHLY_SPLIT` - Split by months
- `CUSTOM_SPLIT` - Custom quantity per period

### 3. Single-Line Quick Mode
Create a plan with a single entry:
```json
{
  "mode": "SINGLE_LINE_QUICK",
  "singleLine": {
    "plannedDate": "2025-11-05",
    "plannedQty": 50.00
  }
}
```

## Audit Logging

All changes to plan versions and plan lines are automatically logged via database triggers:

- **Table:** `audit_logs`
- **Table Names:** `PLAN_VERSION`, `PLAN_LINES`
- **Actions:** `INSERT`, `UPDATE`, `DELETE`
- **Fields Logged:**
  - Old data (JSON) for UPDATE/DELETE
  - New data (JSON) for INSERT/UPDATE
  - Changed by (user ID)
  - Changed on (timestamp)

## Seed Data

Seed data is provided in migration `023-seed-plan-versions.xml`:

- **Task:** "Excavation Zone A / Trench Digging"
- **Version 1:** 10 days, 10 cubic meters per day (total: 100)
- **Version 2:** 10 days, 12 cubic meters per day (total: 120)
- **Date Range:** 2025-11-05 to 2025-11-14

## Frontend Integration

### Required Updates

The frontend needs to be updated to use the new field names:

**Old → New:**
- `versionId` → `planVersionId`
- `versionNumber` → `versionNo`
- `isCurrent` → `isActive`
- `lineId` → `planLineId`
- `plannedDate` → `workDate`
- `versionId` (in PlanLineDTO) → `planVersionId`

### Components

1. **PlanVersions Page** (`/pages/projects/PlanVersions.jsx`)
   - List versions with active indicator
   - Create new version
   - Activate version
   - Delete version
   - Compare versions

2. **PlanEditor Component** (`/components/PlanEditor.jsx`)
   - Support all 3 creation modes
   - Inline validation
   - Business rule violation handling

3. **PlanVersionComparison Component** (`/components/PlanVersionComparison.jsx`)
   - Side-by-side comparison
   - Highlight differences

4. **Task Plan Summary Widget**
   - Total planned quantity
   - Total actual quantity (from updates)
   - Variance
   - Active version badge

## Testing

### Backend Tests

- Unit tests for plan version creation
- Unit tests for business rule violations
- Integration tests for full CRUD operations
- Edge case: Date outside task range

### Frontend Tests

- Cypress tests:
  - Create version
  - Add plan lines
  - Validate rule violation modal
  - Activate version

## Migration Notes

The schema was updated in migration `022-update-plan-schema-and-add-audit.xml`:

1. Renamed columns to match requirements
2. Added unique constraints
3. Updated indexes
4. Added audit triggers

**Important:** If you have existing data, the migration will rename columns. Ensure you have a backup before running.

## Future Enhancements

- Plan template support
- Bulk import/export
- Plan approval workflow
- Integration with resource planning
- Advanced analytics and reporting

