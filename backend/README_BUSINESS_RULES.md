# Business Rule Engine Module

## Overview

The Business Rule Engine module provides **ACTIVE VALIDATION** that prevents actions violating business rules. Rules are not passive - they actively block invalid operations across the entire system.

## Core Concept

**Business rules are ACTIVE VALIDATORS** - they prevent actions that violate rules, not just log violations. When a rule is violated, a `BusinessRuleException` is thrown, which blocks the operation.

## Features

- **Active Validation**: Rules prevent invalid operations, not just log them
- **Tenant-Aware**: All rules are scoped to tenants
- **Rule Registry**: Automatic registration of validators by rule number
- **Caching**: Rules are cached for performance
- **Extensible**: Easy to add new rules and validators
- **Centralized Logic**: All validation logic in one place

## Database Schema

### business_rules Table

```sql
CREATE TABLE business_rules (
    rule_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_number INT NOT NULL,
    control_point VARCHAR(100) NOT NULL,
    applicability VARCHAR(1) NOT NULL, -- Y or N
    rule_value VARCHAR(500),
    description VARCHAR(1000),
    activate_flag BIT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_on DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by BIGINT,
    updated_on DATETIME2,
    
    CONSTRAINT FK_business_rules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT UQ_business_rules_tenant_rule_number UNIQUE (tenant_id, rule_number)
);
```

**Indexes:**
- `idx_business_rules_tenant_rule_number` on (tenant_id, rule_number) - unique
- `idx_business_rules_tenant_control_point` on (tenant_id, control_point)
- `idx_business_rules_tenant_active` on (tenant_id, activate_flag)

## Default Business Rules

The seed data includes the following default rules:

### Rule 101: BACKDATE_ALLOWED_TILL
- **Control Point**: TASK_UPDATE
- **Rule Value**: 7 (days)
- **Description**: Maximum number of days allowed for backdating task updates
- **Validation**: Prevents backdating more than 7 days

### Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
- **Control Point**: TASK_UPDATE
- **Rule Value**: N
- **Description**: Whether backdating is allowed after lock date
- **Validation**: Prevents backdating before lock date

### Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
- **Control Point**: TASK
- **Description**: Task start date cannot be in the future
- **Validation**: Prevents scheduling tasks with future start dates

### Rule 202: WBS_DATE_RANGE_VALIDATION
- **Control Point**: WBS
- **Description**: WBS end date cannot be before start date
- **Validation**: Ensures valid date ranges for work breakdown structures

### Rule 203: ALLOCATION_DATE_RANGE_VALIDATION
- **Control Point**: ALLOCATION
- **Description**: Resource allocation end date cannot be before start date
- **Validation**: Ensures valid allocation periods

### Rule 204: ATTENDANCE_DATE_VALIDATION
- **Control Point**: ATTENDANCE
- **Description**: Attendance date cannot be in the future
- **Validation**: Attendance can only be recorded for today or past dates

### Rule 205: MATERIAL_USAGE_VALIDATION
- **Control Point**: MATERIAL_USAGE
- **Description**: Material usage date cannot be in the future and quantity cannot be negative
- **Validation**: Ensures valid material usage entries

### Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
- **Control Point**: CONFIRMATION
- **Description**: Confirmed entries cannot be modified or overwritten
- **Validation**: Prevents modification of confirmed entries

### Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY
- **Control Point**: TASK_UPDATE
- **Description**: Daily update quantity cannot exceed the planned quantity
- **Validation**: Prevents over-reporting of actual work done

### Rule 402: PLAN_VERSION_DATE_VALIDATION
- **Control Point**: PLAN_VERSION
- **Description**: Plan version date must be valid and cannot be in the future
- **Validation**: Ensures plan versions are created with valid dates

### Rule 501: ALLOCATION_START_END_DATE_MUST_BE_VALID
- **Control Point**: ALLOCATION
- **Description**: Manpower allocation end date must be on or after the start date
- **Validation**: Ensures valid allocation date ranges

