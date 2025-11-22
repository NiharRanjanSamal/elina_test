-- Audit triggers for manpower and equipment allocation tables

IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_manpower_allocations_audit' AND parent_id = OBJECT_ID('dbo.manpower_allocations'))
BEGIN
    DROP TRIGGER dbo.trg_manpower_allocations_audit;
END
GO

CREATE TRIGGER dbo.trg_manpower_allocations_audit
ON dbo.manpower_allocations
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ActionType VARCHAR(20);
    IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted)
        SET @ActionType = 'UPDATE';
    ELSE IF EXISTS (SELECT 1 FROM inserted)
        SET @ActionType = 'INSERT';
    ELSE
        SET @ActionType = 'DELETE';

    IF @ActionType IN ('INSERT', 'UPDATE')
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT i.tenant_id,
               'MANPOWER_ALLOCATIONS',
               i.allocation_id,
               @ActionType,
               CASE WHEN @ActionType = 'UPDATE' THEN (
                    SELECT allocation_id, wbs_id, employee_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM deleted d WHERE d.allocation_id = i.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ) END,
               (
                    SELECT allocation_id, wbs_id, employee_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM inserted i2 WHERE i2.allocation_id = i.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ),
               ISNULL(i.updated_by, 0),
               SYSUTCDATETIME()
        FROM inserted i;
    END

    IF @ActionType = 'DELETE'
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT d.tenant_id,
               'MANPOWER_ALLOCATIONS',
               d.allocation_id,
               'DELETE',
               (
                    SELECT allocation_id, wbs_id, employee_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM deleted d2 WHERE d2.allocation_id = d.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ),
               NULL,
               ISNULL(d.updated_by, 0),
               SYSUTCDATETIME()
        FROM deleted d;
    END
END
GO

IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_equipment_allocations_audit' AND parent_id = OBJECT_ID('dbo.equipment_allocations'))
BEGIN
    DROP TRIGGER dbo.trg_equipment_allocations_audit;
END
GO

CREATE TRIGGER dbo.trg_equipment_allocations_audit
ON dbo.equipment_allocations
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ActionType VARCHAR(20);
    IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted)
        SET @ActionType = 'UPDATE';
    ELSE IF EXISTS (SELECT 1 FROM inserted)
        SET @ActionType = 'INSERT';
    ELSE
        SET @ActionType = 'DELETE';

    IF @ActionType IN ('INSERT', 'UPDATE')
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT i.tenant_id,
               'EQUIPMENT_ALLOCATIONS',
               i.allocation_id,
               @ActionType,
               CASE WHEN @ActionType = 'UPDATE' THEN (
                    SELECT allocation_id, wbs_id, equipment_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM deleted d WHERE d.allocation_id = i.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ) END,
               (
                    SELECT allocation_id, wbs_id, equipment_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM inserted i2 WHERE i2.allocation_id = i.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ),
               ISNULL(i.updated_by, 0),
               SYSUTCDATETIME()
        FROM inserted i;
    END

    IF @ActionType = 'DELETE'
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT d.tenant_id,
               'EQUIPMENT_ALLOCATIONS',
               d.allocation_id,
               'DELETE',
               (
                    SELECT allocation_id, wbs_id, equipment_id, start_date, end_date, hours_assigned, duration_days, total_cost, remarks
                    FROM deleted d2 WHERE d2.allocation_id = d.allocation_id
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
               ),
               NULL,
               ISNULL(d.updated_by, 0),
               SYSUTCDATETIME()
        FROM deleted d;
    END
END
GO

