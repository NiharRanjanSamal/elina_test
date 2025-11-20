# Master Data / Configuration Management Module

## Overview

The Master Data Management module provides a comprehensive solution for managing tenant-aware configuration codes across the application. It includes CRUD operations, bulk upload capabilities, Redis caching, and intelligent UI rendering (radio buttons vs dropdowns based on code count).

## Features

- **Tenant-Aware**: All master codes are scoped to tenants, ensuring complete data isolation
- **Redis Caching**: Master codes are cached for performance with configurable TTL
- **Bulk Upload**: Support for CSV and Excel file uploads with validation and dry-run preview
- **Intelligent UI**: Automatically renders radio buttons (≤3 codes) or dropdowns (>3 codes)
- **Inline Documentation**: Long descriptions provide context and help text for each code type
- **Audit Trail**: All operations track created_by, created_on, updated_by, updated_on
- **Permission-Based Access**: Write operations require `PAGE_MASTER_DATA_EDIT`, read requires `PAGE_MASTER_DATA_VIEW`

## Database Schema

### master_codes Table

```sql
CREATE TABLE master_codes (
    code_id BIGINT IDENTITY(1,1) PRIMARY KEY,
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
    
    CONSTRAINT FK_master_codes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT UQ_master_codes_tenant_type_value UNIQUE (tenant_id, code_type, code_value)
);
```

**Indexes:**
- `idx_master_codes_tenant_type` on (tenant_id, code_type)
- `idx_master_codes_tenant_type_active` on (tenant_id, code_type, activate_flag)
- `idx_master_codes_code_type` on (code_type)

## API Endpoints

### List Master Codes
```
GET /api/master-codes?codeType=&search=&activeOnly=true&page=0&size=20
```
**Query Parameters:**
- `codeType` (optional): Filter by code type
- `search` (optional): Search in code value and short description
- `activeOnly` (optional, default: false): Show only active codes
- `page` (optional, default: 0): Page number (0-indexed)
- `size` (optional, default: 20): Page size

**Response:** Paginated list of MasterCodeDTO

**Permissions:** `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`

### Get Master Code by ID
```
GET /api/master-codes/{id}
```
**Response:** MasterCodeDTO

**Permissions:** `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`

### Get Active Master Codes by Type
```
GET /api/master-codes/by-type/{codeType}
```
**Response:** List<MasterCodeDTO> (cached in Redis)

**Permissions:** `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`

### Get Active Count by Type
```
GET /api/master-codes/count?codeType={codeType}&limit=3
```
**Response:** MasterCodeCountDTO
```json
{
  "codeType": "WORK_CENTER",
  "activeCount": 2,
  "useRadio": true
}
```

**Permissions:** `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`

### Get All Code Types
```
GET /api/master-codes/code-types
```
**Response:** List<String>

**Permissions:** `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`

### Create Master Code
```
POST /api/master-codes
Content-Type: application/json

{
  "codeType": "WORK_CENTER",
  "codeValue": "WC_SITE",
  "shortDescription": "Site Work Center",
  "longDescription": "Primary work center for site operations...",
  "activateFlag": true
}
```
**Response:** MasterCodeDTO (201 Created)

**Permissions:** `PAGE_MASTER_DATA_EDIT` or `ROLE_SYSTEM_ADMIN`

### Update Master Code
```
PUT /api/master-codes/{id}
Content-Type: application/json

{
  "codeType": "WORK_CENTER",
  "codeValue": "WC_SITE",
  "shortDescription": "Updated description",
  "longDescription": "Updated long description",
  "activateFlag": true
}
```
**Response:** MasterCodeDTO

**Permissions:** `PAGE_MASTER_DATA_EDIT` or `ROLE_SYSTEM_ADMIN`

### Delete Master Code
```
DELETE /api/master-codes/{id}
```
**Response:** 204 No Content

**Permissions:** `PAGE_MASTER_DATA_EDIT` or `ROLE_SYSTEM_ADMIN`

### Bulk Upload
```
POST /api/master-codes/bulk-upload
Content-Type: multipart/form-data

file: <CSV or Excel file>
dryRun: true/false
```
**Response:** BulkUploadResult

**Permissions:** `PAGE_MASTER_DATA_EDIT` or `ROLE_SYSTEM_ADMIN`

### Refresh Cache
```
POST /api/master-codes/refresh-cache?codeType={codeType}
```
**Response:** 
```json
{
  "message": "Cache refreshed successfully"
}
```

**Permissions:** `ROLE_SYSTEM_ADMIN`

## Bulk Upload CSV Template

### CSV Format

```csv
code_type,code_value,short_description,long_description
WORK_CENTER,WC_SITE,Site Work Center,Primary work center for site operations
WORK_CENTER,WC_FOUND,Foundation Work Center,Work center for foundation work
COST_CENTER,CC_001,Cost Center 001,Primary cost center for operations
COST_CENTER,CC_002,Cost Center 002,Secondary cost center
```

### Excel Format

Same columns as CSV:
- **code_type** (required): The code type (e.g., WORK_CENTER, COST_CENTER)
- **code_value** (required): The code value (e.g., WC_SITE, CC_001)
- **short_description** (optional, required for WORK_CENTER and COST_CENTER): Brief description
- **long_description** (optional): Detailed documentation

### Validation Rules

1. `code_type` is required and cannot be empty
2. `code_value` is required and cannot be empty
3. `short_description` is required for critical code types:
   - `WORK_CENTER`
   - `COST_CENTER`
4. Uniqueness: `(tenant_id, code_type, code_value)` must be unique
5. If a code already exists, it will be updated (in commit mode)