### Rule 601: ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE
- **Control Point**: ATTENDANCE
- **Description**: Attendance date cannot be in the future
- **Validation**: Attendance can only be posted for today or past dates

## API Endpoints

### List Business Rules
```
GET /api/business-rules
```
**Response:** List<BusinessRuleDTO>

**Permissions:** `PAGE_BUSINESS_RULES_VIEW` or `PAGE_BUSINESS_RULES_EDIT`

### Get Business Rule by ID
```
GET /api/business-rules/{id}
```
**Response:** BusinessRuleDTO

**Permissions:** `PAGE_BUSINESS_RULES_VIEW` or `PAGE_BUSINESS_RULES_EDIT`

### Get Business Rule by Number
```
GET /api/business-rules/by-number/{ruleNumber}
```
**Response:** BusinessRuleDTO

**Permissions:** `PAGE_BUSINESS_RULES_VIEW` or `PAGE_BUSINESS_RULES_EDIT`

### Get All Control Points
```
GET /api/business-rules/control-points
```
**Response:** List<String>

**Permissions:** `PAGE_BUSINESS_RULES_VIEW` or `PAGE_BUSINESS_RULES_EDIT`

### Create Business Rule
```
POST /api/business-rules
Content-Type: application/json

{
  "ruleNumber": 101,
  "controlPoint": "TASK_UPDATE",
  "applicability": "Y",
  "ruleValue": "7",
  "description": "Maximum 7 days for backdating",
  "activateFlag": true
}
```
**Response:** BusinessRuleDTO (201 Created)

**Permissions:** `PAGE_BUSINESS_RULES_EDIT` or `ROLE_SYSTEM_ADMIN`

### Update Business Rule
```
PUT /api/business-rules/{id}
Content-Type: application/json

{
  "ruleNumber": 101,
  "controlPoint": "TASK_UPDATE",
  "applicability": "Y",
  "ruleValue": "10",
  "description": "Maximum 10 days for backdating",
  "activateFlag": true
}
```
**Response:** BusinessRuleDTO

**Permissions:** `PAGE_BUSINESS_RULES_EDIT` or `ROLE_SYSTEM_ADMIN`

### Toggle Activate Flag
```
PUT /api/business-rules/{id}/activate-toggle
```
**Response:** BusinessRuleDTO (with toggled activateFlag)

**Permissions:** `PAGE_BUSINESS_RULES_EDIT` or `ROLE_SYSTEM_ADMIN`

**Description:** Toggles the `activate_flag` of a business rule. Useful for temporarily disabling rules without deleting them.

### Delete Business Rule
```
DELETE /api/business-rules/{id}
```
**Response:** 204 No Content

**Permissions:** `PAGE_BUSINESS_RULES_EDIT` or `ROLE_SYSTEM_ADMIN`

## Using the Business Rule Engine

### In Service Classes

To use the Business Rule Engine in your services, inject `BusinessRuleEngine` and call `validate()`:

```java
@Service
public class TaskService {
    
    private final BusinessRuleEngine businessRuleEngine;
    
    public TaskService(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }
    
    @Transactional
    public TaskDTO updateTask(Long taskId, TaskUpdateDTO dto) {
        // Build context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("TASK_UPDATE")
            .entityId(taskId)
            .updateDate(dto.getUpdateDate())
            .plannedQty(dto.getPlannedQty())
            .dailyUpdateQty(dto.getDailyUpdateQty())
            .build();
        
        // Validate rules
        businessRuleEngine.validate(101, context); // Backdate rule
        businessRuleEngine.validate(401, context); // Planned vs actual rule
        
        // Or validate multiple rules at once
        businessRuleEngine.validateAll(Arrays.asList(101, 102, 401), context);
        
        // Proceed with update if validation passes
        // ...
    }
}
```

### BusinessRuleContext Fields

The `BusinessRuleContext` supports the following fields:

