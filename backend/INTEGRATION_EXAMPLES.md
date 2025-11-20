# Business Rule Engine - Integration Examples

This document provides practical examples of how to integrate the Business Rule Engine into your service classes.

## Overview

The Business Rule Engine is an **ACTIVE VALIDATOR** that prevents invalid operations. Every service that performs business operations should validate relevant rules before proceeding.

## Integration Pattern

The standard pattern for integrating business rule validation:

```java
@Service
public class YourService {
    
    private final BusinessRuleEngine businessRuleEngine;
    
    @Transactional
    public YourDTO performOperation(YourCreateDTO dto) {
        // 1. Build BusinessRuleContext with all relevant data
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("YOUR_ENTITY_TYPE")
            // ... add all relevant fields
            .build();
        
        // 2. Validate applicable rules
        businessRuleEngine.validate(101, context); // Rule 101
        businessRuleEngine.validate(401, context); // Rule 401
        // Or validate multiple at once:
        businessRuleEngine.validateAll(Arrays.asList(101, 401, 301), context);
        
        // 3. If validation passes, proceed with operation
        // ... your business logic here ...
    }
}
```

## Example 1: Day-wise Task Update

**Use Case:** Creating a day-wise update for a task.

**Rules to Validate:**
- Rule 101: BACKDATE_ALLOWED_TILL (max 7 days)
- Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY
- Rule 201: START_DATE_CANNOT_BE_IN_FUTURE

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
            .taskStartDate(task.getStartDate())
            .lockDate(task.getLockDate())
            .isConfirmed(task.getIsConfirmed())
            .build();
        
        // Validate all applicable rules
        businessRuleEngine.validateAll(
            Arrays.asList(101, 102, 201, 301, 401), 
            context
        );
        
        // If we get here, all rules passed - proceed with update
        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setUpdateDate(dto.getUpdateDate());
        update.setQuantity(dto.getQuantity());
        // ... set other fields ...
        
        return toDTO(taskUpdateRepository.save(update));
    }
}
```

## Example 2: Confirming WBS

**Use Case:** Confirming a Work Breakdown Structure.

**Rules to Validate:**
- Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN

```java
@Service
public class WbsService {
    
    private final BusinessRuleEngine businessRuleEngine;
    private final WbsRepository wbsRepository;
    
    @Transactional
    public WbsDTO confirmWbs(Long wbsId) {
        Wbs wbs = wbsRepository.findById(wbsId)
            .orElseThrow(() -> new RuntimeException("WBS not found"));
        
        // Build validation context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("CONFIRMATION")
            .entityId(wbsId)
            .isConfirmed(wbs.getIsConfirmed())
            .isLocked(wbs.getIsLocked())
            .confirmationDate(LocalDate.now())
            .build();
        
        // Validate confirmation rule
        businessRuleEngine.validate(301, context);
        
        // If validation passes, confirm the WBS
        wbs.setIsConfirmed(true);
        wbs.setConfirmationDate(LocalDate.now());
        wbs.setConfirmedBy(getCurrentUserId());
        
        return toDTO(wbsRepository.save(wbs));
    }
}
```

## Example 3: Adding Manpower Allocation

**Use Case:** Creating a manpower allocation.

**Rules to Validate:**
- Rule 501: ALLOCATION_START_END_DATE_MUST_BE_VALID

```java
@Service
public class ResourceAllocationService {
    
    private final BusinessRuleEngine businessRuleEngine;
    private final AllocationRepository allocationRepository;
    
    @Transactional
    public AllocationDTO createAllocation(AllocationCreateDTO dto) {
        // Build validation context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("ALLOCATION")
            .allocationStartDate(dto.getStartDate())
            .allocationEndDate(dto.getEndDate())
            .build();
        
        // Validate allocation date rules
        businessRuleEngine.validateAll(
            Arrays.asList(203, 501), 
            context
        );
        
        // If validation passes, create allocation
        Allocation allocation = new Allocation();
        allocation.setStartDate(dto.getStartDate());
        allocation.setEndDate(dto.getEndDate());
        // ... set other fields ...
        
        return toDTO(allocationRepository.save(allocation));
    }
}
```

## Example 4: Posting Attendance

**Use Case:** Posting attendance entry.

**Rules to Validate:**
- Rule 601: ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE

```java
@Service
public class AttendanceService {
    
    private final BusinessRuleEngine businessRuleEngine;
    private final AttendanceRepository attendanceRepository;
    
