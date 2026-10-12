package openchain_sentinel_backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}