package openchain_sentinel_backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.RiskAssessmentDocument;
import openchain_sentinel_backend.model.ScanRiskSummary;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.repository.RiskAssessmentRepository;
import openchain_sentinel_backend.repository.ScanRiskSummaryRepository;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;

@Service
public class ScanRiskSummaryService {

    private final RiskAssessmentRepository
            riskAssessmentRepository;

    private final VulnerabilityResultRepository
            vulnerabilityResultRepository;

    private final ScanRiskSummaryRepository
            scanRiskSummaryRepository;

    public ScanRiskSummaryService(
            RiskAssessmentRepository
                    riskAssessmentRepository,
            VulnerabilityResultRepository
                    vulnerabilityResultRepository,
            ScanRiskSummaryRepository
                    scanRiskSummaryRepository) {

        this.riskAssessmentRepository =
                riskAssessmentRepository;

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.scanRiskSummaryRepository =
                scanRiskSummaryRepository;
    }

    public ScanRiskSummary generateSummary(
            String scanId,
            String projectId) {

        List<RiskAssessmentDocument> assessments =
                riskAssessmentRepository.findByScanId(
                        scanId
                );

        List<VulnerabilityResult> vulnerabilities =
                vulnerabilityResultRepository
                        .findByScanId(scanId);

        ScanRiskSummary summary =
                new ScanRiskSummary();

        summary.setScanId(scanId);
        summary.setProjectId(projectId);

        /*
         * No vulnerabilities.
         */
        if (assessments.isEmpty()) {

            summary.setOverallRisk("NONE");
            summary.setHighestRiskScore(0);
            summary.setVulnerabilityCount(0);
            summary.setCriticalCount(0);
            summary.setHighCount(0);
            summary.setMediumCount(0);
            summary.setLowCount(0);
            summary.setAffectedComponentCount(0);

            scanRiskSummaryRepository.deleteByScanId(
                    scanId
            );

            return scanRiskSummaryRepository.save(
                    summary
            );
        }

        double highestRiskScore = 0;

        String overallRisk = "LOW";

        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (RiskAssessmentDocument assessment :
                assessments) {

            double score =
                    assessment.getRiskScore();

            if (score > highestRiskScore) {

                highestRiskScore = score;
                overallRisk =
                        assessment.getRiskLevel();
            }

            switch (
                    assessment.getRiskLevel()
                            .toUpperCase()
            ) {

                case "CRITICAL":
                    criticalCount++;
                    break;

                case "HIGH":
                    highCount++;
                    break;

                case "MEDIUM":
                    mediumCount++;
                    break;

                case "LOW":
                    lowCount++;
                    break;

                default:
                    break;
            }
        }

        /*
         * Count distinct vulnerable package versions.
         *
         * Example:
         *
         * Tomcat 11.0.24
         *     ├── vulnerability A
         *     ├── vulnerability B
         *     └── vulnerability C
         *
         * This should count as ONE affected component,
         * not three.
         */
        Set<String> affectedComponents =
                new HashSet<>();

        for (VulnerabilityResult vulnerability :
                vulnerabilities) {

            String coordinate =
                    vulnerability.getGroupId()
                            + ":"
                            + vulnerability.getArtifactId()
                            + ":"
                            + vulnerability.getVersion();

            affectedComponents.add(coordinate);
        }

        summary.setOverallRisk(overallRisk);

        summary.setHighestRiskScore(
                highestRiskScore
        );

        summary.setVulnerabilityCount(
                assessments.size()
        );

        summary.setCriticalCount(
                criticalCount
        );

        summary.setHighCount(
                highCount
        );

        summary.setMediumCount(
                mediumCount
        );

        summary.setLowCount(
                lowCount
        );

        summary.setAffectedComponentCount(
                affectedComponents.size()
        );

        /*
         * Keep exactly one summary per scan.
         */
        scanRiskSummaryRepository.deleteByScanId(
                scanId
        );

        return scanRiskSummaryRepository.save(
                summary
        );
    }

    public ScanRiskSummary getSummary(
            String scanId) {

        return scanRiskSummaryRepository
                .findByScanId(scanId)
                .orElse(null);
    }
}