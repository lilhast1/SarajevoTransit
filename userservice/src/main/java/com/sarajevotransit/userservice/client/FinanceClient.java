package com.sarajevotransit.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "moneyman")
public interface FinanceClient {

    @PostMapping("/api/finance/purchase")
    Object purchaseTicket(@RequestBody Map<String, Object> request);
}