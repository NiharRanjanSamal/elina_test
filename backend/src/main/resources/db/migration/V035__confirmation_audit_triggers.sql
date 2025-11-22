-- Creates DML triggers that pipe confirmation changes into audit_logs

IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_confirmations_audit' AND parent_id = OBJECT_ID('dbo.confirmations'))
BEGIN
    DROP TRIGGER dbo.trg_confirmations_audit;
END
GO

CREATE TRIGGER dbo.trg_confirmations_audit
ON dbo.confirmations
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
        SELECT
            i.tenant_id,
            'CONFIRMATIONS',
            i.confirmation_id,
            @ActionType,
            CASE WHEN @ActionType = 'UPDATE'
                 THEN (SELECT entity_type, entity_id, confirmation_date, confirmed_by, confirmed_on, remarks FROM deleted d WHERE d.confirmation_id = i.confirmation_id FOR JSON PATH)
                 ELSE NULL
            END,
            (SELECT entity_type, entity_id, confirmation_date, confirmed_by, confirmed_on, remarks FROM inserted ii WHERE ii.confirmation_id = i.confirmation_id FOR JSON PATH),
            ISNULL(i.created_by, 0),
            SYSUTCDATETIME()
        FROM inserted i;
    END

    IF @ActionType = 'DELETE'
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT
            d.tenant_id,
            'CONFIRMATIONS',
            d.confirmation_id,
            'DELETE',
            (SELECT entity_type, entity_id, confirmation_date, confirmed_by, confirmed_on, remarks FROM deleted dd WHERE dd.confirmation_id = d.confirmation_id FOR JSON PATH),
            NULL,
            ISNULL(d.created_by, 0),
            SYSUTCDATETIME()
        FROM deleted d;
    END
END
GO

IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_confirmation_locks_audit' AND parent_id = OBJECT_ID('dbo.confirmation_locks'))
BEGIN
    DROP TRIGGER dbo.trg_confirmation_locks_audit;
END
GO

CREATE TRIGGER dbo.trg_confirmation_locks_audit
ON dbo.confirmation_locks
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
        SELECT
            i.tenant_id,
            'CONFIRMATION_LOCKS',
            i.lock_id,
            @ActionType,
            CASE WHEN @ActionType = 'UPDATE'
                 THEN (SELECT lock_date FROM deleted d WHERE d.lock_id = i.lock_id FOR JSON PATH)
                 ELSE NULL
            END,
            (SELECT lock_date FROM inserted ii WHERE ii.lock_id = i.lock_id FOR JSON PATH),
            ISNULL(i.created_by, 0),
            SYSUTCDATETIME()
        FROM inserted i;
    END

    IF @ActionType = 'DELETE'
    BEGIN
        INSERT INTO dbo.audit_logs (tenant_id, table_name, record_id, action_type, old_data, new_data, changed_by, changed_on)
        SELECT
            d.tenant_id,
            'CONFIRMATION_LOCKS',
            d.lock_id,
            'DELETE',
            (SELECT lock_date FROM deleted dd WHERE dd.lock_id = d.lock_id FOR JSON PATH),
            NULL,
            ISNULL(d.created_by, 0),
            SYSUTCDATETIME()
        FROM deleted d;
    END
END
GO


