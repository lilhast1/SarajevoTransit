package com.sarajevotransit.feedbackservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userservice", path = "/api/v1/users")
public interface UserClient {
    @GetMapping("/{userId}")
    Object getUserById(@PathVariable("userId") Long userId);
}