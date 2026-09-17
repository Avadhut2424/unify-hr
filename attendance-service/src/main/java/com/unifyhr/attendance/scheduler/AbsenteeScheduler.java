package com.unifyhr.attendance.scheduler;

import com.unifyhr.attendance.client.EmployeeServiceClient;
import com.unifyhr.attendance.dto.EmployeeDTO;
import com.unifyhr.attendance.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AbsenteeScheduler {

    private static final Logger log = LoggerFactory.getLogger(AbsenteeScheduler.class);

    private final ExecutorService executorService;
    private final AttendanceService attendanceService;
    private final EmployeeServiceClient employeeServiceClient;

    public AbsenteeScheduler(ExecutorService attendanceExecutorService,
                             AttendanceService attendanceService,
                             EmployeeServiceClient employeeServiceClient) {
        this.executorService = attendanceExecutorService;
        this.attendanceService = attendanceService;
        this.employeeServiceClient = employeeServiceClient;
    }

    /**
     * Runs automatically shortly after startup (10s) and daily thereafter.
     */
    @Scheduled(initialDelay = 10000, fixedRate = 86400000)
    public void runScheduledAbsenteeJob() {
        log.info("[AbsenteeScheduler] Scheduled trigger fired. Initiating background absentee processing...");
        processAbsenteesAsync(null);
    }

    /**
     * Asynchronously checks and marks absentees using the fixed thread pool ExecutorService.
     * This method submits tasks to worker threads and does not block the caller or incoming REST requests.
     */
    public CompletableFuture<Integer> processAbsenteesAsync(String authToken) {
        LocalDate today = LocalDate.now();
        log.info("[AbsenteeScheduler] Starting multithreaded absentee processing for date: {}", today);

        return CompletableFuture.supplyAsync(() -> {
            List<EmployeeDTO> employees = employeeServiceClient.getAllEmployees(authToken);
            if (employees.isEmpty()) {
                log.warn("[AbsenteeScheduler] No employees retrieved from employee-service to check for absentees.");
                return 0;
            }

            log.info("[AbsenteeScheduler] Evaluating {} employees across worker thread pool...", employees.size());
            AtomicInteger absenteesMarked = new AtomicInteger(0);

            // Dispatch concurrently to the fixed thread pool
            List<CompletableFuture<Void>> futures = employees.stream()
                    .map(emp -> CompletableFuture.runAsync(() -> {
                        try {
                            if (!attendanceService.hasRecordForDate(emp.getId(), today)) {
                                attendanceService.markAbsentee(emp.getId(), today);
                                absenteesMarked.incrementAndGet();
                                log.info("[AbsenteeScheduler] Thread [{}] Auto-marked employee ID={} ({}) as ABSENT for {}",
                                        Thread.currentThread().getName(), emp.getId(), emp.getName(), today);
                            } else {
                                log.debug("[AbsenteeScheduler] Thread [{}] Employee ID={} already has attendance for {}",
                                        Thread.currentThread().getName(), emp.getId(), today);
                            }
                        } catch (Exception e) {
                            log.error("[AbsenteeScheduler] Error processing attendance for employee {}: {}",
                                    emp.getId(), e.getMessage());
                        }
                    }, executorService))
                    .toList();

            // Wait for all worker threads to complete the batch
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("[AbsenteeScheduler] Completed absentee processing for {}. Total marked ABSENT: {}",
                    today, absenteesMarked.get());

            return absenteesMarked.get();
        }, executorService);
    }
}
