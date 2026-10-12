
package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.dto.AuthResponse;
import openchain_sentinel_backend.dto.LoginRequest;
import openchain_sentinel_backend.dto.RegisterRequest;
import openchain_sentinel_backend.dto.UserResponse;
import openchain_sentinel_backend.exception.AuthException;
import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.repository.UserRepository;
import openchain_sentinel_backend.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(
            AuthService authService,
            UserRepository userRepository
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request
    ) {
        try {
            AuthResponse response = authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (AuthException exception) {
            return ResponseEntity
                    .status(exception.getStatus())
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (AuthException exception) {
            return ResponseEntity
                    .status(exception.getStatus())
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Authentication required."
                    ));
        }

        try {
            // The JWT filter uses the username as the principal.
            String username = authentication.getName();

            // Resolve the actual MongoDB user first.
            Optional<User> userOptional =
                    userRepository.findByUsernameIgnoreCase(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error",
                                "User account no longer exists."
                        ));
            }

            // Pass the real MongoDB ID to AuthService.
            User user = userOptional.get();

            UserResponse response =
                    authService.getCurrentUser(user.getId());

            return ResponseEntity.ok(response);

        } catch (AuthException exception) {
            return ResponseEntity
                    .status(exception.getStatus())
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
