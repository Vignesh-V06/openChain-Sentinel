package openchain_sentinel_backend.dto;

public record LoginRequest(
        String identifier,
        String password
) {
}