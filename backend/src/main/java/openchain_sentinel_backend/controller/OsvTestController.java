package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.MavenDependency;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.service.MavenAnalyzerService;
import openchain_sentinel_backend.service.OsvService;
import openchain_sentinel_backend.service.OsvVulnerabilityStorageService;

import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/osv")
public class OsvTestController {

    private final OsvService osvService;
    private final MavenAnalyzerService mavenAnalyzerService;
    private final OsvVulnerabilityStorageService
            osvVulnerabilityStorageService;

    public OsvTestController(
            OsvService osvService,
            MavenAnalyzerService mavenAnalyzerService,
            OsvVulnerabilityStorageService
                    osvVulnerabilityStorageService) {

        this.osvService = osvService;
        this.mavenAnalyzerService = mavenAnalyzerService;
        this.osvVulnerabilityStorageService =
                osvVulnerabilityStorageService;
    }

    /**
     * Tests a single package/version against OSV.
     */
    @GetMapping("/test")
    public Map<String, Object> testOsv(
            @RequestParam String groupId,
            @RequestParam String artifactId,
            @RequestParam String version) {

        JsonNode response =
                osvService.queryVulnerabilities(
                        groupId,
                        artifactId,
                        version
                );

        return Map.of(
                "groupId",
                groupId,

                "artifactId",
                artifactId,

                "version",
                version,

                "vulnerabilityCount",
                osvService.getVulnerabilityCount(
                        response
                ),

                "response",
                response
        );
    }

    /**
     * Runs Maven dependency analysis,
     * scans all resolved dependencies through OSV,
     * converts vulnerable results into VulnerabilityResult
     * documents and stores them in MongoDB.
     */
    @PostMapping("/scan")
    public Map<String, Object> scanRepository(
            @RequestParam String url,
            @RequestParam String projectId,
            @RequestParam String scanId) {

        /*
         * Step 1:
         * Analyze the repository using the
         * existing Maven Analyzer.
         */
        MavenAnalysisResult analysisResult =
                mavenAnalyzerService.analyze(url);

        /*
         * Step 2:
         * Get all resolved Maven dependencies.
         */
        List<MavenDependency> dependencies =
                analysisResult.getDependencies();

        /*
         * Step 3:
         * Scan dependencies through OSV
         * AND store vulnerability records
         * in MongoDB.
         */
        List<VulnerabilityResult> storedVulnerabilities =
                osvVulnerabilityStorageService.scanAndStore(
                        projectId,
                        scanId,
                        dependencies
                );

        /*
         * Step 4:
         * Count unique vulnerable package versions.
         */
        int vulnerableDependencies = 0;

        for (MavenDependency dependency : dependencies) {

            boolean vulnerable = false;

            for (VulnerabilityResult vulnerability :
                    storedVulnerabilities) {

                if (dependency.getGroupId()
                        .equals(vulnerability.getGroupId())
                        && dependency.getArtifactId()
                        .equals(vulnerability.getArtifactId())
                        && dependency.getVersion()
                        .equals(vulnerability.getVersion())) {

                    vulnerable = true;
                    break;
                }
            }

            if (vulnerable) {
                vulnerableDependencies++;
            }
        }

        /*
         * Step 5:
         * Build response.
         */
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "repositoryUrl",
                url
        );

        response.put(
                "projectId",
                projectId
        );

        response.put(
                "scanId",
                scanId
        );

        response.put(
                "resolvedDependencyCount",
                dependencies.size()
        );

        response.put(
                "vulnerableDependencyCount",
                vulnerableDependencies
        );

        response.put(
                "totalVulnerabilityCount",
                storedVulnerabilities.size()
        );

        response.put(
                "storedVulnerabilities",
                storedVulnerabilities
        );

        return response;
    }
}