### Dry Run vs Commit

- **Dry Run (dryRun=true)**: Validates the file and shows preview without making changes
- **Commit (dryRun=false)**: Actually creates/updates master codes in the database

## Redis Caching

### Cache Configuration

Cache is configured in `application.yml`:

```yaml
master-data:
  cache:
    ttl-minutes: 30  # Cache TTL in minutes
    enabled: true     # Enable/disable caching
```

### Cache Keys

Cache keys follow the pattern:
```
master_codes:{tenantId}:{codeType}:active
master_codes:{tenantId}:{codeType}:{codeValue}
```

### Cache Invalidation

Cache is automatically invalidated on:
- Create master code
- Update master code
- Delete master code
- Bulk upload (commit mode)
- Manual refresh via `/api/master-codes/refresh-cache`

### Graceful Fallback

If Redis is unavailable, the system will:
1. Log a warning
2. Fall back to database queries
3. Continue operating normally (without caching)

## Frontend Components

### MasterDataList

Main page for managing master codes with:
- Search and filtering
- Pagination
- Create/Edit/Delete operations
- Bulk upload button

**Location:** `elina/frontend/src/pages/admin/MasterData.jsx`

### MasterDataEdit

Modal component for creating/editing master codes:
- Form validation
- Inline code type documentation
- Support for new code types

**Location:** `elina/frontend/src/components/MasterDataEdit.jsx`

### MasterDataSelect

Intelligent selection component that:
- Automatically fetches count via `/api/master-codes/count`
- Renders radio buttons if count ≤ limitForRadio (default: 3)
- Renders dropdown if count > limitForRadio
- Shows short descriptions in tooltips/labels
- Caches results per tenant per code type

**Usage:**
```jsx
<MasterDataSelect
  codeType="WORK_CENTER"
  value={selectedValue}
  onChange={setSelectedValue}
  label="Work Center"
  required={true}
  limitForRadio={3}
/>
```

**Location:** `elina/frontend/src/components/MasterDataSelect.jsx`

### BulkUpload

Modal component for bulk upload:
- File selection (CSV/Excel)
- Validation with preview
- Dry-run mode
- Commit mode
- Error highlighting

**Location:** `elina/frontend/src/components/BulkUpload.jsx`

## Sample Code Types

The seed data includes sample codes for:

- **WORK_CENTER**: WC_SITE, WC_FOUND, WC_FINISH
- **COST_CENTER**: CC_001, CC_002, CC_003, CC_004, CC_005
- **ROLE_TYPES**: SYSTEM_ADMIN, SUPERVISOR, END_USER
- **WBS_STATUS**: NOT_STARTED, IN_PROGRESS, COMPLETED, ON_HOLD, CANCELLED
- **REVIEW_FREQUENCY**: DAILY, WEEKLY, FORTNIGHT, MONTHLY, QUARTERLY
- **TASK_PRIORITY**: CRITICAL, HIGH, MEDIUM, LOW
- **PROJECT_TYPE**: CONSTRUCTION, MAINTENANCE, RENOVATION, PLANNING
- **RESOURCE_TYPE**: LABOR, EQUIPMENT, MATERIAL, SERVICE
- **ALLOCATION_STATUS**: PLANNED, CONFIRMED, ACTIVE, COMPLETED, CANCELLED

## Security Considerations

1. **Tenant Isolation**: All queries automatically filter by tenant_id from JWT
2. **Permission Checks**: 
   - Read operations: `PAGE_MASTER_DATA_VIEW` or `PAGE_MASTER_DATA_EDIT`
   - Write operations: `PAGE_MASTER_DATA_EDIT` or `ROLE_SYSTEM_ADMIN`
3. **Audit Fields**: `created_by` and `updated_by` are automatically set from JWT user_id
4. **Input Validation**: All inputs are validated on both client and server
5. **Unique Constraints**: Database enforces uniqueness per tenant

## Testing

### Unit Tests

```bash
cd elina/backend
mvn test -Dtest=MasterCodeServiceTest
```

### Integration Tests

```bash
mvn test -Dtest=MasterCodeControllerIntegrationTest
```

## Troubleshooting

### Cache Not Working

1. Check Redis connection in `application.yml`
2. Verify Redis is running: `redis-cli ping`
3. Check cache configuration: `master-data.cache.enabled=true`
4. Review logs for cache errors

### Bulk Upload Fails

1. Verify file format (CSV or Excel)
2. Check required columns are present
3. Review validation errors in response
4. Ensure tenant context is set (user is authenticated)

### Permission Denied

1. Verify user has `PAGE_MASTER_DATA_EDIT` permission
2. Check JWT token includes permissions
3. Verify user's role has the required permission

### Codes Not Showing in UI

1. Check `activate_flag` is true
2. Verify tenant_id matches current user's tenant
3. Check cache - try refreshing cache
4. Review browser console for API errors

## Migration Files

- `V011__create_master_codes.sql`: Creates the master_codes table
- `V012__seed_master_codes.sql`: Seeds sample master codes

These are Flyway migrations located in `elina/backend/src/main/resources/db/migration/`

## Configuration

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
```

### Master Data Cache Configuration

```yaml
master-data:
  cache:
    ttl-minutes: ${MASTER_DATA_CACHE_TTL:30}
    enabled: ${MASTER_DATA_CACHE_ENABLED:true}
```

## Future Enhancements

- [ ] Support for hierarchical codes (parent_code relationships)
- [ ] Code type templates with predefined fields
- [ ] Export master codes to CSV/Excel
- [ ] Code value validation rules per code type
- [ ] Multi-language support for descriptions
- [ ] Code usage tracking and analytics

