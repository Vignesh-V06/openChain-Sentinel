package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.Project;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends MongoRepository<Project, String> {

    /*
     * Get all projects belonging to one user.
     */
    List<Project> findByUserId(String userId);

    /*
     * Get one project only if it belongs to the user.
     */
    Optional<Project> findByIdAndUserId(
            String id,
            String userId
    );

    /*
     * Check ownership without loading the project.
     */
    boolean existsByIdAndUserId(
            String id,
            String userId
    );
}