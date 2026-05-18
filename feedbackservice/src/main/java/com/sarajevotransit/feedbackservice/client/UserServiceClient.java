package com.sarajevotransit.feedbackservice.client;

import com.sarajevotransit.feedbackservice.client.dto.UserProfileSnapshot;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.users.id:userservice}")
    private String usersServiceId;

    public UserProfileSnapshot validateUser(Long userId) {
        return getUser(userId);
    }

    public UserProfileSnapshot getUser(Long userId) {
        ServiceInstance instance = loadBalancerClient.choose(usersServiceId);
        if (instance == null) {
            throw new ServiceUnavailableException("User service is unavailable.");
        }

        String url = instance.getUri() + "/api/v1/users/" + userId;
        try {
            UserProfileSnapshot response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, clientResponse) -> {
                                throw new NotFoundException("User with id " + userId + " not found.");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, clientResponse) -> {
                                throw new ServiceUnavailableException("User service is unavailable.");
                            })
                    .body(UserProfileSnapshot.class);
            if (response == null) {
                throw new ServiceUnavailableException("User service returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("User service is unavailable.");
        }
    }
}
