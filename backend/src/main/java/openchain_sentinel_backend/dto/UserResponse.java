package openchain_sentinel_backend.dto;

import openchain_sentinel_backend.model.User;

import java.time.Instant;

public record UserResponse(
        String id,
        String username,
        String email,
        String displayName,
        String provider,
        String role,
        Instant createdAt,
        Instant lastLoginAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProvider(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}