#!/usr/bin/env python3
"""
UnifyHR - Phase 10 Load & Uptime Sanity Test Script
Seeds ~1000 employee records via employee-service API, then fires concurrent
GET and POST requests across all 4 microservices (Auth, Employee, Attendance, Payroll)
while recording response times, status codes, throughput, and error rates.
"""

import sys
import time
import random
import string
import argparse
from typing import List, Dict, Any
from concurrent.futures import ThreadPoolExecutor, as_completed
import statistics
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# Default Service URLs
DEFAULT_AUTH_URL = "http://localhost:8081"
DEFAULT_EMP_URL = "http://localhost:8082"
DEFAULT_ATT_URL = "http://localhost:8083"
DEFAULT_PAY_URL = "http://localhost:8084"

def get_http_session(pool_connections: int = 50, pool_maxsize: int = 100) -> requests.Session:
    session = requests.Session()
    retry_strategy = Retry(
        total=2,
        backoff_factor=0.2,
        status_forcelist=[502, 503, 504],
        raise_on_status=False
    )
    adapter = HTTPAdapter(
        pool_connections=pool_connections,
        pool_maxsize=pool_maxsize,
        max_retries=retry_strategy
    )
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session

def random_string(length: int = 8) -> str:
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))

def obtain_jwt_tokens(auth_url: str, session: requests.Session) -> Dict[str, str]:
    tokens = {}
    roles = [
        ("load_admin", "Password123!", "ADMIN"),
        ("load_hr", "Password123!", "HR"),
        ("load_emp", "Password123!", "EMPLOYEE")
    ]
    
    print("[1/3] Setting up test credentials and acquiring JWT tokens...")
    for username, password, role in roles:
        try:
            session.post(
                f"{auth_url}/auth/register",
                json={"username": username, "password": password, "role": role},
                timeout=5
            )
        except Exception:
            pass
        
        resp = session.post(
            f"{auth_url}/auth/login",
            json={"username": username, "password": password},
            timeout=5
        )
        if resp.status_code == 200:
            token = resp.json().get("token")
            tokens[role] = token
            print(f"  -> Acquired JWT token for role: {role}")
        else:
            raise RuntimeError(f"Failed to authenticate user {username}: {resp.status_code} {resp.text}")
            
    return tokens

def seed_single_employee(i: int, emp_url: str, token: str, session: requests.Session) -> int:
    payload = {
        "name": f"Employee_{i:04d}_{random_string(4)}",
        "email": f"emp_{i:04d}_{random_string(4)}@unifyhr.com",
        "department": random.choice(["Engineering", "Human Resources", "Finance", "Operations", "Product"]),
        "designation": random.choice(["Software Engineer", "Senior Engineer", "HR Manager", "Financial Analyst", "QA Lead"]),
        "salary": random.randint(45000, 150000),
        "status": "ACTIVE"
    }
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    resp = session.post(f"{emp_url}/employees", json=payload, headers=headers, timeout=10)
    if resp.status_code in (200, 201):
        data = resp.json()
        return data.get("id", i)
    else:
        return -1

def seed_employees(emp_url: str, token: str, count: int, concurrency: int = 20) -> List[int]:
    print(f"\n[2/3] Seeding {count} employee records via Employee Service API...")
    start_time = time.time()
    seeded_ids = []
    failed_count = 0
    
    session = get_http_session(pool_connections=concurrency, pool_maxsize=concurrency * 2)
    
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(seed_single_employee, i, emp_url, token, session) for i in range(1, count + 1)]
        completed = 0
        for future in as_completed(futures):
            emp_id = future.result()
            if emp_id > 0:
                seeded_ids.append(emp_id)
            else:
                failed_count += 1
            completed += 1
            if completed % 200 == 0 or completed == count:
                elapsed = time.time() - start_time
                rate = completed / elapsed if elapsed > 0 else 0
                print(f"  -> Seeded {completed}/{count} records ({rate:.1f} req/s)...")
                
    elapsed_total = time.time() - start_time
    print(f"  -> Seeding complete in {elapsed_total:.2f}s: {len(seeded_ids)} succeeded, {failed_count} failed.")
    return seeded_ids

