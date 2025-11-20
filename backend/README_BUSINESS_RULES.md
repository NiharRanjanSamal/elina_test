# Business Rules Engine Module

## Overview

The Business Rules Engine is a **centralized validation system** that actively prevents invalid operations across the entire application. Business rules are **NOT passive** - they are **ACTIVE VALIDATORS** that block actions that violate rules.

### Key Concepts

- **Active Validation**: Rules prevent invalid operations, not just log violations
- **Tenant-Scoped**: Each tenant has their own set of business rules
- **Centralized Engine**: All validation logic is centralized in `BusinessRuleEngine`
- **Automatic Enforcement**: Rules are automatically enforced in service layer operations
- **Configurable**: Rules can be enabled/disabled per tenant via `applicability` and `activate_flag`

## Architecture

### Backend Components

#### 1. **BusinessRuleEngine** (`com.elina.authorization.rule.BusinessRuleEngine`)
   - Core engine that validates business rules
   - Caches rules for performance
   - Routes validation to appropriate validators
   - Methods:
     - `boolean isRuleActive(int ruleNumber)` - Check if rule is active
     - `String getRuleValue(int ruleNumber)` - Get rule value
     - `void validate(int ruleNumber, BusinessRuleContext ctx)` - Validate a rule
     - `void validateAll(List<Integer> ruleNumbers, BusinessRuleContext ctx)` - Validate multiple rules

#### 2. **BusinessRuleContext** (`com.elina.authorization.rule.BusinessRuleContext`)
   - Context object containing all data needed for validation
   - Fields include:
     - `tenantId`, `userId`
     - `entityType`, `entityId`
     - `plannedQty`, `actualQty`, `updateQty`
     - `updateDate`, `confirmationDate`, `lockDate`
     - `taskStartDate`, `taskEndDate`
     - `wbsStartDate`, `wbsEndDate`
     - `allocationStartDate`, `allocationEndDate`
     - `attendanceDate`, `materialUsageDate`
     - `planVersionDate`
     - Additional dynamic parameters via `additionalParams` map

#### 3. **BusinessRuleException** (`com.elina.authorization.rule.BusinessRuleException`)
   - Custom exception thrown when a rule is violated
   - Contains:
     - `ruleNumber` - The rule number that was violated
     - `message` - Error message
     - `hint` - User-friendly hint for resolving the issue

#### 4. **BusinessRuleValidator Interface**
   - Interface for individual rule validators
   - Each validator implements:
     - `void validate(BusinessRule rule, BusinessRuleContext context)`
     - `int[] getSupportedRuleNumbers()` - Returns array of rule numbers this validator handles

#### 5. **Individual Validators**

   - **BackdateRuleValidator** (Rules 101, 102)
     - Rule 101: `BACKDATE_ALLOWED_TILL` - Maximum days allowed for backdating
     - Rule 102: `BACKDATE_ALLOWED_AFTER_LOCK` - Whether backdating is allowed after lock date

   - **TaskDateRangeRuleValidator** (Rule 201)
     - Rule 201: `START_DATE_CANNOT_BE_IN_FUTURE` - Task start date cannot be in the future

   - **WbsDateRangeRuleValidator** (Rule 202)
     - Rule 202: `END_DATE_CANNOT_BE_BEFORE_START_DATE` - WBS end date cannot be before start date

   - **ConfirmationLockRuleValidator** (Rule 301)
     - Rule 301: `CONFIRMATION_CANNOT_BE_OVERWRITTEN` - Confirmed entries cannot be modified

   - **PlannedVsActualRuleValidator** (Rule 401)
     - Rule 401: `DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY` - Daily update quantity cannot exceed planned quantity

   - **AllocationDateRuleValidator** (Rules 203, 501)
     - Rule 203: `ALLOCATION_DATE_RANGE_VALIDATION` - General allocation date validation
     - Rule 501: `ALLOCATION_START_END_DATE_MUST_BE_VALID` - Allocation end date must be on or after start date

   - **AttendanceEntryRuleValidator** (Rules 204, 601)
     - Rule 204: `ATTENDANCE_DATE_VALIDATION` - General attendance validation
     - Rule 601: `ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE` - Attendance date cannot be in the future

   - **MaterialUsageRuleValidator** (Rule 205)
     - Rule 205: `MATERIAL_USAGE_VALIDATION` - Material usage date and quantity validation

