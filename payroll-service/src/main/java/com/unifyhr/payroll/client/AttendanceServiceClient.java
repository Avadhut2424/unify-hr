package com.unifyhr.payroll.client;

import com.unifyhr.payroll.dto.AttendanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class AttendanceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AttendanceServiceClient.class);
    private final RestClient restClient;
    private final AuthServiceClient authServiceClient;

    public AttendanceServiceClient(@Value("${attendance.service.url:http://attendance-service:8083}") String attendanceServiceUrl,
                                   AuthServiceClient authServiceClient) {
        this.restClient = RestClient.builder()
                .baseUrl(attendanceServiceUrl)
                .build();
        this.authServiceClient = authServiceClient;
    }

    public List<AttendanceDTO> getAttendanceByEmployee(Long employeeId, String token) {
        try {
            String authToken = (token != null && !token.isBlank()) ? token : authServiceClient.obtainSystemToken();
            if (authToken == null) {
                log.error("Unable to obtain token for attendance-service request");
                return Collections.emptyList();
            }

            return restClient.get()
                    .uri("/attendance/{employeeId}", employeeId)
                    .header("Authorization", "Bearer " + authToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AttendanceDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch attendance for employee {}: {}", employeeId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