class MetricsTracker:
    def __init__(self):
        self.latencies: List[float] = []
        self.errors = 0
        self.successes = 0
        self.service_stats: Dict[str, Dict[str, Any]] = {
            "Auth Service": {"count": 0, "success": 0, "error": 0, "latencies": []},
            "Employee Service": {"count": 0, "success": 0, "error": 0, "latencies": []},
            "Attendance Service": {"count": 0, "success": 0, "error": 0, "latencies": []},
            "Payroll Service": {"count": 0, "success": 0, "error": 0, "latencies": []}
        }

    def record(self, service: str, latency_ms: float, success: bool):
        self.latencies.append(latency_ms)
        if success:
            self.successes += 1
        else:
            self.errors += 1
            
        stats = self.service_stats[service]
        stats["count"] += 1
        if success:
            stats["success"] += 1
        else:
            stats["error"] += 1
        stats["latencies"].append(latency_ms)

def run_worker_task(
    service_urls: Dict[str, str],
    tokens: Dict[str, str],
    seeded_ids: List[int],
    session: requests.Session
) -> tuple:
    scenario = random.choice([
        ("Auth Service", "POST_LOGIN"),
        ("Auth Service", "GET_VALIDATE"),
        ("Employee Service", "GET_EMPLOYEES"),
        ("Employee Service", "GET_EMPLOYEE_ID"),
        ("Employee Service", "POST_EMPLOYEE"),
        ("Attendance Service", "POST_MARK"),
        ("Attendance Service", "GET_ATTENDANCE"),
        ("Payroll Service", "POST_GENERATE"),
        ("Payroll Service", "GET_PAYROLL")
    ])
    
    service_name, action = scenario
    t0 = time.perf_counter()
    success = False
    
    admin_token = tokens["ADMIN"]
    hr_token = tokens["HR"]
    emp_token = tokens["EMPLOYEE"]
    random_emp_id = random.choice(seeded_ids) if seeded_ids else 1

    try:
        if action == "POST_LOGIN":
            resp = session.post(
                f"{service_urls['auth']}/auth/login",
                json={"username": "load_admin", "password": "Password123!"},
                timeout=5
            )
            success = resp.status_code == 200
        elif action == "GET_VALIDATE":
            resp = session.get(
                f"{service_urls['auth']}/auth/validate",
                headers={"Authorization": f"Bearer {admin_token}"},
                timeout=5
            )
            success = resp.status_code == 200
        elif action == "GET_EMPLOYEES":
            resp = session.get(
                f"{service_urls['emp']}/employees",
                headers={"Authorization": f"Bearer {emp_token}"},
                timeout=5
            )
            success = resp.status_code == 200
        elif action == "GET_EMPLOYEE_ID":
            resp = session.get(
                f"{service_urls['emp']}/employees/{random_emp_id}",
                headers={"Authorization": f"Bearer {emp_token}"},
                timeout=5
            )
            success = resp.status_code == 200
        elif action == "POST_EMPLOYEE":
            resp = session.post(
                f"{service_urls['emp']}/employees",
                json={
                    "name": f"Dynamic_{random_string(5)}",
                    "email": f"dyn_{random_string(6)}@unifyhr.com",
                    "department": "Engineering",
                    "designation": "Staff Engineer",
                    "salary": 110000,
                    "status": "ACTIVE"
                },
                headers={"Authorization": f"Bearer {hr_token}", "Content-Type": "application/json"},
                timeout=5
            )
            success = resp.status_code in (200, 201)
        elif action == "POST_MARK":
            resp = session.post(
                f"{service_urls['att']}/attendance/mark",
                json={
                    "employeeId": random_emp_id,
                    "status": random.choice(["PRESENT", "ABSENT"])
                },
                headers={"Authorization": f"Bearer {hr_token}", "Content-Type": "application/json"},
                timeout=5
            )
            success = resp.status_code in (200, 201)
        elif action == "GET_ATTENDANCE":
            resp = session.get(
                f"{service_urls['att']}/attendance/{random_emp_id}",
                headers={"Authorization": f"Bearer {emp_token}"},
                timeout=5
            )
            success = resp.status_code == 200
        elif action == "POST_GENERATE":
            resp = session.post(
                f"{service_urls['pay']}/payroll/generate/{random_emp_id}",
                headers={"Authorization": f"Bearer {admin_token}"},
                timeout=5
            )
            success = resp.status_code in (200, 201)
        elif action == "GET_PAYROLL":
            resp = session.get(
                f"{service_urls['pay']}/payroll/{random_emp_id}",
                headers={"Authorization": f"Bearer {emp_token}"},
                timeout=5
            )
            success = resp.status_code == 200
    except Exception:
        success = False

    latency_ms = (time.perf_counter() - t0) * 1000.0
    return service_name, latency_ms, success

