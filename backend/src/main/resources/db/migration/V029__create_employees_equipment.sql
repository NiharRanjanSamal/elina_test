-- Creates employees and equipment master tables for resource allocation

IF OBJECT_ID('dbo.employees', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.employees (
        employee_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        name NVARCHAR(200) NOT NULL,
        skill_level NVARCHAR(100) NULL,
        rate_per_day DECIMAL(18,2) NOT NULL DEFAULT 0,
        activate_flag BIT NOT NULL DEFAULT 1,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_by BIGINT NULL,
        updated_on DATETIME2 NULL,
        CONSTRAINT fk_employees_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_employees_tenant' AND object_id = OBJECT_ID('dbo.employees'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_employees_tenant ON dbo.employees (tenant_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_employees_tenant_active' AND object_id = OBJECT_ID('dbo.employees'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_employees_tenant_active ON dbo.employees (tenant_id, activate_flag);
END
GO

IF OBJECT_ID('dbo.equipment', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.equipment (
        equipment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        equipment_name NVARCHAR(200) NOT NULL,
        equipment_type NVARCHAR(100) NULL,
        rate_per_day DECIMAL(18,2) NOT NULL DEFAULT 0,
        activate_flag BIT NOT NULL DEFAULT 1,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_by BIGINT NULL,
        updated_on DATETIME2 NULL,
        CONSTRAINT fk_equipment_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_equipment_tenant' AND object_id = OBJECT_ID('dbo.equipment'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_equipment_tenant ON dbo.equipment (tenant_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_equipment_tenant_active' AND object_id = OBJECT_ID('dbo.equipment'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_equipment_tenant_active ON dbo.equipment (tenant_id, activate_flag);
END
GO

