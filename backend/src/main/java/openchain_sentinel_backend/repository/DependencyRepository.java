package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.Dependency;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DependencyRepository
        extends MongoRepository<Dependency, String> {

    List<Dependency> findByScanId(String scanId);

    List<Dependency> findByProjectId(String projectId);

    long countByScanId(String scanId);
} 