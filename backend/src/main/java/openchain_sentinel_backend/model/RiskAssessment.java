package openchain_sentinel_backend.model;

import java.util.ArrayList;
import java.util.List;

public class RiskAssessment {

    private String vulnerabilityId;

    private double riskScore;

    private String riskLevel;

    private String priority;

    private int dependencyDepth;

    private int affectedPathCount;

    private int affectedComponentCount;

    private List<String> reasons = new ArrayList<>();

    public RiskAssessment() {
    }

    public RiskAssessment(
            String vulnerabilityId,
            double riskScore,
            String riskLevel,
            String priority,
            int dependencyDepth,
            int affectedPathCount,
            int affectedComponentCount,
            List<String> reasons) {

        this.vulnerabilityId = vulnerabilityId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.priority = priority;
        this.dependencyDepth = dependencyDepth;
        this.affectedPathCount = affectedPathCount;
        this.affectedComponentCount = affectedComponentCount;
        this.reasons = reasons;
    }

    public String getVulnerabilityId() {
        return vulnerabilityId;
    }

    public void setVulnerabilityId(String vulnerabilityId) {
        this.vulnerabilityId = vulnerabilityId;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getDependencyDepth() {
        return dependencyDepth;
    }

    public void setDependencyDepth(int dependencyDepth) {
        this.dependencyDepth = dependencyDepth;
    }

    public int getAffectedPathCount() {
        return affectedPathCount;
    }

    public void setAffectedPathCount(int affectedPathCount) {
        this.affectedPathCount = affectedPathCount;
    }

    public int getAffectedComponentCount() {
        return affectedComponentCount;
    }

    public void setAffectedComponentCount(int affectedComponentCount) {
        this.affectedComponentCount = affectedComponentCount;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}