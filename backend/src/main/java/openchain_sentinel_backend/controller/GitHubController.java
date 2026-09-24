package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.GitHubRepositoryMetadata;
import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.PomFileInfo;
import openchain_sentinel_backend.service.GitHubService;
import openchain_sentinel_backend.service.MavenAnalyzerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;
    private final MavenAnalyzerService mavenAnalyzerService;

    public GitHubController(
            GitHubService gitHubService,
            MavenAnalyzerService mavenAnalyzerService) {

        this.gitHubService = gitHubService;
        this.mavenAnalyzerService =
                mavenAnalyzerService;
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
}