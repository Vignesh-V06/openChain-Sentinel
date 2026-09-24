package openchain_sentinel_backend.model;

import java.util.ArrayList;
import java.util.List;

public class MavenAnalysisResult {

    private GitHubRepositoryMetadata repository;

    private List<PomFileInfo> pomFiles =
            new ArrayList<>();

    private List<MavenProject> mavenProjects =
            new ArrayList<>();

    private List<MavenDependency> dependencies =
            new ArrayList<>();

    private int directDependencyCount;
    private int transitiveDependencyCount;

    public MavenAnalysisResult() {
    }

    public GitHubRepositoryMetadata getRepository() {
        return repository;
    }

    public void setRepository(
            GitHubRepositoryMetadata repository) {

        this.repository = repository;
    }

    public List<PomFileInfo> getPomFiles() {
        return pomFiles;
    }

    public void setPomFiles(
            List<PomFileInfo> pomFiles) {

        this.pomFiles = pomFiles;
    }

    public List<MavenProject> getMavenProjects() {
        return mavenProjects;
    }

    public void setMavenProjects(
            List<MavenProject> mavenProjects) {

        this.mavenProjects = mavenProjects;
    }

    public List<MavenDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<MavenDependency> dependencies) {

        this.dependencies = dependencies;
    }

    public int getDirectDependencyCount() {
        return directDependencyCount;
    }

    public void setDirectDependencyCount(
            int directDependencyCount) {

        this.directDependencyCount =
                directDependencyCount;
    }

    public int getTransitiveDependencyCount() {
        return transitiveDependencyCount;
    }

    public void setTransitiveDependencyCount(
            int transitiveDependencyCount) {

        this.transitiveDependencyCount =
                transitiveDependencyCount;
    }
}