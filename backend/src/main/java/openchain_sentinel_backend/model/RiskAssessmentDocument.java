package openchain_sentinel_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "risk_assessments")
public class RiskAssessmentDocument {

    @Id
    private String id;

    @Indexed
    private String scanId;

    @Indexed
    private String projectId;

    @Indexed
    private String vulnerabilityId;

    private double riskScore;

    private String riskLevel;

    private String priority;

    private int dependencyDepth;

    private int affectedPathCount;

    private int affectedComponentCount;

    private List<String> reasons = new ArrayList<>();

    public RiskAssessmentDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public void setAffectedComponentCount(
            int affectedComponentCount) {

        this.affectedComponentCount =
                affectedComponentCount;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}