### Database Schema

#### business_rules Table

```sql
CREATE TABLE business_rules (
    rule_id BIGINT PRIMARY KEY IDENTITY,
    tenant_id BIGINT NOT NULL,
    rule_number INT NOT NULL,
    control_point VARCHAR(100) NOT NULL,
    applicability CHAR(1) NOT NULL,  -- Y or N
    rule_value VARCHAR(500),
    description VARCHAR(1000),
    activate_flag BIT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_on DATETIME NOT NULL,
    updated_by BIGINT,
    updated_on DATETIME,
    CONSTRAINT uq_plan_versions_task_version UNIQUE (tenant_id, rule_number)
);
```

**Fields:**
- `rule_number`: Unique rule identifier (101, 102, 201, etc.)
- `control_point`: Where the rule applies (TASK_UPDATE, WBS, TASK, CONFIRMATION, etc.)
- `applicability`: Y = Rule is applicable, N = Rule is disabled
- `rule_value`: Rule-specific value (e.g., number of days, Y/N flag)
- `activate_flag`: true = Rule is active, false = Rule is temporarily disabled

## Default Business Rules

The following rules are seeded for each tenant:

| Rule # | Control Point | Description | Rule Value |
|--------|--------------|-------------|------------|
| 101 | TASK_UPDATE | Maximum days allowed for backdating task updates | 7 (days) |
| 102 | TASK_UPDATE | Whether backdating is allowed after lock date | N (not allowed) |
| 201 | TASK | Task start date cannot be in the future | (empty) |
| 202 | WBS | WBS end date cannot be before start date | (empty) |
| 203 | ALLOCATION | Resource allocation end date cannot be before start date | (empty) |
| 204 | ATTENDANCE | Attendance date cannot be in the future | (empty) |
| 205 | MATERIAL_USAGE | Material usage date and quantity validation | (empty) |
| 301 | CONFIRMATION | Confirmed entries cannot be modified or overwritten | (empty) |
| 401 | TASK_UPDATE | Daily update quantity cannot exceed planned quantity | (empty) |
| 402 | PLAN_VERSION | Plan version date must be valid and cannot be in the future | (empty) |
| 501 | ALLOCATION | Manpower allocation end date must be on or after start date | (empty) |
| 601 | ATTENDANCE | Attendance date cannot be in the future | (empty) |

## Rule Enforcement

### Integration Points

Business rules are automatically enforced in the following services:

#### TaskService
- **Rule 201**: Validates task start date when creating/updating tasks
- **Rule 301**: Validates confirmation status when confirming tasks

#### WbsService
- **Rule 202**: Validates WBS date range when creating/updating WBS
- **Rule 301**: Validates confirmation status when confirming WBS

#### PlanService
- **Rule 101**: Validates backdating when creating plan versions
- **Rule 201**: Validates plan version date when creating plan versions
- **Rule 402**: Validates plan version date validity

#### TaskUpdateService
- **Rule 101**: Validates backdating when creating task updates
- **Rule 102**: Validates backdating after lock date
- **Rule 401**: Validates daily update quantity against planned quantity

#### ConfirmationService
- **Rule 301**: Validates that confirmed entries cannot be overwritten

### Usage Example

```java
@Service
public class TaskUpdateService {
    
    private final BusinessRuleEngine businessRuleEngine;
    
    public TaskUpdateDTO createTaskUpdate(TaskUpdateCreateDTO dto) {
        // Build context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("TASK_UPDATE")
            .updateDate(dto.getUpdateDate())
            .updateQty(dto.getDailyUpdateQty())
            .plannedQty(task.getPlannedQty())
            .lockDate(task.getLockDate())
            .build();
        
        // Validate rules
        businessRuleEngine.validateAll(
            Arrays.asList(101, 102, 401), 
            context
        );
        
        // Proceed with creation...
    }
}
```

