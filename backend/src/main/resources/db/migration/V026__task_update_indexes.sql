-- Flyway Migration: Create additional indexes for task_updates table
-- Enhances query performance for day-wise updates module

-- Index for date range queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_task_updates_date_range' AND object_id = OBJECT_ID('dbo.task_updates'))
BEGIN
    CREATE INDEX idx_task_updates_date_range ON dbo.task_updates(tenant_id, task_id, update_date);
END
GO

-- Index for summary/reporting queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_task_updates_summary' AND object_id = OBJECT_ID('dbo.task_updates'))
BEGIN
    CREATE INDEX idx_task_updates_summary ON dbo.task_updates(tenant_id, task_id, update_date, actual_qty, planned_qty);
END
GO

