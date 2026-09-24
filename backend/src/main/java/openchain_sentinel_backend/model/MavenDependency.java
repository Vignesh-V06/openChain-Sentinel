package openchain_sentinel_backend.model;

import java.util.ArrayList;
import java.util.List;

public class MavenDependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private String classifier;

    private boolean optional;
    private boolean direct;

    private int depth;

    private String parentCoordinate;

    private String pomPath;

    private List<String> dependencyPath = new ArrayList<>();

    public MavenDependency() {
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getParentCoordinate() {
        return parentCoordinate;
    }

    public void setParentCoordinate(String parentCoordinate) {
        this.parentCoordinate = parentCoordinate;
    }

    public String getPomPath() {
        return pomPath;
    }

    public void setPomPath(String pomPath) {
        this.pomPath = pomPath;
    }

    public List<String> getDependencyPath() {
        return dependencyPath;
    }

    public void setDependencyPath(List<String> dependencyPath) {
        this.dependencyPath = dependencyPath;
    }
}