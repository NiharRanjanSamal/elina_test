-- Creates confirmation_locks table used to cache latest lock date per WBS

IF OBJECT_ID('dbo.confirmation_locks', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.confirmation_locks (
        lock_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        wbs_id BIGINT NOT NULL,
        lock_date DATE NOT NULL,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    ALTER TABLE dbo.confirmation_locks
        ADD CONSTRAINT fk_confirmation_locks_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);

    ALTER TABLE dbo.confirmation_locks
        ADD CONSTRAINT fk_confirmation_locks_wbs FOREIGN KEY (wbs_id) REFERENCES dbo.wbs(wbs_id);

    ALTER TABLE dbo.confirmation_locks
        ADD CONSTRAINT uk_confirmation_locks_wbs UNIQUE (tenant_id, wbs_id);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_confirmation_locks_tenant' AND object_id = OBJECT_ID('dbo.confirmation_locks'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_confirmation_locks_tenant ON dbo.confirmation_locks(tenant_id);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_confirmation_locks_wbs' AND object_id = OBJECT_ID('dbo.confirmation_locks'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_confirmation_locks_wbs ON dbo.confirmation_locks(tenant_id, wbs_id);
END
GO


