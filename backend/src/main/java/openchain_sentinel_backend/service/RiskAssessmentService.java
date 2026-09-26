package openchain_sentinel_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.AffectedPathMetrics;
import openchain_sentinel_backend.model.RiskAssessment;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;

@Service
public class RiskAssessmentService {

    private final VulnerabilityResultRepository vulnerabilityResultRepository;
    private final AffectedPathService affectedPathService;
    private final RiskEngineService riskEngineService;

    public RiskAssessmentService(
            VulnerabilityResultRepository vulnerabilityResultRepository,
            AffectedPathService affectedPathService,
            RiskEngineService riskEngineService) {

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.affectedPathService =
                affectedPathService;

        this.riskEngineService =
                riskEngineService;
    }

    public List<RiskAssessment> assessScan(String scanId) {

        List<VulnerabilityResult> vulnerabilities =
                vulnerabilityResultRepository.findByScanId(scanId);

        List<RiskAssessment> assessments =
                new ArrayList<>();

        for (VulnerabilityResult vulnerability : vulnerabilities) {

            String projectId =
                    vulnerability.getProjectId();

            String vulnerabilityId =
                    vulnerability.getVulnerabilityId();

            /*
             * Get dependency impact information
             * from Neo4j.
             */
            AffectedPathMetrics metrics =
                    affectedPathService.analyzeVulnerability(
                            projectId,
                            scanId,
                            vulnerabilityId
                    );

            /*
             * Pass the vulnerability itself together
             * with the Neo4j dependency impact metrics
             * to the Risk Engine.
             */
            RiskAssessment assessment =
                    riskEngineService.assess(
                            vulnerability,
                            metrics.getAffectedPathCount(),
                            metrics.getAffectedComponentCount()
                    );

            /*
             * Use the actual Neo4j dependency depth.
             */
            assessment.setDependencyDepth(
                    metrics.getDependencyDepth()
            );

            /*
             * Keep the affected-path count from Neo4j.
             */
            assessment.setAffectedPathCount(
                    metrics.getAffectedPathCount()
            );

            /*
             * Keep the affected-component count from Neo4j.
             */
            assessment.setAffectedComponentCount(
                    metrics.getAffectedComponentCount()
            );

            assessments.add(assessment);
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
                affectedPathService.analyzeVulnerability(
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
}