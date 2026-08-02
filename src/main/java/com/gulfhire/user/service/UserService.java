package com.gulfhire.user.service;

import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User createUser(User user);
    Optional<User> getUserById(UUID id);
    Optional<User> getUserByEmail(String email);
    boolean existsByEmail(String email);
}
