-- Seeds sample WBS confirmation + lock for documentation/demo purposes
-- Note: Uses entity_type='WBS' and entity_id to reference WBS since confirmations table uses generic entity structure

DECLARE @wbsId BIGINT = 1;
DECLARE @confirmationDate DATE = '2025-11-10';
DECLARE @tenantId BIGINT;

SELECT @tenantId = tenant_id FROM dbo.wbs WHERE wbs_id = @wbsId;

IF @tenantId IS NOT NULL
BEGIN
    -- Get a user ID for confirmed_by
    DECLARE @confirmedBy BIGINT;
    SELECT TOP 1 @confirmedBy = id FROM dbo.users WHERE tenant_id = @tenantId AND is_active = 1;
    IF @confirmedBy IS NULL
        SET @confirmedBy = 1; -- Default to 1 if no user found

    IF NOT EXISTS (
        SELECT 1 FROM dbo.confirmations
        WHERE tenant_id = @tenantId 
          AND entity_type = 'WBS' 
          AND entity_id = @wbsId 
          AND confirmation_date = @confirmationDate
    )
    BEGIN
        INSERT INTO dbo.confirmations (tenant_id, entity_type, entity_id, confirmation_date, confirmed_by, confirmed_on, remarks, created_by, created_on)
        VALUES (@tenantId, 'WBS', @wbsId, @confirmationDate, @confirmedBy, SYSUTCDATETIME(), N'Seeded freeze for Site Mobilisation', @confirmedBy, SYSUTCDATETIME());
    END

    IF NOT EXISTS (
        SELECT 1 FROM dbo.confirmation_locks WHERE tenant_id = @tenantId AND wbs_id = @wbsId
    )
    BEGIN
        INSERT INTO dbo.confirmation_locks (tenant_id, wbs_id, lock_date, created_by, created_on, updated_on)
        VALUES (@tenantId, @wbsId, @confirmationDate, @confirmedBy, SYSUTCDATETIME(), SYSUTCDATETIME());
    END
    ELSE
    BEGIN
        UPDATE dbo.confirmation_locks
        SET lock_date = CASE WHEN lock_date < @confirmationDate THEN @confirmationDate ELSE lock_date END,
            updated_on = SYSUTCDATETIME()
        WHERE tenant_id = @tenantId AND wbs_id = @wbsId;
    END
END
GO


