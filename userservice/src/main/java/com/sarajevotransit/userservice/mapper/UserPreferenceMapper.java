package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.UserPreferenceResponse;
import com.sarajevotransit.userservice.model.UserPreference;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceMapper {

    private final ModelMapper modelMapper;

    public UserPreferenceMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserPreferenceResponse toResponse(UserPreference preference) {
        if (preference == null) {
            return null;
        }

        return modelMapper.map(preference, UserPreferenceResponse.class);
    }
}
