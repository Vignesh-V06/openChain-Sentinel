package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.GitHubRepositoryMetadata;
import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.PomFileInfo;
import openchain_sentinel_backend.service.GitHubService;
import openchain_sentinel_backend.service.MavenAnalyzerService;
import openchain_sentinel_backend.model.Dependency;
import openchain_sentinel_backend.service.DependencyStorageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;
    private final MavenAnalyzerService mavenAnalyzerService;
    private final DependencyStorageService dependencyStorageService;

    public GitHubController(
            GitHubService gitHubService,
            MavenAnalyzerService mavenAnalyzerService,
            DependencyStorageService dependencyStorageService) {

        this.gitHubService = gitHubService;
        this.mavenAnalyzerService =
                mavenAnalyzerService;
        this.dependencyStorageService = dependencyStorageService;
    }

    // TEMPORARY TEST ENDPOINT

    @GetMapping("/validate")
    public Map<String, Object> validateRepository(
            @RequestParam String url) {

        return gitHubService.validateRepository(url);
    }

    // TEMPORARY TEST ENDPOINT

    @GetMapping("/metadata")
    public GitHubRepositoryMetadata getMetadata(
            @RequestParam String url) {

        return gitHubService.getRepositoryMetadata(url);
    }

    // TEMPORARY TEST ENDPOINT

    @GetMapping("/pom-files")
    public List<PomFileInfo> findPomFiles(
            @RequestParam String url) {

        GitHubRepositoryMetadata metadata =
                gitHubService.getRepositoryMetadata(url);

        return gitHubService.findPomFiles(metadata);
    }

    // TEMPORARY TEST ENDPOINT

    @GetMapping("/pom")
    public String getPom(
            @RequestParam String url) {

        GitHubRepositoryMetadata metadata =
                gitHubService.getRepositoryMetadata(url);

        return gitHubService.getPomXml(metadata);
    }

    // TEMPORARY TEST ENDPOINT
    // Full Maven analysis

    @GetMapping("/maven-analysis")
    public MavenAnalysisResult analyzeMavenRepository(
            @RequestParam String url) {

        return mavenAnalyzerService.analyze(url);
    }

    // TEMPORARY TEST ENDPOINT
    // Store dependencies in database
    @PostMapping("/maven-analysis/store")
    public Map<String, Object> analyzeAndStore(
            @RequestParam String url,
            @RequestParam String projectId,
            @RequestParam String scanId) {

        MavenAnalysisResult analysisResult =
                mavenAnalyzerService.analyze(url);

        List<openchain_sentinel_backend.model.Dependency>
                savedDependencies =
                dependencyStorageService.saveDependencies(
                        projectId,
                        scanId,
                        analysisResult
                );

        return Map.of(
                "projectId", projectId,
                "scanId", scanId,
                "dependencyCount", savedDependencies.size(),
                "message", "Dependencies stored successfully"
        );
    }
    @GetMapping("/dependencies")
    public List<Dependency> getDependencies(
            @RequestParam String scanId) {

        return dependencyStorageService
                .getDependenciesByScan(scanId);
    }
    @GetMapping("/dependencies/count")
    public long countDependencies(
            @RequestParam String scanId) {

        return dependencyStorageService
                .countDependenciesByScan(scanId);
    }
}