package com.unifyhr.employee.client;

import com.unifyhr.employee.dto.ValidateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private final RestClient restClient;

    public AuthServiceClient(@Value("${auth.service.url:http://auth-service:8081}") String authServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    public ValidateResponse validateToken(String token) {
        try {
            return restClient.get()
                    .uri("/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Auth service returned error status: {}", response.getStatusCode());
                    })
                    .body(ValidateResponse.class);
        } catch (Exception e) {
            log.error("Failed to validate token with auth-service: {}", e.getMessage());
            return null;
        }
    }
}
