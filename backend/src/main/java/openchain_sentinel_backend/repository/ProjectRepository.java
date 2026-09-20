package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepository extends MongoRepository<Project, String> {
}