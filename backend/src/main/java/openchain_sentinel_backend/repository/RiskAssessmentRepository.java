package openchain_sentinel_backend.repository;

import openchain_sentinel_backend.model.RiskAssessmentDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RiskAssessmentRepository
        extends MongoRepository<RiskAssessmentDocument, String> {

    List<RiskAssessmentDocument> findByScanId(
            String scanId
    );

    List<RiskAssessmentDocument> findByProjectId(
            String projectId
    );

    void deleteByScanId(
            String scanId
    );
}