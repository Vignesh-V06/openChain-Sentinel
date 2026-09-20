package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.Scan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScanRepository extends MongoRepository<Scan, String> {

    List<Scan> findByProjectId(String projectId);
}