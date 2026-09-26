package openchain_sentinel_backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import openchain_sentinel_backend.model.ScanRiskSummary;

public interface ScanRiskSummaryRepository
        extends MongoRepository<ScanRiskSummary, String> {

    Optional<ScanRiskSummary> findByScanId(
            String scanId
    );

    void deleteByScanId(
            String scanId
    );
}