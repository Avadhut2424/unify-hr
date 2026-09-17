package com.unifyhr.attendance.client;

import com.unifyhr.attendance.dto.EmployeeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class EmployeeServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceClient.class);
    private final RestClient restClient;
    private final AuthServiceClient authServiceClient;

    public EmployeeServiceClient(@Value("${employee.service.url:http://employee-service:8082}") String employeeServiceUrl,
                                 AuthServiceClient authServiceClient) {
        this.restClient = RestClient.builder()
                .baseUrl(employeeServiceUrl)
                .build();
        this.authServiceClient = authServiceClient;
    }

    public List<EmployeeDTO> getAllEmployees(String token) {
        try {
            String authToken = (token != null && !token.isBlank()) ? token : authServiceClient.obtainSystemToken();
            if (authToken == null) {
                log.error("Could not obtain auth token to query employee-service");
                return Collections.emptyList();
            }

            return restClient.get()
                    .uri("/employees")
                    .header("Authorization", "Bearer " + authToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EmployeeDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch employees from employee-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
