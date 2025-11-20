# How to Check SQL Server is Running and Accessible

## Method 1: Check Windows Services (Recommended)

1. Press `Win + R` to open Run dialog
2. Type `services.msc` and press Enter
3. Look for these services:
   - **SQL Server (SQLEXPRESS)** - Should be "Running"
   - **SQL Server Browser** - Should be "Running" (Required for named instances like SQLEXPRESS)
4. If not running, right-click and select "Start"

## Method 2: Using SQL Server Management Studio (SSMS)

1. Open SQL Server Management Studio
2. In the "Connect to Server" dialog:
   - **Server name**: `LAPTOP-R5A7J452\SQLEXPRESS` or `localhost\SQLEXPRESS`
   - **Authentication**: SQL Server Authentication
   - **Login**: `elina`
   - **Password**: `elina123`
3. Click "Connect"
4. If connection succeeds, SQL Server is accessible

## Method 3: Using Command Line (sqlcmd)

Open PowerShell or Command Prompt and run:

```powershell
sqlcmd -S LAPTOP-R5A7J452\SQLEXPRESS -U elina -P elina123 -Q "SELECT @@VERSION"
```

If successful, it will display SQL Server version information.

## Method 4: Using PowerShell

```powershell
# Check if SQL Server service is running
Get-Service | Where-Object {$_.DisplayName -like "*SQL Server*"}

# Check if port 1433 (default) or named instance port is listening
Test-NetConnection -ComputerName LAPTOP-R5A7J452 -Port 1433
```

## Method 5: Check SQL Server Browser Service (Critical for Named Instances)

The SQL Server Browser service is required for named instances (like SQLEXPRESS):

1. Open Services (`services.msc`)
2. Find **SQL Server Browser**
3. Ensure it's "Running"
4. If not, start it and set it to "Automatic" startup

## Method 6: Test Connection from Java Application

Create a simple test class or use this Maven command:

```bash
# Test database connection
cd elina/backend
mvn spring-boot:run
```

If the application starts without database connection errors, SQL Server is accessible.

## Method 7: Check Firewall

If connection fails, check Windows Firewall:

1. Open Windows Defender Firewall
2. Check if port 1433 (SQL Server) and 1434 (SQL Server Browser) are allowed
3. Or temporarily disable firewall to test

## Common Issues and Solutions

### Issue: "Cannot connect to named instance"
**Solution**: Ensure SQL Server Browser service is running

### Issue: "Connection timeout"
**Solution**: 
- Check if SQL Server is configured to accept TCP/IP connections
- Verify firewall settings
- Check if SQL Server is listening on the correct port

### Issue: "Login failed for user"
**Solution**: 
- Verify username/password are correct
- Check if SQL Server Authentication is enabled (not just Windows Authentication)
- Verify the user has access to the database

## Quick Test Script

Save this as `test-connection.ps1`:

```powershell
# Test SQL Server Connection
$serverName = "LAPTOP-R5A7J452\SQLEXPRESS"
$databaseName = "elina"
$username = "elina"
$password = "elina123"

Write-Host "Testing SQL Server connection..."
Write-Host "Server: $serverName"
Write-Host "Database: $databaseName"

try {
    $connectionString = "Server=$serverName;Database=$databaseName;User Id=$username;Password=$password;TrustServerCertificate=True;"
    $connection = New-Object System.Data.SqlClient.SqlConnection($connectionString)
    $connection.Open()
    Write-Host "✓ Connection successful!" -ForegroundColor Green
    $connection.Close()
} catch {
    Write-Host "✗ Connection failed: $($_.Exception.Message)" -ForegroundColor Red
}
```

Run with: `powershell -ExecutionPolicy Bypass -File test-connection.ps1`

