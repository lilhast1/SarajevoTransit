package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    private final ModelMapper modelMapper;

    public UserProfileResponse toResponse(UserProfile user) {
        if (user == null) {
            return null;
        }

        return modelMapper.map(user, UserProfileResponse.class);
    }
}
