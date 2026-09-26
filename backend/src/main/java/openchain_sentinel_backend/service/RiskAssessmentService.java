package openchain_sentinel_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.AffectedPathMetrics;
import openchain_sentinel_backend.model.RiskAssessment;
import openchain_sentinel_backend.model.RiskAssessmentDocument;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.repository.RiskAssessmentRepository;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;

@Service
public class RiskAssessmentService {

    private final VulnerabilityResultRepository
            vulnerabilityResultRepository;

    private final RiskAssessmentRepository
            riskAssessmentRepository;

    private final AffectedPathService
            affectedPathService;

    private final RiskEngineService
            riskEngineService;

    public RiskAssessmentService(
            VulnerabilityResultRepository
                    vulnerabilityResultRepository,
            RiskAssessmentRepository
                    riskAssessmentRepository,
            AffectedPathService
                    affectedPathService,
            RiskEngineService
                    riskEngineService) {

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.riskAssessmentRepository =
                riskAssessmentRepository;

        this.affectedPathService =
                affectedPathService;

        this.riskEngineService =
                riskEngineService;
    }

    public List<RiskAssessment> assessScan(
            String scanId) {

        List<VulnerabilityResult> vulnerabilities =
                vulnerabilityResultRepository
                        .findByScanId(scanId);

        List<RiskAssessment> assessments =
                new ArrayList<>();

        /*
         * Remove old risk assessments for this scan.
         * This keeps rescans idempotent.
         */
        riskAssessmentRepository.deleteByScanId(
                scanId
        );

        for (VulnerabilityResult vulnerability :
                vulnerabilities) {

            String projectId =
                    vulnerability.getProjectId();

            String vulnerabilityId =
                    vulnerability.getVulnerabilityId();

            /*
             * Get dependency impact from Neo4j.
             */
            AffectedPathMetrics metrics =
                    affectedPathService
                            .analyzeVulnerability(
                                    projectId,
                                    scanId,
                                    vulnerabilityId
                            );

            /*
             * Calculate risk.
             */
            RiskAssessment assessment =
                    riskEngineService.assess(
                            vulnerability,
                            metrics.getAffectedPathCount(),
                            metrics.getAffectedComponentCount()
                    );

            /*
             * Use actual Neo4j depth.
             */
            assessment.setDependencyDepth(
                    metrics.getDependencyDepth()
            );

            assessment.setAffectedPathCount(
                    metrics.getAffectedPathCount()
            );

            assessment.setAffectedComponentCount(
                    metrics.getAffectedComponentCount()
            );

            assessments.add(assessment);

            /*
             * Persist risk assessment.
             */
            RiskAssessmentDocument document =
                    new RiskAssessmentDocument();

            document.setScanId(scanId);

            document.setProjectId(projectId);

            document.setVulnerabilityId(
                    assessment.getVulnerabilityId()
            );

            document.setRiskScore(
                    assessment.getRiskScore()
            );

            document.setRiskLevel(
                    assessment.getRiskLevel()
            );

            document.setPriority(
                    assessment.getPriority()
            );

            document.setDependencyDepth(
                    assessment.getDependencyDepth()
            );

            document.setAffectedPathCount(
                    assessment.getAffectedPathCount()
            );

            document.setAffectedComponentCount(
                    assessment.getAffectedComponentCount()
            );

            document.setReasons(
                    new ArrayList<>(
                            assessment.getReasons()
                    )
            );

            riskAssessmentRepository.save(
                    document
            );
        }

        return assessments;
    }

    public RiskAssessment assessVulnerability(
            String scanId,
            String vulnerabilityId) {

        List<VulnerabilityResult> vulnerabilities =
                vulnerabilityResultRepository
                        .findByScanIdAndVulnerabilityId(
                                scanId,
                                vulnerabilityId
                        );

        if (vulnerabilities.isEmpty()) {

            throw new RuntimeException(
                    "Vulnerability not found for scan: "
                            + scanId
                            + ", vulnerability: "
                            + vulnerabilityId
            );
        }

        VulnerabilityResult vulnerability =
                vulnerabilities.get(0);

        AffectedPathMetrics metrics =
                affectedPathService
                        .analyzeVulnerability(
                                vulnerability.getProjectId(),
                                scanId,
                                vulnerabilityId
                        );

        RiskAssessment assessment =
                riskEngineService.assess(
                        vulnerability,
                        metrics.getAffectedPathCount(),
                        metrics.getAffectedComponentCount()
                );

        assessment.setDependencyDepth(
                metrics.getDependencyDepth()
        );

        assessment.setAffectedPathCount(
                metrics.getAffectedPathCount()
        );

        assessment.setAffectedComponentCount(
                metrics.getAffectedComponentCount()
        );

        return assessment;
    }

    public List<RiskAssessmentDocument> getPersistedAssessments(
            String scanId) {

        return riskAssessmentRepository.findByScanId(
                scanId
        );
    }
}