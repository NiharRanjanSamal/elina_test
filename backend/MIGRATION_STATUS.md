# Master Data Module - Migration Status

## ✅ Completed Setup Steps

### 1. Database Migrations (Liquibase)

Created Liquibase changesets that will run automatically when the Spring Boot application starts:

- ✅ **011-create-master-codes-table.xml** - Creates the `master_codes` table with all required fields, indexes, and constraints
- ✅ **012-seed-master-codes.xml** - Seeds sample master codes for 10+ code types (50+ codes total)
- ✅ **013-add-master-data-permissions.xml** - Creates `PAGE_MASTER_DATA_VIEW` and `PAGE_MASTER_DATA_EDIT` permissions and assigns them to SYSTEM_ADMIN role

**Status:** Migrations are included in `db.changelog-master.xml` and will run automatically on application startup.

### 2. Redis Configuration

✅ **Redis Cache Configuration:**
- Added Redis dependency to `pom.xml`
- Configured Redis connection in `application.yml`
- Created `RedisCacheConfig.java` with graceful fallback
- Cache will use NoOpCacheManager if Redis is unavailable (with warning log)

**To disable Redis caching:**
```yaml
master-data:
  cache:
    enabled: false
```

**To use Redis (default):**
- Ensure Redis is running on `localhost:6379` (or configure host/port in `application.yml`)
- Or set environment variables: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

### 3. Permissions Created

✅ **Permissions will be created automatically via migration:**
- `PAGE_MASTER_DATA_VIEW` - For read access to master data
- `PAGE_MASTER_DATA_EDIT` - For write access (create, update, delete, bulk upload)

✅ **Permissions assigned to SYSTEM_ADMIN role** (role_id = 1)

## 🚀 Next Steps

### Start the Application

The migrations will run automatically when you start the Spring Boot application:

```bash
cd elina/backend
mvn spring-boot:run
```

Or if using an IDE, just run the `AuthorizationModuleApplication` class.

### Verify Migrations

After starting the application, check the logs for:
- `Liquibase: Update Database` messages
- Any migration errors (should be none)

### Verify Permissions

You can verify the permissions were created by:
1. Logging in as admin (admin@example.com / Admin@123)
2. The JWT token should include the new permissions
3. Or query the database:
   ```sql
   SELECT * FROM permissions WHERE code IN ('PAGE_MASTER_DATA_VIEW', 'PAGE_MASTER_DATA_EDIT');
   ```

### Verify Master Codes

Check that master codes were seeded:
```sql
SELECT code_type, COUNT(*) as count 
FROM master_codes 
WHERE tenant_id = 1 
GROUP BY code_type;
```

You should see codes for:
- WORK_CENTER (3 codes)
- COST_CENTER (5 codes)
- ROLE_TYPES (3 codes)
- WBS_STATUS (5 codes)
- REVIEW_FREQUENCY (5 codes)
- TASK_PRIORITY (4 codes)
- PROJECT_TYPE (4 codes)
- RESOURCE_TYPE (4 codes)
- ALLOCATION_STATUS (5 codes)

## 📝 Manual Migration (If Needed)

If you need to run migrations manually (without starting the app), you can use:

```bash
cd elina/backend
mvn liquibase:update -Dliquibase.url=jdbc:sqlserver://localhost:1433;databaseName=elina -Dliquibase.username=elina -Dliquibase.password=elina123
```

Or configure the Liquibase Maven plugin in `pom.xml` with database connection details.

## ⚠️ Important Notes

1. **Liquibase runs automatically** - No manual intervention needed if you start the Spring Boot app
2. **Redis is optional** - The system will work without Redis (just no caching)
3. **Permissions are tenant-scoped** - Created for tenant_id = 1 (DEFAULT tenant)
4. **Role assignments** - Permissions are assigned to SYSTEM_ADMIN role (role_id = 1)

## ✅ All Setup Complete!

The Master Data module is fully configured and ready to use. Just start the application and the migrations will run automatically.

