package openchain_sentinel_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.RiskAssessment;
import openchain_sentinel_backend.model.VulnerabilityResult;

@Service
public class RiskEngineService {

    public RiskAssessment assess(
            VulnerabilityResult vulnerability,
            int affectedPathCount,
            int affectedComponentCount) {

        double score = 0.0;

        List<String> reasons = new ArrayList<>();

        /*
         * --------------------------------------------------
         * 1. Vulnerability severity
         * --------------------------------------------------
         */

        String severity = vulnerability.getSeverity();

        if (severity != null) {

            switch (severity.toUpperCase()) {

                case "CRITICAL":
                    score += 60;
                    reasons.add(
                            "Critical vulnerability severity"
                    );
                    break;

                case "HIGH":
                    score += 45;
                    reasons.add(
                            "High vulnerability severity"
                    );
                    break;

                case "MEDIUM":
                    score += 30;
                    reasons.add(
                            "Medium vulnerability severity"
                    );
                    break;

                case "LOW":
                    score += 15;
                    reasons.add(
                            "Low vulnerability severity"
                    );
                    break;

                default:
                    score += 10;
                    reasons.add(
                            "Unknown vulnerability severity"
                    );
            }
        }

        /*
         * --------------------------------------------------
         * 2. Direct vs transitive dependency
         * --------------------------------------------------
         */

        if (vulnerability.isDirect()) {

            score += 15;

            reasons.add(
                    "Dependency is directly declared"
            );

        } else {

            score += 5;

            reasons.add(
                    "Dependency is transitive"
            );
        }

        /*
         * --------------------------------------------------
         * 3. Dependency depth
         * --------------------------------------------------
         */

        int depth = vulnerability.getDepth();

        if (depth <= 1) {

            score += 10;

        } else if (depth <= 3) {

            score += 7;

        } else if (depth <= 5) {

            score += 5;

        } else {

            score += 2;
        }

        /*
         * --------------------------------------------------
         * 4. Affected dependency paths
         * --------------------------------------------------
         */

        if (affectedPathCount >= 5) {

            score += 10;

            reasons.add(
                    "Vulnerability is reachable through multiple dependency paths"
            );

        } else if (affectedPathCount >= 2) {

            score += 7;

            reasons.add(
                    "Vulnerability has multiple affected dependency paths"
            );

        } else if (affectedPathCount == 1) {

            score += 3;

            reasons.add(
                    "One affected dependency path identified"
            );
        }

        /*
         * --------------------------------------------------
         * 5. Affected components
         * --------------------------------------------------
         */

        if (affectedComponentCount >= 5) {

            score += 10;

            reasons.add(
                    "Multiple components are affected"
            );

        } else if (affectedComponentCount >= 2) {

            score += 7;

            reasons.add(
                    "More than one component is affected"
            );

        } else if (affectedComponentCount == 1) {

            score += 3;

            reasons.add(
                    "One component is affected"
            );
        }

        /*
         * --------------------------------------------------
         * 6. Fix availability
         * --------------------------------------------------
         */

        if (vulnerability.getFixedVersion() != null
                && !vulnerability.getFixedVersion().isBlank()) {

            reasons.add(
                    "A fixed version is available"
            );

        } else {

            score += 5;

            reasons.add(
                    "No fixed version is currently available"
            );
        }

        /*
         * --------------------------------------------------
         * 7. Clamp score
         * --------------------------------------------------
         */

        score = Math.min(score, 100.0);

        /*
         * --------------------------------------------------
         * 8. Convert score to risk level
         * --------------------------------------------------
         */

        String riskLevel;

        if (score >= 80) {

            riskLevel = "CRITICAL";

        } else if (score >= 60) {

            riskLevel = "HIGH";

        } else if (score >= 40) {

            riskLevel = "MEDIUM";

        } else {

            riskLevel = "LOW";
        }

        /*
         * --------------------------------------------------
         * 9. Priority
         * --------------------------------------------------
         */

        String priority;

        switch (riskLevel) {

            case "CRITICAL":
                priority = "P0";
                break;

            case "HIGH":
                priority = "P1";
                break;

            case "MEDIUM":
                priority = "P2";
                break;

            default:
                priority = "P3";
        }

        return new RiskAssessment(
                vulnerability.getVulnerabilityId(),
                score,
                riskLevel,
                priority,
                depth,
                affectedPathCount,
                affectedComponentCount,
                reasons
        );
    }
}