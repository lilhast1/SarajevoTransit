package com.sarajevotransit.moneyman.client;

import com.sarajevotransit.moneyman.dto.CouponApplicationResponse;
import com.sarajevotransit.moneyman.dto.CouponApplyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoyaltyCouponClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.userservice.id:userservice}")
    private String userServiceId;

    public CouponApplicationResponse applyCoupon(Long userId, String couponCode, String rideCode) {
        ServiceInstance instance = loadBalancerClient.choose(userServiceId);
        if (instance == null) {
            throw new IllegalStateException("User service is unavailable");
        }

        String url = instance.getUri().toString() + "/api/v1/users/" + userId + "/loyalty/coupons/" + couponCode + "/apply";

        try {
            return restClientBuilder.build()
                    .post()
                    .uri(url)
                    .body(new CouponApplyRequest(rideCode))
                    .retrieve()
                    .body(CouponApplicationResponse.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Coupon could not be applied: " + e.getMessage(), e);
        }
    }
}