- **Tenant/User**: `tenantId`, `userId`
- **Entity**: `entityType`, `entityId`
- **Quantities**: `plannedQty`, `actualQty`, `updateQty`, `dailyUpdateQty`
- **Dates**: `updateDate`, `confirmationDate`, `lockDate`, `taskStartDate`, `taskEndDate`, `wbsStartDate`, `wbsEndDate`, `allocationStartDate`, `allocationEndDate`, `attendanceDate`, `materialUsageDate`, `planVersionDate`
- **Status**: `taskStatus`, `wbsStatus`, `confirmationStatus`, `isLocked`, `isConfirmed`
- **Additional**: `additionalParams` (Map for custom parameters)

### Rule Validation Flow

1. Service builds `BusinessRuleContext` with relevant data
2. Service calls `businessRuleEngine.validate(ruleNumber, context)`
3. Engine checks if rule exists and is active/applicable
4. Engine routes to appropriate validator
5. Validator throws `BusinessRuleException` if rule is violated
6. Exception is caught and returned to client with rule number and message

## Creating New Rules

### Step 1: Create Validator

```java
@Component
public class MyCustomRuleValidator implements BusinessRuleValidator {
    
    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) 
            throws BusinessRuleException {
        // Your validation logic here
        if (/* violation condition */) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "Error message",
                "User-friendly hint"
            );
        }
    }
    
    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{601}; // Your rule number
    }
}
```

### Step 2: Add Rule to Database

Create the rule via API or directly in database:

```sql
INSERT INTO business_rules (tenant_id, rule_number, control_point, applicability, rule_value, description, activate_flag, created_by, created_on)
VALUES (1, 601, 'CUSTOM_CONTROL_POINT', 'Y', 'value', 'Description', 1, 1, GETDATE());
```

### Step 3: Use in Services

```java
businessRuleEngine.validate(601, context);
```

## Rule Configuration

### Applicability
- **Y**: Rule is applicable and will be validated
- **N**: Rule is not applicable and will be skipped

### Activate Flag
- **true**: Rule is active
- **false**: Rule is inactive (temporarily disabled)

### Rule Value
- Rule-specific value (e.g., number of days, Y/N flag, etc.)
- Format depends on the rule
- Can be empty if not needed

## Integration Points

Rules should be validated in:

- **TaskService**: Task creation, updates, date changes
- **WbsService**: WBS creation, updates, date changes
- **PlanService**: Plan version creation, updates
- **TaskUpdateService**: Daily updates, backdating
- **ConfirmationService**: Confirmation operations
- **ResourceAllocationService**: Allocation creation, updates
- **AttendanceService**: Attendance entry creation
- **MaterialService**: Material usage entry creation

## Error Handling

When a rule is violated, `BusinessRuleException` is thrown with:
- `ruleNumber`: The rule number that was violated
- `message`: Error message
- `hint`: User-friendly hint

The `GlobalExceptionHandler` converts this to a JSON response:

```json
{
  "message": "Backdating is only allowed for 7 days. Attempted to backdate by 10 days.",
  "ruleNumber": 101,
  "hint": "You can only backdate up to 7 days from today.",
  "type": "BUSINESS_RULE_VIOLATION"
}
```

## Frontend Components

### BusinessRules Page
- List all business rules
- Filter by control point
- Active only toggle
- Create/Edit/Delete operations
- Activate/Deactivate toggle button

**Location:** `elina/frontend/src/pages/admin/BusinessRules.jsx`

### BusinessRuleEdit Modal
- Create/Edit form
- Rule number, control point, applicability
- Rule value and description
- Active flag toggle

**Location:** `elina/frontend/src/components/BusinessRuleEdit.jsx`

### RuleViolationModal Component
- Automatically triggered when a BusinessRuleException is caught
- Displays rule number, message, and hint
- Global handler in App.jsx - works across all pages
- Auto-closes after 10 seconds

**Location:** `elina/frontend/src/components/RuleViolationModal.jsx`

**Usage:** The modal is automatically triggered by the axios interceptor when a `BUSINESS_RULE_VIOLATION` error is received. No manual integration needed.

