package openchain_sentinel_backend.service;

import openchain_sentinel_backend.dto.AuthResponse;
import openchain_sentinel_backend.dto.LoginRequest;
import openchain_sentinel_backend.dto.RegisterRequest;
import openchain_sentinel_backend.dto.UserResponse;
import openchain_sentinel_backend.exception.AuthException;
import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.repository.UserRepository;
import openchain_sentinel_backend.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService
    ) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;

        this.passwordEncoder =
                new BCryptPasswordEncoder();
    }

    public AuthResponse register(RegisterRequest request) {

        validateRegisterRequest(request);

        String username =
                request.username().trim();

        String email =
                normalizeEmail(request.email());

        String displayName =
                request.displayName() == null
                        || request.displayName().isBlank()
                        ? username
                        : request.displayName().trim();

        if (userRepository.existsByUsernameIgnoreCase(username)) {

            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "Username is already registered."
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {

            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "Email is already registered."
            );
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);

        /*
         * Never store the raw password.
         */
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setProvider("LOCAL");
        user.setProviderId(null);
        user.setRole("USER");

        Instant now = Instant.now();

        user.setCreatedAt(now);
        user.setLastLoginAt(now);

        try {

            User savedUser =
                    userRepository.save(user);

            String token =
                    jwtService.generateToken(
                            savedUser.getId(),
                            savedUser.getUsername(),
                            savedUser.getEmail(),
                            savedUser.getRole()
                    );

            return new AuthResponse(
                    token,
                    UserResponse.from(savedUser)
            );

        } catch (DuplicateKeyException exception) {

            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "Username or email is already registered."
            );
        }
    }

    public AuthResponse login(LoginRequest request) {

        validateLoginRequest(request);

        String identifier =
                request.identifier().trim();

        User user;

        if (identifier.contains("@")) {

            user = userRepository
                    .findByEmailIgnoreCase(
                            normalizeEmail(identifier)
                    )
                    .orElseThrow(() ->
                            invalidCredentials()
                    );

        } else {

            user = userRepository
                    .findByUsernameIgnoreCase(identifier)
                    .orElseThrow(() ->
                            invalidCredentials()
                    );
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {

            throw invalidCredentials();
        }

        user.setLastLoginAt(
                Instant.now()
        );

        User updatedUser =
                userRepository.save(user);

        String token =
                jwtService.generateToken(
                        updatedUser.getId(),
                        updatedUser.getUsername(),
                        updatedUser.getEmail(),
                        updatedUser.getRole()
                );

        return new AuthResponse(
                token,
                UserResponse.from(updatedUser)
        );
    }

    public UserResponse getCurrentUser(String userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new AuthException(
                                        HttpStatus.UNAUTHORIZED,
                                        "User account no longer exists."
                                )
                        );

        return UserResponse.from(user);
    }

    private void validateRegisterRequest(
            RegisterRequest request
    ) {

        if (request == null) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Registration request is required."
            );
        }

        if (isBlank(request.username())) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Username is required."
            );
        }

        if (isBlank(request.email())) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required."
            );
        }

        if (isBlank(request.password())) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Password is required."
            );
        }

        String username =
                request.username().trim();

        if (!username.matches(
                "^[a-zA-Z0-9._-]{3,30}$"
        )) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Username must contain 3-30 letters, numbers, dots, underscores or hyphens."
            );
        }

        String email =
                normalizeEmail(request.email());

        if (!email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a valid email address."
            );
        }

        if (request.password().length() < 8) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 8 characters."
            );
        }

        if (request.password().length() > 72) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 characters."
            );
        }
    }

    private void validateLoginRequest(
            LoginRequest request
    ) {

        if (request == null) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Login request is required."
            );
        }

        if (isBlank(request.identifier())
                || isBlank(request.password())) {

            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "Username/email and password are required."
            );
        }
    }

    private AuthException invalidCredentials() {

        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username/email or password."
        );
    }

    private String normalizeEmail(String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {

        return value == null
                || value.isBlank();
    }
}