-- Flyway Migration: Create master_codes table
-- This table stores tenant-aware master data / configuration codes

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'master_codes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.master_codes (
        code_id BIGINT IDENTITY(1,1) NOT NULL,
        tenant_id BIGINT NOT NULL,
        code_type VARCHAR(100) NOT NULL,
        code_value VARCHAR(255) NOT NULL,
        short_description VARCHAR(500) NULL,
        long_description NVARCHAR(MAX) NULL,
        activate_flag BIT NOT NULL DEFAULT 1,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_by BIGINT NULL,
        updated_on DATETIME2 NULL,
        
        CONSTRAINT PK_master_codes PRIMARY KEY (code_id),
        CONSTRAINT FK_master_codes_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id),
        CONSTRAINT UQ_master_codes_tenant_type_value UNIQUE (tenant_id, code_type, code_value)
    );

    -- Create indexes for performance
    CREATE INDEX idx_master_codes_tenant_type ON dbo.master_codes(tenant_id, code_type);
    CREATE INDEX idx_master_codes_tenant_type_active ON dbo.master_codes(tenant_id, code_type, activate_flag);
    CREATE INDEX idx_master_codes_code_type ON dbo.master_codes(code_type);
    CREATE INDEX idx_master_codes_created_by ON dbo.master_codes(created_by);
    CREATE INDEX idx_master_codes_updated_by ON dbo.master_codes(updated_by);
END
GO

