#!/usr/bin/env bash
# UnifyHR - Cross-Service Role-Based Access Control (RBAC) Verification Script

set -euo pipefail

BASE_AUTH="http://localhost:8081"
BASE_EMP="http://localhost:8082"
BASE_ATT="http://localhost:8083"
BASE_PAY="http://localhost:8084"

echo "======================================================================="
echo "               UnifyHR - Cross-Service RBAC Verification               "
echo "======================================================================="

# Helper to register user
register_user() {
  local user=$1
  local pass=$2
  local role=$3
  curl -s -X POST "$BASE_AUTH/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$user\",\"password\":\"$pass\",\"role\":\"$role\"}" > /dev/null || true
}

# Helper to login and get token
get_token() {
  local user=$1
  local pass=$2
  curl -s -X POST "$BASE_AUTH/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$user\",\"password\":\"$pass\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/'
}

echo -e "\n[1/3] Ensuring test accounts exist..."
register_user "rbac_admin" "Password123!" "ADMIN"
register_user "rbac_hr" "Password123!" "HR"
register_user "rbac_emp" "Password123!" "EMPLOYEE"

echo -e "\n[2/3] Authenticating roles..."
ADMIN_TOKEN=$(get_token "rbac_admin" "Password123!")
HR_TOKEN=$(get_token "rbac_hr" "Password123!")
EMP_TOKEN=$(get_token "rbac_emp" "Password123!")

echo "  -> ADMIN token acquired"
echo "  -> HR token acquired"
echo "  -> EMPLOYEE token acquired"

echo -e "\n[3/3] Testing RBAC Permissions Across Services..."
echo "-----------------------------------------------------------------------"

TEST_COUNT=0
PASS_COUNT=0

test_endpoint() {
  local role=$1
  local service=$2
  local method=$3
  local url=$4
  local token=$5
  local data=$6
  local expected=$7

  TEST_COUNT=$((TEST_COUNT + 1))
  local status
  if [ -n "$data" ]; then
    status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "$data")
  else
    status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer $token")
  fi

  if [[ "$status" =~ ^($expected)$ ]]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "[PASS] Role: $role | Service: $service | $method $url | Status: $status (Expected: $expected)"
  else
    echo "[FAIL] Role: $role | Service: $service | $method $url | Status: $status (Expected: $expected)"
  fi
}

RAND=$RANDOM
# 1. Employee Service: POST /employees
test_endpoint "ADMIN" "employee-service" "POST" "$BASE_EMP/employees" "$ADMIN_TOKEN" "{\"name\":\"AdminEmp$RAND\",\"email\":\"adm$RAND@unifyhr.com\",\"department\":\"Tech\",\"designation\":\"Lead\",\"salary\":90000,\"status\":\"ACTIVE\"}" "200|201"
test_endpoint "HR" "employee-service" "POST" "$BASE_EMP/employees" "$HR_TOKEN" "{\"name\":\"HrEmp$RAND\",\"email\":\"hr$RAND@unifyhr.com\",\"department\":\"HR\",\"designation\":\"Recruiter\",\"salary\":60000,\"status\":\"ACTIVE\"}" "200|201"
test_endpoint "EMPLOYEE" "employee-service" "POST" "$BASE_EMP/employees" "$EMP_TOKEN" "{\"name\":\"EmpEmp$RAND\",\"email\":\"emp$RAND@unifyhr.com\",\"department\":\"Ops\",\"designation\":\"Associate\",\"salary\":50000,\"status\":\"ACTIVE\"}" "403"

# 2. Attendance Service: POST /attendance/mark
test_endpoint "ADMIN" "attendance-service" "POST" "$BASE_ATT/attendance/mark" "$ADMIN_TOKEN" "{\"employeeId\":1,\"status\":\"PRESENT\"}" "200|201"
test_endpoint "HR" "attendance-service" "POST" "$BASE_ATT/attendance/mark" "$HR_TOKEN" "{\"employeeId\":2,\"status\":\"PRESENT\"}" "200|201"
test_endpoint "EMPLOYEE" "attendance-service" "POST" "$BASE_ATT/attendance/mark" "$EMP_TOKEN" "{\"employeeId\":3,\"status\":\"PRESENT\"}" "403"

# 3. Payroll Service: POST /payroll/generate/{id}
test_endpoint "ADMIN" "payroll-service" "POST" "$BASE_PAY/payroll/generate/1" "$ADMIN_TOKEN" "" "200|201"
test_endpoint "HR" "payroll-service" "POST" "$BASE_PAY/payroll/generate/2" "$HR_TOKEN" "" "200|201"
test_endpoint "EMPLOYEE" "payroll-service" "POST" "$BASE_PAY/payroll/generate/3" "$EMP_TOKEN" "" "403"

# Read Operations
test_endpoint "EMPLOYEE" "employee-service" "GET" "$BASE_EMP/employees" "$EMP_TOKEN" "" "200"
test_endpoint "EMPLOYEE" "attendance-service" "GET" "$BASE_ATT/attendance/1" "$EMP_TOKEN" "" "200"
test_endpoint "EMPLOYEE" "payroll-service" "GET" "$BASE_PAY/payroll/1" "$EMP_TOKEN" "" "200"

echo "-----------------------------------------------------------------------"
echo "Verification Summary: $PASS_COUNT / $TEST_COUNT Tests Passed"
if [ "$PASS_COUNT" -eq "$TEST_COUNT" ]; then
  echo "RBAC ENFORCEMENT VERIFIED: SUCCESS"
else
  echo "RBAC ENFORCEMENT VERIFIED: FAILURE"
fi
