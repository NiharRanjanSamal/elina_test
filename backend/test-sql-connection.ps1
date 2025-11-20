# SQL Server Connection Test Script
# This script tests if SQL Server is accessible with the configured credentials

$serverName = "LAPTOP-R5A7J452\SQLEXPRESS"
$databaseName = "elina"
$username = "elina"
$password = "elina123"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SQL Server Connection Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Server: $serverName" -ForegroundColor Yellow
Write-Host "Database: $databaseName" -ForegroundColor Yellow
Write-Host "Username: $username" -ForegroundColor Yellow
Write-Host ""

# Check if SQL Server service is running
Write-Host "1. Checking SQL Server services..." -ForegroundColor Cyan
$sqlService = Get-Service | Where-Object { $_.DisplayName -like "*SQL Server (SQLEXPRESS)*" -or $_.Name -like "*MSSQL*SQLEXPRESS*" }
$browserService = Get-Service | Where-Object { $_.DisplayName -like "*SQL Server Browser*" }

if ($sqlService) {
    if ($sqlService.Status -eq "Running") {
        Write-Host "   ✓ SQL Server (SQLEXPRESS) is running" -ForegroundColor Green
    } else {
        Write-Host "   ✗ SQL Server (SQLEXPRESS) is NOT running (Status: $($sqlService.Status))" -ForegroundColor Red
        Write-Host "     Start it from Services (services.msc)" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ⚠ SQL Server (SQLEXPRESS) service not found" -ForegroundColor Yellow
}

if ($browserService) {
    if ($browserService.Status -eq "Running") {
        Write-Host "   ✓ SQL Server Browser is running" -ForegroundColor Green
    } else {
        Write-Host "   ✗ SQL Server Browser is NOT running (Status: $($browserService.Status))" -ForegroundColor Red
        Write-Host "     This is REQUIRED for named instances. Start it from Services." -ForegroundColor Yellow
    }
} else {
    Write-Host "   ⚠ SQL Server Browser service not found" -ForegroundColor Yellow
}

Write-Host ""

# Test network connectivity
Write-Host "2. Testing network connectivity..." -ForegroundColor Cyan
$hostname = $serverName.Split('\')[0]
try {
    $ping = Test-Connection -ComputerName $hostname -Count 1 -Quiet
    if ($ping) {
        Write-Host "   ✓ Host $hostname is reachable" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Host $hostname is NOT reachable" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Network test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test SQL Server connection
Write-Host "3. Testing SQL Server connection..." -ForegroundColor Cyan
try {
    # Load SQL Server assembly
    Add-Type -AssemblyName System.Data
    
    $connectionString = "Server=$serverName;Database=$databaseName;User Id=$username;Password=$password;TrustServerCertificate=True;Connection Timeout=10;"
    $connection = New-Object System.Data.SqlClient.SqlConnection($connectionString)
    
    Write-Host "   Attempting to connect..." -ForegroundColor Yellow
    $connection.Open()
    
    Write-Host "   ✓ Connection successful!" -ForegroundColor Green
    
    # Get server version
    $command = $connection.CreateCommand()
    $command.CommandText = "SELECT @@VERSION AS Version, DB_NAME() AS CurrentDatabase"
    $reader = $command.ExecuteReader()
    
    if ($reader.Read()) {
        Write-Host "   Current Database: $($reader['CurrentDatabase'])" -ForegroundColor Green
        $version = $reader['Version'].ToString().Split("`n")[0]
        Write-Host "   SQL Server Version: $version" -ForegroundColor Green
    }
    
    $reader.Close()
    $connection.Close()
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "✓ All checks passed! SQL Server is ready." -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    
} catch {
    Write-Host "   ✗ Connection failed!" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Troubleshooting steps:" -ForegroundColor Yellow
    Write-Host "   1. Verify SQL Server (SQLEXPRESS) service is running" -ForegroundColor Yellow
    Write-Host "   2. Verify SQL Server Browser service is running" -ForegroundColor Yellow
    Write-Host "   3. Check if SQL Server Authentication is enabled" -ForegroundColor Yellow
    Write-Host "   4. Verify username/password are correct" -ForegroundColor Yellow
    Write-Host "   5. Check Windows Firewall settings" -ForegroundColor Yellow
    Write-Host "   6. Verify the database 'elina' exists" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "✗ Connection test failed" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    exit 1
}