def run_load_test(
    service_urls: Dict[str, str],
    tokens: Dict[str, str],
    seeded_ids: List[int],
    duration_secs: int,
    concurrency: int
) -> MetricsTracker:
    print(f"\n[3/3] Firing concurrent requests across all 4 services...")
    print(f"  -> Concurrency: {concurrency} worker threads")
    print(f"  -> Duration:    {duration_secs} seconds")
    print("  -> Traffic mix: Auth, Employee, Attendance, Payroll (Reads & Writes)")
    
    metrics = MetricsTracker()
    session = get_http_session(pool_connections=concurrency, pool_maxsize=concurrency * 2)
    end_time = time.time() + duration_secs
    
    start_time = time.time()
    last_report_time = start_time
    
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        active_futures = set()
        
        for _ in range(concurrency):
            f = executor.submit(run_worker_task, service_urls, tokens, seeded_ids, session)
            active_futures.add(f)
            
        while active_futures:
            done = [f for f in active_futures if f.done()]
            if not done:
                time.sleep(0.01)
                continue
                
            for f in done:
                active_futures.remove(f)
                try:
                    svc, lat, succ = f.result()
                    metrics.record(svc, lat, succ)
                except Exception:
                    metrics.errors += 1
                
                if time.time() < end_time:
                    new_f = executor.submit(run_worker_task, service_urls, tokens, seeded_ids, session)
                    active_futures.add(new_f)
            
            now = time.time()
            if now - last_report_time >= 15:
                elapsed = now - start_time
                remaining = max(0, int(end_time - now))
                total_reqs = metrics.successes + metrics.errors
                current_rate = total_reqs / elapsed if elapsed > 0 else 0
                err_rate = (metrics.errors / total_reqs * 100.0) if total_reqs > 0 else 0
                avg_lat = statistics.mean(metrics.latencies) if metrics.latencies else 0
                print(f"  [{int(elapsed)}s elapsed | {remaining}s remaining] Reqs: {total_reqs} | Rate: {current_rate:.1f} req/s | Errors: {metrics.errors} ({err_rate:.2f}%) | Avg Latency: {avg_lat:.2f}ms")
                last_report_time = now

    return metrics

def percentile(data: List[float], p: float) -> float:
    if not data:
        return 0.0
    sorted_data = sorted(data)
    idx = int(len(sorted_data) * p / 100.0)
    idx = min(idx, len(sorted_data) - 1)
    return sorted_data[idx]

