-- Seed resource masters, allocations, and resource-specific business rules

DECLARE @tenantId BIGINT = (SELECT TOP 1 id FROM dbo.tenants ORDER BY id);

IF @tenantId IS NOT NULL
BEGIN
    -- Seed employees
    IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE tenant_id = @tenantId AND name = N'Ram Kumar')
    BEGIN
        INSERT INTO dbo.employees (tenant_id, name, skill_level, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@tenantId, N'Ram Kumar', N'Mason', 800, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE tenant_id = @tenantId AND name = N'Suresh Das')
    BEGIN
        INSERT INTO dbo.employees (tenant_id, name, skill_level, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@tenantId, N'Suresh Das', N'Electrician', 950, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
    END

    -- Additional employees for coverage
    DECLARE @employeeSeed TABLE (name NVARCHAR(200), skill NVARCHAR(100), rate DECIMAL(18,2));
    INSERT INTO @employeeSeed (name, skill, rate)
    VALUES (N'Ravi Singh', N'Carpenter', 780),
           (N'Anil Verma', N'Bar Bender', 820),
           (N'Farhan Ali', N'Painter', 700),
           (N'Gopal Mehta', N'Foreman', 1100),
           (N'Joseph Mathew', N'Welder', 900),
           (N'Prakash Jha', N'Plumber', 850),
           (N'Santhosh Nair', N'Supervisor', 1200),
           (N'Mukesh Rao', N'Helper', 500),
           (N'Vikas Yadav', N'Scaffolder', 650);

    INSERT INTO dbo.employees (tenant_id, name, skill_level, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
    SELECT @tenantId, s.name, s.skill, s.rate, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME()
    FROM @employeeSeed s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.employees e WHERE e.tenant_id = @tenantId AND e.name = s.name);

    -- Seed equipment
    IF NOT EXISTS (SELECT 1 FROM dbo.equipment WHERE tenant_id = @tenantId AND equipment_name = N'JCB Excavator')
    BEGIN
        INSERT INTO dbo.equipment (tenant_id, equipment_name, equipment_type, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@tenantId, N'JCB Excavator', N'Earth Moving', 3500, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.equipment WHERE tenant_id = @tenantId AND equipment_name = N'Concrete Mixer')
    BEGIN
        INSERT INTO dbo.equipment (tenant_id, equipment_name, equipment_type, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
        VALUES (@tenantId, N'Concrete Mixer', N'Concrete Works', 1500, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
    END

    DECLARE @equipmentSeed TABLE (name NVARCHAR(200), type NVARCHAR(100), rate DECIMAL(18,2));
    INSERT INTO @equipmentSeed (name, type, rate)
    VALUES (N'Tower Crane', N'Lifting', 5000),
           (N'Vibratory Roller', N'Compaction', 2800),
           (N'Water Tanker', N'Logistics', 900);

    INSERT INTO dbo.equipment (tenant_id, equipment_name, equipment_type, rate_per_day, activate_flag, created_by, created_on, updated_by, updated_on)
    SELECT @tenantId, s.name, s.type, s.rate, 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME()
    FROM @equipmentSeed s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.equipment e WHERE e.tenant_id = @tenantId AND e.equipment_name = s.name);

    -- Sample allocations referencing WBS 2.1 when available
    DECLARE @targetWbsId BIGINT = (
        SELECT TOP 1 wbs_id
        FROM dbo.wbs
        WHERE tenant_id = @tenantId AND (wbs_code = '2.1' OR wbs_code = 'WBS-2.1' OR wbs_code = 'WBS 2.1')
        ORDER BY wbs_id
    );

    IF @targetWbsId IS NULL
        SET @targetWbsId = (SELECT TOP 1 wbs_id FROM dbo.wbs WHERE tenant_id = @tenantId ORDER BY wbs_id);

    IF @targetWbsId IS NOT NULL
    BEGIN
        DECLARE @ramId BIGINT = (SELECT TOP 1 employee_id FROM dbo.employees WHERE tenant_id = @tenantId AND name = N'Ram Kumar');
        DECLARE @jcbId BIGINT = (SELECT TOP 1 equipment_id FROM dbo.equipment WHERE tenant_id = @tenantId AND equipment_name = N'JCB Excavator');

        IF @ramId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.manpower_allocations WHERE tenant_id = @tenantId AND wbs_id = @targetWbsId AND employee_id = @ramId)
        BEGIN
            INSERT INTO dbo.manpower_allocations (tenant_id, wbs_id, employee_id, start_date, end_date, hours_assigned, duration_days, daily_rate, total_cost, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
            VALUES (@tenantId, @targetWbsId, @ramId, DATEADD(DAY, -10, CAST(GETDATE() AS DATE)), DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), 8, 10, 800, 8000, N'10-day masonry package', 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
        END

        IF @jcbId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.equipment_allocations WHERE tenant_id = @tenantId AND wbs_id = @targetWbsId AND equipment_id = @jcbId)
        BEGIN
            INSERT INTO dbo.equipment_allocations (tenant_id, wbs_id, equipment_id, start_date, end_date, hours_assigned, duration_days, daily_rate, total_cost, remarks, activate_flag, created_by, created_on, updated_by, updated_on)
            VALUES (@tenantId, @targetWbsId, @jcbId, CAST(GETDATE() AS DATE), DATEADD(DAY, 4, CAST(GETDATE() AS DATE)), 10, 5, 3500, 17500, N'Excavation support for WBS 2.1', 1, 1, SYSUTCDATETIME(), 1, SYSUTCDATETIME());
        END
    END
END
GO

-- Seed resource-specific business rules (601, 602) for all tenants if missing
INSERT INTO dbo.business_rules (tenant_id, rule_number, control_point, applicability, rule_value, description, activate_flag, created_by, created_on, updated_by, updated_on)
SELECT t.id,
       601,
       'RESOURCE_ALLOCATION',
       'Y',
       NULL,
       'Allocation dates must stay within WBS dates',
       1,
       1,
       SYSUTCDATETIME(),
       1,
       SYSUTCDATETIME()
FROM dbo.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.business_rules br
    WHERE br.tenant_id = t.id AND br.rule_number = 601
);
GO

INSERT INTO dbo.business_rules (tenant_id, rule_number, control_point, applicability, rule_value, description, activate_flag, created_by, created_on, updated_by, updated_on)
SELECT t.id,
       602,
       'RESOURCE_ALLOCATION',
       'Y',
       NULL,
       'Prevent overlapping allocations for the same resource',
       1,
       1,
       SYSUTCDATETIME(),
       1,
       SYSUTCDATETIME()
FROM dbo.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.business_rules br
    WHERE br.tenant_id = t.id AND br.rule_number = 602
);
GO