    @Transactional
    public AttendanceDTO postAttendance(AttendanceCreateDTO dto) {
        // Build validation context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("ATTENDANCE")
            .attendanceDate(dto.getAttendanceDate())
            .build();
        
        // Validate attendance rules
        businessRuleEngine.validateAll(
            Arrays.asList(204, 601), 
            context
        );
        
        // If validation passes, create attendance entry
        Attendance attendance = new Attendance();
        attendance.setAttendanceDate(dto.getAttendanceDate());
        // ... set other fields ...
        
        return toDTO(attendanceRepository.save(attendance));
    }
}
```

## Example 5: Creating WBS with Date Validation

**Use Case:** Creating a WBS with start and end dates.

**Rules to Validate:**
- Rule 202: WBS_DATE_RANGE_VALIDATION

```java
@Service
public class WbsService {
    
    private final BusinessRuleEngine businessRuleEngine;
    
    @Transactional
    public WbsDTO createWbs(WbsCreateDTO dto) {
        // Build validation context
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("WBS")
            .wbsStartDate(dto.getStartDate())
            .wbsEndDate(dto.getEndDate())
            .build();
        
        // Validate WBS date rules
        businessRuleEngine.validate(202, context);
        
        // If validation passes, create WBS
        Wbs wbs = new Wbs();
        wbs.setStartDate(dto.getStartDate());
        wbs.setEndDate(dto.getEndDate());
        // ... set other fields ...
        
        return toDTO(wbsRepository.save(wbs));
    }
}
```

## Rule Enforcement Checklist

When implementing a new service method, ask:

1. **What entity type is this?** (TASK_UPDATE, WBS, TASK, CONFIRMATION, ALLOCATION, ATTENDANCE, MATERIAL_USAGE, PLAN_VERSION)
2. **What rules apply to this operation?** (Check rule control points)
3. **What data do I need for validation?** (Dates, quantities, status flags, etc.)
4. **Where should I validate?** (Before any database writes)

## Common Rules by Operation

| Operation | Rules to Validate |
|-----------|------------------|
| Day-wise update | 101, 102, 201, 401 |
| WBS creation/edit | 202 |
| WBS confirmation | 301 |
| Task creation/edit | 201, 501 (if task has dates) |
| Allocation creation | 203, 501 |
| Attendance posting | 204, 601 |
| Material usage | 205 |
| Plan version | 402 |

## Error Handling

When a rule is violated, `BusinessRuleException` is thrown. The `GlobalExceptionHandler` converts it to:

```json
{
  "message": "Backdating is only allowed for 7 days. Attempted to backdate by 10 days.",
  "ruleNumber": 101,
  "hint": "You can only backdate up to 7 days from today.",
  "type": "BUSINESS_RULE_VIOLATION"
}
```

The frontend axios interceptor automatically catches this and displays the `RuleViolationModal`.

## Best Practices

1. **Validate Early**: Validate rules before any database operations
2. **Build Complete Context**: Include all relevant data in `BusinessRuleContext`
3. **Validate Multiple Rules**: Use `validateAll()` for multiple rules
4. **Handle Exceptions**: Let `BusinessRuleException` propagate - it's handled globally
5. **Cache Refresh**: Rules are cached - call `businessRuleEngine.refreshCache()` after rule changes
6. **Log Violations**: Rule violations are automatically logged by the engine

## Testing Rule Integration

When testing services that use business rules:

```java
@Test
void testCreateTaskUpdate_BackdateExceedsLimit() {
    // Arrange
    TaskUpdateCreateDTO dto = new TaskUpdateCreateDTO();
    dto.setUpdateDate(LocalDate.now().minusDays(10)); // Exceeds 7 day limit
    
    // Act & Assert
    BusinessRuleException exception = assertThrows(BusinessRuleException.class,
        () -> taskUpdateService.createTaskUpdate(dto));
    
    assertEquals(101, exception.getRuleNumber());
    assertTrue(exception.getMessage().contains("Backdating is only allowed for 7 days"));
}
```

## Adding New Rules

1. Create validator implementing `BusinessRuleValidator`
2. Register rule numbers in `getSupportedRuleNumbers()`
3. Add rule to database via API or migration
4. Update services to validate the new rule
5. Update this documentation

---

**Remember:** Business rules are ACTIVE - they prevent invalid operations, not just log them!

