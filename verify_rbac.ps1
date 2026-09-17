# UnifyHR - Cross-Service Role-Based Access Control (RBAC) Verification Script

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "               UnifyHR - Cross-Service RBAC Verification               " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

# Step 1: Ensure test users exist
Write-Host "`n[1/3] Ensuring test accounts exist in Auth Service..." -ForegroundColor Yellow
$users = @(
    @{ username = "rbac_admin"; password = "Password123!"; role = "ADMIN" },
    @{ username = "rbac_hr"; password = "Password123!"; role = "HR" },
    @{ username = "rbac_emp"; password = "Password123!"; role = "EMPLOYEE" }
)

foreach ($u in $users) {
    try {
        $regBody = @{ username = $u.username; password = $u.password; role = $u.role } | ConvertTo-Json
        $null = Invoke-RestMethod -Uri "http://localhost:8081/auth/register" -Method Post -Body $regBody -ContentType "application/json" -ErrorAction SilentlyContinue
    } catch {
        # Already exists, proceed
    }
}

# Step 2: Obtain JWT tokens for each role
Write-Host "`n[2/3] Authenticating as ADMIN, HR, and EMPLOYEE..." -ForegroundColor Yellow
$tokens = @{}
foreach ($u in $users) {
    $loginBody = @{ username = $u.username; password = $u.password } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $tokens[$u.role] = $res.token
    Write-Host "  -> Logged in as $($u.role) ($($u.username)): Token acquired" -ForegroundColor Green
}

# Step 3: Run RBAC test matrix
Write-Host "`n[3/3] Testing RBAC Permissions Across Services..." -ForegroundColor Yellow
Write-Host "-----------------------------------------------------------------------"

$testCounter = 0
$passCounter = 0

function Test-Endpoint {
    param (
        [string]$Role,
        [string]$ServiceName,
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [int[]]$ExpectedStatuses
    )

    $script:testCounter++
    $token = $tokens[$Role]
    $headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }

    $actualStatus = 0
    try {
        if ($Body) {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -Body $Body -UseBasicParsing -ErrorAction Stop
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -UseBasicParsing -ErrorAction Stop
        }
        $actualStatus = [int]$resp.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $actualStatus = [int]$_.Exception.Response.StatusCode
        } else {
            $actualStatus = 500
        }
    }

    $passed = $ExpectedStatuses -contains $actualStatus
    if ($passed) {
        $script:passCounter++
        $statusLabel = "PASS"
        Write-Host "[$statusLabel] Role: $Role | Service: $ServiceName | $Method $Url | Status: $actualStatus (Expected: $($ExpectedStatuses -join '/'))" -ForegroundColor Green
    } else {
        $statusLabel = "FAIL"
        Write-Host "[$statusLabel] Role: $Role | Service: $ServiceName | $Method $Url | Status: $actualStatus (Expected: $($ExpectedStatuses -join '/'))" -ForegroundColor Red
    }
}

$uniqueId = Get-Random -Minimum 1000 -Maximum 9999

# Test Matrix:
# 1. employee-service: POST /employees (Restricted to ADMIN, HR)
$empPayloadAdmin = @{ name = "Test Admin Emp $uniqueId"; email = "emp_admin_$uniqueId@unifyhr.com"; department = "Engineering"; designation = "Dev"; salary = 70000; status = "ACTIVE" } | ConvertTo-Json
$empPayloadHr = @{ name = "Test HR Emp $uniqueId"; email = "emp_hr_$uniqueId@unifyhr.com"; department = "HR"; designation = "Specialist"; salary = 65000; status = "ACTIVE" } | ConvertTo-Json
$empPayloadEmp = @{ name = "Test Fail Emp $uniqueId"; email = "emp_emp_$uniqueId@unifyhr.com"; department = "Finance"; designation = "Analyst"; salary = 60000; status = "ACTIVE" } | ConvertTo-Json

Test-Endpoint -Role "ADMIN"    -ServiceName "employee-service"   -Method "POST" -Url "http://localhost:8082/employees" -Body $empPayloadAdmin -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "HR"       -ServiceName "employee-service"   -Method "POST" -Url "http://localhost:8082/employees" -Body $empPayloadHr    -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "EMPLOYEE" -ServiceName "employee-service"   -Method "POST" -Url "http://localhost:8082/employees" -Body $empPayloadEmp   -ExpectedStatuses @(403)

# 2. attendance-service: POST /attendance/mark (Restricted to ADMIN, HR)
$attPayloadAdmin = @{ employeeId = 1; date = (Get-Date).ToString("yyyy-MM-dd"); status = "PRESENT" } | ConvertTo-Json
$attPayloadHr    = @{ employeeId = 2; date = (Get-Date).ToString("yyyy-MM-dd"); status = "PRESENT" } | ConvertTo-Json
$attPayloadEmp   = @{ employeeId = 3; date = (Get-Date).ToString("yyyy-MM-dd"); status = "PRESENT" } | ConvertTo-Json

Test-Endpoint -Role "ADMIN"    -ServiceName "attendance-service" -Method "POST" -Url "http://localhost:8083/attendance/mark" -Body $attPayloadAdmin -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "HR"       -ServiceName "attendance-service" -Method "POST" -Url "http://localhost:8083/attendance/mark" -Body $attPayloadHr    -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "EMPLOYEE" -ServiceName "attendance-service" -Method "POST" -Url "http://localhost:8083/attendance/mark" -Body $attPayloadEmp   -ExpectedStatuses @(403)

# 3. payroll-service: POST /payroll/generate/{id} (Restricted to ADMIN, HR)
Test-Endpoint -Role "ADMIN"    -ServiceName "payroll-service"    -Method "POST" -Url "http://localhost:8084/payroll/generate/1" -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "HR"       -ServiceName "payroll-service"    -Method "POST" -Url "http://localhost:8084/payroll/generate/2" -ExpectedStatuses @(200, 201)
Test-Endpoint -Role "EMPLOYEE" -ServiceName "payroll-service"    -Method "POST" -Url "http://localhost:8084/payroll/generate/3" -ExpectedStatuses @(403)

# Read operations check: All authenticated roles can read
Test-Endpoint -Role "EMPLOYEE" -ServiceName "employee-service"   -Method "GET"  -Url "http://localhost:8082/employees" -ExpectedStatuses @(200)
Test-Endpoint -Role "EMPLOYEE" -ServiceName "attendance-service" -Method "GET"  -Url "http://localhost:8083/attendance/1" -ExpectedStatuses @(200)
Test-Endpoint -Role "EMPLOYEE" -ServiceName "payroll-service"    -Method "GET"  -Url "http://localhost:8084/payroll/1" -ExpectedStatuses @(200)

Write-Host "-----------------------------------------------------------------------"
Write-Host "Verification Summary: $passCounter / $testCounter Tests Passed" -ForegroundColor Cyan
if ($passCounter -eq $testCounter) {
    Write-Host "RBAC ENFORCEMENT VERIFIED: SUCCESS" -ForegroundColor Green
} else {
    Write-Host "RBAC ENFORCEMENT VERIFIED: FAILURE" -ForegroundColor Red
}
