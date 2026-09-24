package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.Dependency;
import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.MavenDependency;
import openchain_sentinel_backend.repository.DependencyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DependencyStorageService {

    private final DependencyRepository dependencyRepository;

    public DependencyStorageService(
            DependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    public List<Dependency> saveDependencies(
            String projectId,
            String scanId,
            MavenAnalysisResult analysisResult) {

        List<Dependency> dependencies = new ArrayList<>();

        for (MavenDependency mavenDependency
                : analysisResult.getDependencies()) {

            Dependency dependency = new Dependency();

            dependency.setProjectId(projectId);
            dependency.setScanId(scanId);

            dependency.setGroupId(
                    mavenDependency.getGroupId());

            dependency.setArtifactId(
                    mavenDependency.getArtifactId());

            dependency.setVersion(
                    mavenDependency.getVersion());

            dependency.setType(
                    mavenDependency.getType());

            dependency.setScope(
                    mavenDependency.getScope());

            dependency.setClassifier(
                    mavenDependency.getClassifier());

            dependency.setOptional(
                    mavenDependency.isOptional());

            dependency.setDirect(
                    mavenDependency.isDirect());

            dependency.setDepth(
                    mavenDependency.getDepth());

            dependency.setParentCoordinate(
                    mavenDependency.getParentCoordinate());

            dependency.setPomPath(
                    mavenDependency.getPomPath());

            dependency.setDependencyPath(
                    new ArrayList<>(
                            mavenDependency.getDependencyPath()));

            dependencies.add(dependency);
        }

        return dependencyRepository.saveAll(dependencies);
    }

    public List<Dependency> getDependenciesByScan(
            String scanId) {

        return dependencyRepository.findByScanId(scanId);
    }

    public List<Dependency> getDependenciesByProject(
            String projectId) {

        return dependencyRepository.findByProjectId(projectId);
    }

    public long countDependenciesByScan(
            String scanId) {

        return dependencyRepository.countByScanId(scanId);
    }
}