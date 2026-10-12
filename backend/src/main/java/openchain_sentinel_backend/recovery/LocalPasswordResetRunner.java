package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.repository.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LocalPasswordResetRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    public LocalPasswordResetRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = System.getenv("RESET_USERNAME");
        String newPassword = System.getenv("RESET_PASSWORD");

        // Do nothing during normal application startup.
        if (username == null || username.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return;
        }

        if (newPassword.length() < 12) {
            throw new IllegalArgumentException(
                    "Use a password of at least 12 characters."
            );
        }

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException(
                        "No user found for the supplied username."
                ));

        user.setPasswordHash(
                new BCryptPasswordEncoder().encode(newPassword)
        );

        userRepository.save(user);

        System.out.println(
                "Password reset completed for the specified account."
        );
    }
}