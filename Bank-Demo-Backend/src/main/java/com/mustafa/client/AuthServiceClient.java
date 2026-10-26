package com.mustafa.client;

import com.mustafa.client.config.AuthServiceClientConfig;
import com.mustafa.dto.response.CustomerAccessContextResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/api/v1/internal/users",
        configuration = AuthServiceClientConfig.class)
public interface AuthServiceClient {
    @GetMapping("/access-context/{identityNumber}")
    CustomerAccessContextResponse getAccessContext(@PathVariable("identityNumber") String identityNumber);
}
