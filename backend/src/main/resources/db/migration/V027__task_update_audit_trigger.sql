-- Flyway Migration: Create audit trigger for task_updates table
-- Automatically logs INSERT, UPDATE, DELETE operations to audit_logs table

IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_task_updates_audit' AND parent_id = OBJECT_ID('dbo.task_updates'))
BEGIN
    DROP TRIGGER dbo.trg_task_updates_audit;
END
GO

CREATE TRIGGER dbo.trg_task_updates_audit
ON dbo.task_updates
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @ActionType VARCHAR(20);
    DECLARE @UpdateId BIGINT;
    DECLARE @TenantId BIGINT;
    DECLARE @ChangedBy BIGINT;
    DECLARE @OldData NVARCHAR(MAX);
    DECLARE @NewData NVARCHAR(MAX);
    
    -- Determine action type
    IF EXISTS (SELECT * FROM inserted) AND EXISTS (SELECT * FROM deleted)
    BEGIN
        SET @ActionType = 'UPDATE';
    END
    ELSE IF EXISTS (SELECT * FROM inserted)
    BEGIN
        SET @ActionType = 'INSERT';
    END
    ELSE IF EXISTS (SELECT * FROM deleted)
    BEGIN
        SET @ActionType = 'DELETE';
    END
    
    -- Process INSERT and UPDATE
    IF @ActionType IN ('INSERT', 'UPDATE')
    BEGIN
        DECLARE insert_cursor CURSOR FOR
        SELECT update_id, tenant_id, updated_by, 
               (SELECT update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag
                FROM inserted i
                WHERE i.update_id = inserted.update_id
                FOR JSON PATH) AS new_data_json
        FROM inserted;
        
        OPEN insert_cursor;
        FETCH NEXT FROM insert_cursor INTO @UpdateId, @TenantId, @ChangedBy, @NewData;
        
        WHILE @@FETCH_STATUS = 0
        BEGIN
            -- For UPDATE, get old data
            IF @ActionType = 'UPDATE'
            BEGIN
                SELECT @OldData = (
                    SELECT update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag
                    FROM deleted d
                    WHERE d.update_id = @UpdateId
                    FOR JSON PATH
                );
            END
            
            -- Insert audit log
            INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
            VALUES (@TenantId, 'TASK_UPDATES', @UpdateId, @ActionType, @OldData, @NewData, 
                    ISNULL(@ChangedBy, 0), GETDATE());
            
            SET @OldData = NULL;
            FETCH NEXT FROM insert_cursor INTO @UpdateId, @TenantId, @ChangedBy, @NewData;
        END
        
        CLOSE insert_cursor;
        DEALLOCATE insert_cursor;
    END
    
    -- Process DELETE
    IF @ActionType = 'DELETE'
    BEGIN
        DECLARE delete_cursor CURSOR FOR
        SELECT update_id, tenant_id, updated_by,
               (SELECT update_date, planned_qty, actual_qty, daily_update_qty, remarks, activate_flag
                FROM deleted d
                WHERE d.update_id = deleted.update_id
                FOR JSON PATH) AS old_data_json
        FROM deleted;
        
        OPEN delete_cursor;
        FETCH NEXT FROM delete_cursor INTO @UpdateId, @TenantId, @ChangedBy, @OldData;
        
        WHILE @@FETCH_STATUS = 0
        BEGIN
            -- Insert audit log
            INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
            VALUES (@TenantId, 'TASK_UPDATES', @UpdateId, @ActionType, @OldData, NULL, 
                    ISNULL(@ChangedBy, 0), GETDATE());
            
            FETCH NEXT FROM delete_cursor INTO @UpdateId, @TenantId, @ChangedBy, @OldData;
        END
        
        CLOSE delete_cursor;
        DEALLOCATE delete_cursor;
    END
END
GO

