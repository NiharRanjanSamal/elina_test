-- Flyway Migration: Seed sample task updates data for tenant TNT_DEMO
-- Task: "Excavation Zone A – Trench Digging"
-- Plan version: 10 days plan, qty 100
-- Date Range: 2025-11-05 to 2025-11-14

-- First, find the tenant ID for TNT_DEMO
DECLARE @TenantId BIGINT;
SELECT @TenantId = id FROM dbo.tenants WHERE tenant_code = 'TNT_DEMO';

-- Find the task "Excavation Zone A – Trench Digging"
DECLARE @TaskId BIGINT;
SELECT @TaskId = task_id FROM dbo.tasks 
WHERE tenant_id = @TenantId 
  AND task_name LIKE '%Excavation Zone A%Trench Digging%';

-- Only proceed if tenant and task exist
IF @TenantId IS NOT NULL AND @TaskId IS NOT NULL
BEGIN
    -- Get task details for planned_qty
    DECLARE @PlannedQty DECIMAL(18,2);
    DECLARE @Unit VARCHAR(20);
    SELECT @PlannedQty = planned_qty, @Unit = unit 
    FROM dbo.tasks 
    WHERE task_id = @TaskId;
    
    -- Default planned qty if null
    IF @PlannedQty IS NULL
        SET @PlannedQty = 10.00; -- 10 units per day for 10 days = 100 total
    
    -- Get a user ID for created_by/updated_by
    DECLARE @UserId BIGINT;
    SELECT TOP 1 @UserId = id FROM dbo.users WHERE tenant_id = @TenantId AND is_active = 1;
    
    -- Day 1-3: actual = plan
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-05')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-05', @PlannedQty, @PlannedQty, @PlannedQty, 'Day 1: On track', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-06')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-06', @PlannedQty, @PlannedQty, @PlannedQty, 'Day 2: On track', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-07')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-07', @PlannedQty, @PlannedQty, @PlannedQty, 'Day 3: On track', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    -- Day 4-5: actual < plan
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-08')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-08', @PlannedQty, @PlannedQty * 0.8, @PlannedQty * 0.8, 'Day 4: Slight delay', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-09')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-09', @PlannedQty, @PlannedQty * 0.75, @PlannedQty * 0.75, 'Day 5: Behind schedule', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    -- Day 6: actual > plan (should be blocked by Rule 401, but seed it for testing)
    -- Note: This will be blocked by business rule validation, but we seed it to test the rule
    -- In practice, this should not be inserted if Rule 401 is active
    -- IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-10')
    -- BEGIN
    --     INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
    --     VALUES (@TenantId, @TaskId, '2025-11-10', @PlannedQty, @PlannedQty * 1.2, @PlannedQty * 1.2, 'Day 6: Exceeds plan (should be blocked)', 1, @UserId, GETDATE(), @UserId, GETDATE());
    -- END
    
    -- Day 7-9: actual random values <= plan
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-11')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-11', @PlannedQty, @PlannedQty * 0.9, @PlannedQty * 0.9, 'Day 7: Catching up', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-12')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-12', @PlannedQty, @PlannedQty * 0.95, @PlannedQty * 0.95, 'Day 8: Progressing', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    IF NOT EXISTS (SELECT 1 FROM dbo.task_updates WHERE tenant_id = @TenantId AND task_id = @TaskId AND update_date = '2025-11-13')
    BEGIN
        INSERT INTO dbo.task_updates (tenant_id, task_id, update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@TenantId, @TaskId, '2025-11-13', @PlannedQty, @PlannedQty * 0.85, @PlannedQty * 0.85, 'Day 9: Steady progress', 1, @UserId, GETDATE(), @UserId, GETDATE());
    END
    
    -- Day 10: Locked date (2025-11-14) - no update should be allowed
    -- We don't insert for this date as it's locked by confirmation
    
    PRINT 'Seeded task updates for task ID: ' + CAST(@TaskId AS VARCHAR) + ' (tenant: TNT_DEMO)';
END
ELSE
BEGIN
    PRINT 'Could not seed task updates: Tenant TNT_DEMO or task not found';
END
GO

