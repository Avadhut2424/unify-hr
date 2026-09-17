package com.unifyhr.payroll.client;

import com.unifyhr.payroll.dto.EmployeeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public EmployeeDTO getEmployeeById(Long employeeId, String token) {
        try {
            String authToken = (token != null && !token.isBlank()) ? token : authServiceClient.obtainSystemToken();
            if (authToken == null) {
                log.error("Unable to obtain token for employee-service request");
                return null;
            }

            return restClient.get()
                    .uri("/employees/{id}", employeeId)
                    .header("Authorization", "Bearer " + authToken)
                    .retrieve()
                    .body(EmployeeDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch employee {} from employee-service: {}", employeeId, e.getMessage());
            return null;
        }
    }
}
