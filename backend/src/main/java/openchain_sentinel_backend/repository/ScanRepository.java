package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.Scan;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScanRepository
        extends MongoRepository<Scan, String> {

    /*
     * Existing query.
     */
    List<Scan> findByProjectId(
            String projectId
    );

    /*
     * Used when verifying that a scan actually
     * belongs to the project in the URL.
     */
    Optional<Scan> findByIdAndProjectId(
            String id,
            String projectId
    );
}