def print_summary(metrics: MetricsTracker, total_duration: float, seed_count: int):
    total_reqs = metrics.successes + metrics.errors
    err_rate = (metrics.errors / total_reqs * 100.0) if total_reqs > 0 else 0.0
    uptime = ((metrics.successes / total_reqs) * 100.0) if total_reqs > 0 else 100.0
    throughput = total_reqs / total_duration if total_duration > 0 else 0.0

    avg_lat = statistics.mean(metrics.latencies) if metrics.latencies else 0.0
    min_lat = min(metrics.latencies) if metrics.latencies else 0.0
    max_lat = max(metrics.latencies) if metrics.latencies else 0.0
    p50_lat = percentile(metrics.latencies, 50)
    p90_lat = percentile(metrics.latencies, 90)
    p95_lat = percentile(metrics.latencies, 95)
    p99_lat = percentile(metrics.latencies, 99)

    print("\n" + "=" * 76)
    print("           UNIFYHR LOAD & UPTIME SANITY TEST - SUMMARY REPORT            ")
    print("=" * 76)
    print(f"  Test Duration:              {total_duration:.2f} seconds")
    print(f"  Total Employees Seeded:     {seed_count}")
    print(f"  Total Requests Fired:       {total_reqs}")
    print(f"  Successful Requests:        {metrics.successes}")
    print(f"  Failed Requests:            {metrics.errors}")
    print(f"  Error Rate:                 {err_rate:.3f}%")
    print(f"  Observed Uptime / Avail.:   {uptime:.3f}%  (Target: >= 99.900%)")
    print(f"  Throughput:                 {throughput:.1f} requests/sec")
    print("-" * 76)
    print("LATENCY DISTRIBUTION:")
    print(f"  Min Latency:                {min_lat:.2f} ms")
    print(f"  Average (Mean) Latency:     {avg_lat:.2f} ms")
    print(f"  50th Percentile (Median):   {p50_lat:.2f} ms")
    print(f"  90th Percentile (p90):      {p90_lat:.2f} ms")
    print(f"  95th Percentile (p95):      {p95_lat:.2f} ms")
    print(f"  99th Percentile (p99):      {p99_lat:.2f} ms")
    print(f"  Max Latency:                {max_lat:.2f} ms")
    print("-" * 76)
    print("PER-SERVICE BREAKDOWN:")
    header = f"  {'Service Name':<20} | {'Requests':<8} | {'Success':<8} | {'Error %':<8} | {'Avg (ms)':<8} | {'p95 (ms)':<8}"
    print(header)
    print("  " + "-" * 72)
    for svc, stats in metrics.service_stats.items():
        cnt = stats["count"]
        succ = stats["success"]
        errs = stats["error"]
        svc_err_rate = (errs / cnt * 100.0) if cnt > 0 else 0.0
        svc_avg = statistics.mean(stats["latencies"]) if stats["latencies"] else 0.0
        svc_p95 = percentile(stats["latencies"], 95)
        print(f"  {svc:<20} | {cnt:<8} | {succ:<8} | {svc_err_rate:<7.2f}% | {svc_avg:<8.2f} | {svc_p95:<8.2f}")
    print("=" * 76)

def main():
    parser = argparse.ArgumentParser(description="UnifyHR Load & Uptime Sanity Test")
    parser.add_argument("--seed-count", type=int, default=1000, help="Number of employee records to seed")
    parser.add_argument("--duration", type=int, default=120, help="Test duration in seconds (default: 120s)")
    parser.add_argument("--concurrency", type=int, default=25, help="Number of concurrent worker threads")
    parser.add_argument("--auth-url", default=DEFAULT_AUTH_URL, help="Auth service URL")
    parser.add_argument("--emp-url", default=DEFAULT_EMP_URL, help="Employee service URL")
    parser.add_argument("--att-url", default=DEFAULT_ATT_URL, help="Attendance service URL")
    parser.add_argument("--pay-url", default=DEFAULT_PAY_URL, help="Payroll service URL")
    args = parser.parse_args()

    service_urls = {
        "auth": args.auth_url,
        "emp": args.emp_url,
        "att": args.att_url,
        "pay": args.pay_url
    }

    session = get_http_session()
    
    tokens = obtain_jwt_tokens(args.auth_url, session)

    seeded_ids = seed_employees(args.emp_url, tokens["ADMIN"], args.seed_count, concurrency=20)
    if not seeded_ids:
        print("[ERROR] Failed to seed any employee records. Aborting.")
        sys.exit(1)

    start_load_time = time.time()
    metrics = run_load_test(
        service_urls=service_urls,
        tokens=tokens,
        seeded_ids=seeded_ids,
        duration_secs=args.duration,
        concurrency=args.concurrency
    )
    total_load_duration = time.time() - start_load_time

    print_summary(metrics, total_load_duration, len(seeded_ids))

if __name__ == "__main__":
    main()
