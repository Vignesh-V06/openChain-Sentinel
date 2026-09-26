package openchain_sentinel_backend.model;

import java.util.ArrayList;
import java.util.List;

public class AffectedPathMetrics {

    private String vulnerabilityId;

    private int affectedPathCount;

    private int affectedComponentCount;

    private int dependencyDepth;

    private List<List<String>> dependencyPaths = new ArrayList<>();

    public AffectedPathMetrics() {
    }

    public AffectedPathMetrics(
            String vulnerabilityId,
            int affectedPathCount,
            int affectedComponentCount,
            int dependencyDepth,
            List<List<String>> dependencyPaths) {

        this.vulnerabilityId = vulnerabilityId;
        this.affectedPathCount = affectedPathCount;
        this.affectedComponentCount = affectedComponentCount;
        this.dependencyDepth = dependencyDepth;
        this.dependencyPaths = dependencyPaths;
    }

    public String getVulnerabilityId() {
        return vulnerabilityId;
    }

    public void setVulnerabilityId(String vulnerabilityId) {
        this.vulnerabilityId = vulnerabilityId;
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

    public int getDependencyDepth() {
        return dependencyDepth;
    }

    public void setDependencyDepth(int dependencyDepth) {
        this.dependencyDepth = dependencyDepth;
    }

    public List<List<String>> getDependencyPaths() {
        return dependencyPaths;
    }

    public void setDependencyPaths(
            List<List<String>> dependencyPaths) {

        this.dependencyPaths = dependencyPaths;
    }
}