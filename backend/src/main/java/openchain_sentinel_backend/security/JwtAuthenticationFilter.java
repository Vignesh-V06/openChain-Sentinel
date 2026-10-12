package openchain_sentinel_backend.security;

import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        /*
         * No Authorization header.
         *
         * This is okay for public endpoints such as:
         * POST /api/auth/register
         * POST /api/auth/login
         * GET  /api/health
         *
         * Spring Security will decide later whether
         * authentication is actually required.
         */
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);

        try {

            /*
             * The JWT contains:
             *
             * sub      -> user ID
             * username -> username
             * email    -> email
             * role     -> role
             *
             * We prefer username because our application
             * uses username as the authentication name.
             */
            String username =
                    jwtService.extractUsernameClaim(token);

            String userId =
                    jwtService.extractSubject(token);

            User user = null;

            /*
             * First try username from the explicit username claim.
             */
            if (username != null && !username.isBlank()) {

                Optional<User> userOptional =
                        userRepository.findByUsernameIgnoreCase(username);

                if (userOptional.isPresent()) {
                    user = userOptional.get();
                }
            }

            /*
             * Fallback:
             *
             * If username lookup fails, use the JWT subject
             * as the Mongo user ID.
             *
             * This makes the authentication layer robust even
             * if the JWT format changes later.
             */
            if (user == null
                    && userId != null
                    && !userId.isBlank()) {

                Optional<User> userOptional =
                        userRepository.findById(userId);

                if (userOptional.isPresent()) {
                    user = userOptional.get();
                }
            }

            /*
             * If we cannot resolve the user, do not authenticate
             * the request.
             */
            if (user != null) {

                String role = user.getRole();

                if (role == null || role.isBlank()) {
                    role = "USER";
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception ignored) {

            /*
             * Invalid / expired JWT.
             *
             * Do not crash the request here.
             * Spring Security will reject protected endpoints
             * because there is no authenticated SecurityContext.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}