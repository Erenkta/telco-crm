package com.bootcamp.order_service.client;

import com.bootcamp.order_service.config.FeignConfig;
import com.bootcamp.order_service.dto.response.CustomerDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name="customer-service",path = "/api/v1/customers",configuration = FeignConfig.class)
public interface CustomerClient {

    @GetMapping("/{id}")
    CustomerDetailResponse getCustomerById(@PathVariable("id") UUID id);
}
