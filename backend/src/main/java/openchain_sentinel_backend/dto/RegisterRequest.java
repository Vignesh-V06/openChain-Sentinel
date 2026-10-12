package openchain_sentinel_backend.dto;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String displayName
) {
}