package tn.esprit.spring.userservice.mapper;

import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.entity.User;

public class UserMapper {
    public static UserResponse toResponse(User user) {
        return UserResponse.from(user);
    }
}