### RuleSummaryChip Component
- Small UI component showing rule summary
- Displays rule number and active status
- Optional description display
- Can be used on pages that depend on specific rules

**Location:** `elina/frontend/src/components/RuleSummaryChip.jsx`

**Example Usage:**
```jsx
import RuleSummaryChip from '../components/RuleSummaryChip'

// In your component
<RuleSummaryChip ruleNumber={101} showDescription={false} />
```

### Axios Interceptor
- Automatically catches `BUSINESS_RULE_VIOLATION` errors
- Dispatches `businessRuleViolation` custom event
- Global RuleViolationModal listens and displays the error

**Location:** `elina/frontend/src/services/api.js`

**How it works:**
1. Backend throws `BusinessRuleException`
2. `GlobalExceptionHandler` converts to JSON with `type: "BUSINESS_RULE_VIOLATION"`
3. Axios interceptor catches 400 with this type
4. Dispatches `businessRuleViolation` event
5. App.jsx listener shows `RuleViolationModal`

## Testing

### Unit Tests

```bash
cd elina/backend
mvn test -Dtest=BusinessRuleEngineTest
mvn test -Dtest=BackdateRuleValidatorTest
```

## Security Considerations

1. **Tenant Isolation**: All rules are tenant-scoped
2. **Permission Checks**: 
   - Read: `PAGE_BUSINESS_RULES_VIEW` or `PAGE_BUSINESS_RULES_EDIT`
   - Write: `PAGE_BUSINESS_RULES_EDIT` or `ROLE_SYSTEM_ADMIN`
3. **Audit Fields**: `created_by` and `updated_by` are automatically set from JWT user_id
4. **Rule Number Uniqueness**: Enforced per tenant

## Migration Files

- `014-create-business-rules-table.xml`: Creates the business_rules table
- `015-seed-business-rules.xml`: Seeds default business rules
- `016-add-business-rules-permissions.xml`: Creates and assigns permissions

These are Liquibase migrations located in `elina/backend/src/main/resources/db/changelog/changes/`

## Service Integration Examples

Example service implementations showing how to integrate business rule validation are available in:
- `com.elina.authorization.service.example.TaskUpdateServiceExample` - Day-wise updates
- `com.elina.authorization.service.example.WbsConfirmationServiceExample` - WBS confirmation
- `com.elina.authorization.service.example.WbsServiceExample` - WBS creation/update
- `com.elina.authorization.service.example.AllocationServiceExample` - Resource allocation
- `com.elina.authorization.service.example.AttendanceServiceExample` - Attendance posting

## Example: Integrating Rule Validation

Here's a complete example of integrating rule validation in a service:

```java
@Service
public class TaskUpdateService {
    
    private final BusinessRuleEngine businessRuleEngine;
    private final TaskRepository taskRepository;
    
    @Transactional
    public TaskUpdateDTO createTaskUpdate(TaskUpdateCreateDTO dto) {
        Task task = taskRepository.findById(dto.getTaskId())
            .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Build validation context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("TASK_UPDATE")
            .entityId(dto.getTaskId())
            .updateDate(dto.getUpdateDate())
            .plannedQty(task.getPlannedQty())
            .dailyUpdateQty(dto.getQuantity())
            .actualQty(task.getActualQty())
            .lockDate(task.getLockDate())
            .isConfirmed(task.getIsConfirmed())
            .build();
        
        // Validate all applicable rules
        businessRuleEngine.validateAll(
            Arrays.asList(101, 102, 301, 401), 
            context
        );
        
        // If we get here, all rules passed - proceed with update
        TaskUpdate update = new TaskUpdate();
        // ... set fields ...
        return toDTO(taskUpdateRepository.save(update));
    }
}
```

## Troubleshooting

### Rule Not Being Validated

1. Check rule is active: `activate_flag = true`
2. Check applicability: `applicability = 'Y'`
3. Verify validator is registered (check logs on startup)
4. Check rule number matches validator's `getSupportedRuleNumbers()`

