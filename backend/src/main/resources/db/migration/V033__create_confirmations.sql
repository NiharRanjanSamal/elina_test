-- Creates confirmations table as per module specification

IF OBJECT_ID('dbo.confirmations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.confirmations (
        confirmation_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        wbs_id BIGINT NOT NULL,
        confirmation_date DATE NOT NULL,
        confirmed_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
        remarks NVARCHAR(1000) NULL,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    ALTER TABLE dbo.confirmations
        ADD CONSTRAINT fk_confirmations_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);

    ALTER TABLE dbo.confirmations
        ADD CONSTRAINT fk_confirmations_wbs FOREIGN KEY (wbs_id) REFERENCES dbo.wbs(wbs_id);

    ALTER TABLE dbo.confirmations
        ADD CONSTRAINT uk_confirmations_wbs_date UNIQUE (tenant_id, wbs_id, confirmation_date);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_confirmations_tenant' AND object_id = OBJECT_ID('dbo.confirmations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_confirmations_tenant ON dbo.confirmations(tenant_id);
END
GO

-- Only create index if wbs_id column exists in the table
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.confirmations') AND name = 'wbs_id')
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_confirmations_wbs' AND object_id = OBJECT_ID('dbo.confirmations'))
    BEGIN
        CREATE NONCLUSTERED INDEX idx_confirmations_wbs ON dbo.confirmations(tenant_id, wbs_id, confirmation_date);
    END
END
GO


