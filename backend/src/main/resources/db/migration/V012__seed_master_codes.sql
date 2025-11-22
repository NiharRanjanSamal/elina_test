-- Flyway Migration: Seed sample master codes for demo tenant (tenant_id = 1)
-- This provides sample data for various code types
-- This migration is idempotent - it will only insert records that don't already exist

-- Note: This assumes tenant_id = 1 exists (DEFAULT tenant from seed data)
-- If tenant_id is different, update accordingly

-- WORK_CENTER codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'WORK_CENTER', 'WC_SITE', 'Site Work Center', 'Primary work center for site operations and field work activities. Used for tracking site-based work assignments and resource allocation.', 1, 1),
    (1, 'WORK_CENTER', 'WC_FOUND', 'Foundation Work Center', 'Work center dedicated to foundation and structural work. Handles all foundation-related activities including excavation, concrete work, and structural installations.', 1, 1),
    (1, 'WORK_CENTER', 'WC_FINISH', 'Finishing Work Center', 'Work center for finishing activities including painting, flooring, fixtures, and final touches. Manages completion phase of projects.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- COST_CENTER codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'COST_CENTER', 'CC_001', 'Cost Center 001', 'Primary cost center for general operations and administrative expenses. Used for budget tracking and financial reporting.', 1, 1),
    (1, 'COST_CENTER', 'CC_002', 'Cost Center 002', 'Secondary cost center for project-specific expenses. Tracks costs related to active projects and initiatives.', 1, 1),
    (1, 'COST_CENTER', 'CC_003', 'Cost Center 003', 'Cost center for maintenance and support activities. Handles ongoing maintenance costs and support operations.', 1, 1),
    (1, 'COST_CENTER', 'CC_004', 'Cost Center 004', 'Cost center for research and development. Tracks R&D expenses and innovation initiatives.', 1, 1),
    (1, 'COST_CENTER', 'CC_005', 'Cost Center 005', 'Cost center for quality assurance and testing. Manages QA-related costs and testing activities.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- ROLE_TYPES (master code version, also exists in roles table)
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'ROLE_TYPES', 'SYSTEM_ADMIN', 'System Administrator', 'Full system access with administrative privileges. Can manage users, roles, permissions, and system configuration.', 1, 1),
    (1, 'ROLE_TYPES', 'SUPERVISOR', 'Supervisor Role', 'Supervisory role with oversight capabilities. Can manage team members, review work, and approve workflows.', 1, 1),
    (1, 'ROLE_TYPES', 'END_USER', 'End User', 'Standard user role with basic access. Can perform assigned tasks and view relevant information.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- WBS_STATUS codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'WBS_STATUS', 'NOT_STARTED', 'Not Started', 'Work breakdown structure item has been created but work has not yet begun. Ready for assignment and scheduling.', 1, 1),
    (1, 'WBS_STATUS', 'IN_PROGRESS', 'In Progress', 'Work breakdown structure item is currently being worked on. Active work is in progress with assigned resources.', 1, 1),
    (1, 'WBS_STATUS', 'COMPLETED', 'Completed', 'Work breakdown structure item has been finished and completed. All work is done and verified.', 1, 1),
    (1, 'WBS_STATUS', 'ON_HOLD', 'On Hold', 'Work breakdown structure item is temporarily paused. Work is suspended pending resolution of blockers or dependencies.', 1, 1),
    (1, 'WBS_STATUS', 'CANCELLED', 'Cancelled', 'Work breakdown structure item has been cancelled. Work will not proceed and item is closed.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- REVIEW_FREQUENCY codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'REVIEW_FREQUENCY', 'DAILY', 'Daily Review', 'Items requiring daily review and status updates. Used for critical or time-sensitive activities.', 1, 1),
    (1, 'REVIEW_FREQUENCY', 'WEEKLY', 'Weekly Review', 'Items reviewed on a weekly basis. Standard frequency for most ongoing activities and projects.', 1, 1),
    (1, 'REVIEW_FREQUENCY', 'FORTNIGHT', 'Fortnightly Review', 'Items reviewed every two weeks. Used for less critical activities or longer-term projects.', 1, 1),
    (1, 'REVIEW_FREQUENCY', 'MONTHLY', 'Monthly Review', 'Items reviewed monthly. Used for strategic initiatives and long-term planning activities.', 1, 1),
    (1, 'REVIEW_FREQUENCY', 'QUARTERLY', 'Quarterly Review', 'Items reviewed quarterly. Used for high-level strategic reviews and annual planning cycles.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- TASK_PRIORITY codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'TASK_PRIORITY', 'CRITICAL', 'Critical Priority', 'Highest priority tasks requiring immediate attention. These tasks block other work or have severe business impact if delayed.', 1, 1),
    (1, 'TASK_PRIORITY', 'HIGH', 'High Priority', 'High priority tasks that should be completed soon. Important for project success but not immediately blocking.', 1, 1),
    (1, 'TASK_PRIORITY', 'MEDIUM', 'Medium Priority', 'Standard priority tasks with normal scheduling. Completed as part of regular workflow.', 1, 1),
    (1, 'TASK_PRIORITY', 'LOW', 'Low Priority', 'Lower priority tasks that can be deferred if needed. Nice to have but not critical for immediate goals.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- PROJECT_TYPE codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'PROJECT_TYPE', 'CONSTRUCTION', 'Construction Project', 'Construction and building projects including new builds, renovations, and infrastructure development.', 1, 1),
    (1, 'PROJECT_TYPE', 'MAINTENANCE', 'Maintenance Project', 'Maintenance and repair projects for existing facilities and infrastructure.', 1, 1),
    (1, 'PROJECT_TYPE', 'RENOVATION', 'Renovation Project', 'Renovation and upgrade projects for existing structures and facilities.', 1, 1),
    (1, 'PROJECT_TYPE', 'PLANNING', 'Planning Project', 'Planning and design projects including feasibility studies and design development.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- RESOURCE_TYPE codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'RESOURCE_TYPE', 'LABOR', 'Labor Resource', 'Human resources including workers, technicians, and skilled labor. Tracks labor hours and costs.', 1, 1),
    (1, 'RESOURCE_TYPE', 'EQUIPMENT', 'Equipment Resource', 'Equipment and machinery resources including tools, vehicles, and heavy machinery. Tracks equipment usage and rental costs.', 1, 1),
    (1, 'RESOURCE_TYPE', 'MATERIAL', 'Material Resource', 'Material resources including raw materials, supplies, and consumables. Tracks material costs and inventory.', 1, 1),
    (1, 'RESOURCE_TYPE', 'SERVICE', 'Service Resource', 'Service resources including subcontractors, consultants, and external services. Tracks service costs and contracts.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

-- ALLOCATION_STATUS codes
MERGE INTO dbo.master_codes AS target
USING (VALUES 
    (1, 'ALLOCATION_STATUS', 'PLANNED', 'Planned Allocation', 'Resource allocation that has been planned but not yet confirmed. Pending approval or scheduling.', 1, 1),
    (1, 'ALLOCATION_STATUS', 'CONFIRMED', 'Confirmed Allocation', 'Resource allocation that has been confirmed and scheduled. Resources are committed to the allocation.', 1, 1),
    (1, 'ALLOCATION_STATUS', 'ACTIVE', 'Active Allocation', 'Resource allocation that is currently active. Resources are being used for the allocated work.', 1, 1),
    (1, 'ALLOCATION_STATUS', 'COMPLETED', 'Completed Allocation', 'Resource allocation that has been completed. Work is finished and resources are released.', 1, 1),
    (1, 'ALLOCATION_STATUS', 'CANCELLED', 'Cancelled Allocation', 'Resource allocation that has been cancelled. Resources were not used or allocation was revoked.', 1, 1)
) AS source (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by)
ON target.tenant_id = source.tenant_id 
   AND target.code_type = source.code_type 
   AND target.code_value = source.code_value
WHEN NOT MATCHED THEN
    INSERT (tenant_id, code_type, code_value, short_description, long_description, activate_flag, created_by, created_on)
    VALUES (source.tenant_id, source.code_type, source.code_value, source.short_description, source.long_description, source.activate_flag, source.created_by, GETDATE());

GO