### Rule Validation Failing Unexpectedly

1. Check rule value format (e.g., "7" not "7.0" for days)
2. Verify context has all required fields
3. Review validator logic for edge cases
4. Check logs for validation details

### Cache Issues

Call `businessRuleEngine.refreshCache()` after creating/updating/deleting rules.

## Integration Examples

See `INTEGRATION_EXAMPLES.md` for detailed examples of:
- Day-wise task update validation
- WBS confirmation validation
- Manpower allocation validation
- Attendance posting validation
- WBS creation with date validation

## Frontend Integration

### Automatic Rule Violation Handling

The frontend automatically handles business rule violations:

1. **Axios Interceptor** (`api.js`):
   - Catches `BUSINESS_RULE_VIOLATION` errors
   - Dispatches `businessRuleViolation` event

2. **Global Modal** (`App.jsx`):
   - Listens for `businessRuleViolation` events
   - Displays `RuleViolationModal` automatically

3. **No Manual Integration Needed**:
   - Just make API calls normally
   - If a rule is violated, the modal appears automatically

### Using RuleSummaryChip

Display rule status on pages:

```jsx
import RuleSummaryChip from '../components/RuleSummaryChip'

function TaskUpdatePage() {
  return (
    <div>
      <h1>Create Task Update</h1>
      <RuleSummaryChip ruleNumber={101} />
      <RuleSummaryChip ruleNumber={401} showDescription={true} />
      {/* Your form here */}
    </div>
  )
}
```

## Frontend Components

### RuleViolationModal
Automatically displays when a business rule is violated. Listens to the `businessRuleViolation` custom event dispatched by the axios interceptor.

**Location:** `elina/frontend/src/components/RuleViolationModal.jsx`

**Usage:** Already integrated in `App.jsx` - works globally across all pages.

### RuleSummaryChip
Small UI component showing rule summary for pages that depend on specific rules.

**Location:** `elina/frontend/src/components/RuleSummaryChip.jsx`

**Usage:**
```jsx
import RuleSummaryChip from '../components/RuleSummaryChip'

// In your component
<RuleSummaryChip ruleNumber={101} />
```

### Business Rules List Page
Full-featured page for managing business rules with:
- Search by rule number, description, or control point
- Filter by control point
- Active only toggle
- Activate/Deactivate toggle
- Edit and Delete actions

**Location:** `elina/frontend/src/pages/admin/BusinessRules.jsx`

## Sample Use Cases Implemented

1. **Day-wise update (update_date older than allowed)**
   - Example: `TaskUpdateServiceExample.createDayWiseUpdate()`
   - Validates: Rule 101 (BACKDATE_ALLOWED_TILL), Rule 401 (DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY), Rule 201 (START_DATE_CANNOT_BE_IN_FUTURE)

2. **Confirmation when lock exists**
   - Example: `WbsConfirmationServiceExample.confirmWbs()`
   - Validates: Rule 301 (CONFIRMATION_CANNOT_BE_OVERWRITTEN)

3. **Task update actual_qty > plan_qty**
   - Example: `TaskUpdateServiceExample.createDayWiseUpdate()`
   - Validates: Rule 401 (DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY)

4. **WBS end_date before start_date**
   - Example: `WbsServiceExample.createOrUpdateWbs()`
   - Validates: Rule 202 (WBS_DATE_RANGE_VALIDATION)

5. **Manpower allocation end_date < start_date**
   - Example: `AllocationServiceExample.createAllocation()`
   - Validates: Rule 501 (ALLOCATION_START_END_DATE_MUST_BE_VALID)

6. **Attendance date in future**
   - Example: `AttendanceServiceExample.postAttendance()`
   - Validates: Rule 601 (ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE)

## Future Enhancements

- [ ] Rule dependency management (rule A depends on rule B)
- [ ] Rule execution order configuration
- [ ] Rule performance metrics
- [ ] Rule violation logging (optional)
- [ ] Rule templates for common patterns
- [ ] Visual rule builder UI

