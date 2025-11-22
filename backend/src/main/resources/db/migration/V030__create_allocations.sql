-- Creates manpower and equipment allocation tables with costing columns

IF OBJECT_ID('dbo.manpower_allocations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.manpower_allocations (
        allocation_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        wbs_id BIGINT NOT NULL,
        employee_id BIGINT NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        hours_assigned DECIMAL(9,2) NULL,
        duration_days BIGINT NULL,
        daily_rate DECIMAL(18,2) NULL,
        total_cost DECIMAL(18,2) NULL,
        remarks NVARCHAR(500) NULL,
        activate_flag BIT NOT NULL DEFAULT 1,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_by BIGINT NULL,
        updated_on DATETIME2 NULL,
        CONSTRAINT fk_manpower_alloc_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id),
        CONSTRAINT fk_manpower_alloc_wbs FOREIGN KEY (wbs_id) REFERENCES dbo.wbs(wbs_id),
        CONSTRAINT fk_manpower_alloc_employee FOREIGN KEY (employee_id) REFERENCES dbo.employees(employee_id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_manpower_alloc_tenant' AND object_id = OBJECT_ID('dbo.manpower_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_manpower_alloc_tenant ON dbo.manpower_allocations (tenant_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_manpower_alloc_wbs' AND object_id = OBJECT_ID('dbo.manpower_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_manpower_alloc_wbs ON dbo.manpower_allocations (tenant_id, wbs_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_manpower_alloc_employee' AND object_id = OBJECT_ID('dbo.manpower_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_manpower_alloc_employee ON dbo.manpower_allocations (tenant_id, employee_id, start_date, end_date);
END
GO

IF OBJECT_ID('dbo.equipment_allocations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.equipment_allocations (
        allocation_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id BIGINT NOT NULL,
        wbs_id BIGINT NOT NULL,
        equipment_id BIGINT NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        hours_assigned DECIMAL(9,2) NULL,
        duration_days BIGINT NULL,
        daily_rate DECIMAL(18,2) NULL,
        total_cost DECIMAL(18,2) NULL,
        remarks NVARCHAR(500) NULL,
        activate_flag BIT NOT NULL DEFAULT 1,
        created_by BIGINT NULL,
        created_on DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_by BIGINT NULL,
        updated_on DATETIME2 NULL,
        CONSTRAINT fk_equipment_alloc_tenant FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id),
        CONSTRAINT fk_equipment_alloc_wbs FOREIGN KEY (wbs_id) REFERENCES dbo.wbs(wbs_id),
        CONSTRAINT fk_equipment_alloc_equipment FOREIGN KEY (equipment_id) REFERENCES dbo.equipment(equipment_id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_equipment_alloc_tenant' AND object_id = OBJECT_ID('dbo.equipment_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_equipment_alloc_tenant ON dbo.equipment_allocations (tenant_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_equipment_alloc_wbs' AND object_id = OBJECT_ID('dbo.equipment_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_equipment_alloc_wbs ON dbo.equipment_allocations (tenant_id, wbs_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_equipment_alloc_resource' AND object_id = OBJECT_ID('dbo.equipment_allocations'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_equipment_alloc_resource ON dbo.equipment_allocations (tenant_id, equipment_id, start_date, end_date);
END
GO

