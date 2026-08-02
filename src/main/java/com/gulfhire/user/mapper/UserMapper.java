package com.gulfhire.user.mapper;

import com.gulfhire.user.dto.UserRequest;
import com.gulfhire.user.dto.UserResponse;
import com.gulfhire.user.entity.User;

public interface UserMapper {
    UserResponse toUserResponse(User user);
    User toUser(UserRequest userRequest);
}
