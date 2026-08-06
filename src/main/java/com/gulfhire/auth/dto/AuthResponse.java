package com.gulfhire.auth.dto;

import com.gulfhire.common.constants.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID id;
    private String token;
    private Role role;
    private String email;
    private String fullName;
}