## API Endpoints

### Business Rules Management

- `GET /api/business-rules` - List all business rules for tenant
- `GET /api/business-rules/{id}` - Get business rule by ID
- `GET /api/business-rules/by-number/{ruleNumber}` - Get business rule by rule number
- `GET /api/business-rules/control-points` - Get all control points
- `POST /api/business-rules` - Create new business rule
- `PUT /api/business-rules/{id}` - Update business rule
- `PUT /api/business-rules/{id}/activate-toggle` - Toggle activate flag
- `DELETE /api/business-rules/{id}` - Delete business rule
- `POST /api/business-rules/validate-single` - Validate a single rule (for frontend pre-validation)

### Response Format

**BusinessRuleException Response (HTTP 400):**
```json
{
  "type": "BUSINESS_RULE_VIOLATION",
  "ruleNumber": 101,
  "message": "Backdating is only allowed for 7 days. Attempted to backdate by 10 days.",
  "hint": "You can only backdate up to 7 days from today."
}
```

## Frontend Integration

### Components

1. **BusinessRules.jsx** - List page for managing business rules
2. **BusinessRuleEdit.jsx** - Modal for creating/editing business rules
3. **RuleViolationModal.jsx** - Modal that automatically displays when a rule is violated
4. **RuleSummaryChip.jsx** - Small chip component showing rule status

### Axios Interceptor

The frontend automatically handles `BusinessRuleException` responses via an axios interceptor:

```javascript
// Automatically triggers RuleViolationModal
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 400 && 
        error.response?.data?.type === 'BUSINESS_RULE_VIOLATION') {
      // Dispatch custom event for RuleViolationModal
      window.dispatchEvent(new CustomEvent('businessRuleViolation', {
        detail: error.response.data
      }));
    }
    return Promise.reject(error);
  }
);
```

## Adding New Rules

### Step 1: Create Validator

```java
@Component
public class MyNewRuleValidator implements BusinessRuleValidator {
    
    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) 
            throws BusinessRuleException {
        // Validation logic here
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
        return new int[]{701}; // Your rule number
    }
}
```

### Step 2: Add Seed Data

Update `015-seed-business-rules.xml` or create a new migration:

```xml
<insert tableName="business_rules">
    <column name="tenant_id" valueNumeric="1"/>
    <column name="rule_number" valueNumeric="701"/>
    <column name="control_point" value="YOUR_CONTROL_POINT"/>
    <column name="applicability" value="Y"/>
    <column name="rule_value" value=""/>
    <column name="description" value="Your rule description"/>
    <column name="activate_flag" valueBoolean="true"/>
    <column name="created_by" valueNumeric="1"/>
    <column name="created_on" valueComputed="CURRENT_TIMESTAMP"/>
</insert>
```

### Step 3: Integrate in Service

```java
// In your service method
BusinessRuleContext context = BusinessRuleContext.builder()
    .tenantId(TenantContext.getTenantId())
    // ... set relevant fields
    .build();

businessRuleEngine.validate(701, context);
```

## Use Cases

### Use Case 1: Day-wise Update (Backdating)

**Scenario**: User tries to create a task update with a date 10 days in the past.

**Rule Applied**: Rule 101 (BACKDATE_ALLOWED_TILL = 7 days)

**Result**: 
- `BusinessRuleException` thrown with rule number 101
- Frontend displays `RuleViolationModal` with message and hint
- Operation is blocked

### Use Case 2: Confirmation When Lock Exists

**Scenario**: User tries to confirm a WBS that is already locked.

**Rule Applied**: Rule 301 (CONFIRMATION_CANNOT_BE_OVERWRITTEN)

