package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.repository.UserRepository;
import openchain_sentinel_backend.service.ProjectService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    private final UserRepository userRepository;

    public ProjectController(
            ProjectService projectService,
            UserRepository userRepository) {

        this.projectService = projectService;

        this.userRepository = userRepository;
    }


    /*
     * =====================================================
     * CREATE
     * =====================================================
     */

    @PostMapping
    public Project createProject(
            @RequestBody Project project,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        return projectService.createProject(
                project,
                userId
        );
    }


    /*
     * =====================================================
     * READ ALL
     * =====================================================
     */

    @GetMapping
    public List<Project> getAllProjects(
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        return projectService.getAllProjects(
                userId
        );
    }


    /*
     * =====================================================
     * READ ONE
     * =====================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(
            @PathVariable String id,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        Project project =
                projectService.getProjectById(
                        id,
                        userId
                );

        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(project);
    }


    /*
     * =====================================================
     * UPDATE
     * =====================================================
     */

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable String id,
            @RequestBody Project project,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        Project updatedProject =
                projectService.updateProject(
                        id,
                        project,
                        userId
                );

        if (updatedProject == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                updatedProject
        );
    }


    /*
     * =====================================================
     * DELETE
     * =====================================================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(
            @PathVariable String id,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        boolean deleted =
                projectService.deleteProject(
                        id,
                        userId
                );

        if (!deleted) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        return ResponseEntity.ok(
                "Project deleted successfully"
        );
    }


    /*
     * =====================================================
     * CURRENT USER
     * =====================================================
     *
     * Authentication.getName() comes from the authenticated
     * principal established by the JWT security layer.
     *
     * Our current auth foundation uses username as the
     * application identity.
     */

    private String getCurrentUserId(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        String username =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(username)
                        .orElse(null);

        if (user == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user not found"
            );
        }

        return user.getId();
    }
}