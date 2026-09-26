package openchain_sentinel_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "scan_risk_summaries")
public class ScanRiskSummary {

    @Id
    private String id;

    @Indexed
    private String scanId;

    @Indexed
    private String projectId;

    private String overallRisk;

    private double highestRiskScore;

    private int vulnerabilityCount;

    private int criticalCount;

    private int highCount;

    private int mediumCount;

    private int lowCount;

    private int affectedComponentCount;

    public ScanRiskSummary() {
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

    public String getOverallRisk() {
        return overallRisk;
    }

    public void setOverallRisk(String overallRisk) {
        this.overallRisk = overallRisk;
    }

    public double getHighestRiskScore() {
        return highestRiskScore;
    }

    public void setHighestRiskScore(
            double highestRiskScore) {

        this.highestRiskScore =
                highestRiskScore;
    }

    public int getVulnerabilityCount() {
        return vulnerabilityCount;
    }

    public void setVulnerabilityCount(
            int vulnerabilityCount) {

        this.vulnerabilityCount =
                vulnerabilityCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getHighCount() {
        return highCount;
    }

    public void setHighCount(int highCount) {
        this.highCount = highCount;
    }

    public int getMediumCount() {
        return mediumCount;
    }

    public void setMediumCount(int mediumCount) {
        this.mediumCount = mediumCount;
    }

    public int getLowCount() {
        return lowCount;
    }

    public void setLowCount(int lowCount) {
        this.lowCount = lowCount;
    }

    public int getAffectedComponentCount() {
        return affectedComponentCount;
    }

    public void setAffectedComponentCount(
            int affectedComponentCount) {

        this.affectedComponentCount =
                affectedComponentCount;
    }
}