**Result**:
- `BusinessRuleException` thrown with rule number 301
- Frontend displays error modal
- Operation is blocked

### Use Case 3: Task Update Exceeds Planned Quantity

**Scenario**: User tries to update task with actual_qty = 150, but planned_qty = 100.

**Rule Applied**: Rule 401 (DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY)

**Result**:
- `BusinessRuleException` thrown with rule number 401
- Frontend displays error modal
- Operation is blocked

### Use Case 4: WBS End Date Before Start Date

**Scenario**: User tries to create WBS with start_date = 2025-01-31, end_date = 2025-01-01.

**Rule Applied**: Rule 202 (END_DATE_CANNOT_BE_BEFORE_START_DATE)

**Result**:
- `BusinessRuleException` thrown with rule number 202
- Frontend displays error modal
- Operation is blocked

### Use Case 5: Allocation End Date Before Start Date

**Scenario**: User tries to create allocation with start_date = 2025-02-01, end_date = 2025-01-15.

**Rule Applied**: Rule 501 (ALLOCATION_START_END_DATE_MUST_BE_VALID)

**Result**:
- `BusinessRuleException` thrown with rule number 501
- Frontend displays error modal
- Operation is blocked

## Testing

### Unit Tests

Validators are unit tested in `BusinessRuleEngineTest.java`:

```java
@Test
void testBackdateRule_ExceedsAllowedDays() {
    BusinessRuleContext context = BusinessRuleContext.builder()
        .updateDate(LocalDate.now().minusDays(10))
        .build();
    
    assertThrows(BusinessRuleException.class, 
        () -> businessRuleEngine.validate(101, context));
}
```

### Integration Tests

Service integration tests verify rule enforcement:

```java
@Test
void testCreateTaskUpdate_BackdateExceedsLimit() {
    TaskUpdateCreateDTO dto = new TaskUpdateCreateDTO();
    dto.setUpdateDate(LocalDate.now().minusDays(10));
    
    assertThrows(BusinessRuleException.class, 
        () -> taskUpdateService.createTaskUpdate(dto));
}
```

## Performance Considerations

- **Rule Caching**: Rules are cached per tenant to reduce database queries
- **Lazy Validation**: Rules are only validated if they are active and applicable
- **Batch Validation**: Use `validateAll()` to validate multiple rules in one call

## Troubleshooting

### Rule Not Being Validated

1. Check if rule exists: `GET /api/business-rules/by-number/{ruleNumber}`
2. Verify `applicability = 'Y'` and `activate_flag = true`
3. Check if validator is registered (check logs for "Registered validator")
4. Verify `BusinessRuleContext` has required fields

### Rule Validation Failing Unexpectedly

1. Check rule value format (e.g., Rule 101 expects numeric value)
2. Verify context fields are set correctly
3. Check validator logic matches rule description

### Frontend Not Showing Violation Modal

1. Verify axios interceptor is checking for `type === 'BUSINESS_RULE_VIOLATION'`
2. Check browser console for errors
3. Verify `RuleViolationModal` is included in `App.jsx`

## Best Practices

1. **Always validate before persisting**: Call `businessRuleEngine.validate()` before saving to database
2. **Build complete context**: Include all relevant fields in `BusinessRuleContext`
3. **Use descriptive messages**: Provide clear error messages and hints
4. **Test edge cases**: Test with null values, boundary conditions, etc.
5. **Document rule purpose**: Always include clear descriptions in seed data
6. **Version control**: Track rule changes in migration files

## Migration Guide

### Updating Existing Rules

1. Create new Liquibase migration
2. Update rule in database
3. Call `businessRuleEngine.refreshCache()` to clear cache
4. Update validator if rule logic changes

### Deprecating Rules

1. Set `applicability = 'N'` or `activate_flag = false`
2. Remove validator registration (optional)
3. Document deprecation in migration comments

## Support

For questions or issues:
1. Check this documentation
2. Review validator implementations
3. Check service integration examples
4. Review test cases for usage patterns
