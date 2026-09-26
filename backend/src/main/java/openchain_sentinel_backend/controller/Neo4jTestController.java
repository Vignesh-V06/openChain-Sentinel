package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.service.MavenAnalyzerService;
import openchain_sentinel_backend.service.Neo4jConnectionService;
import openchain_sentinel_backend.service.Neo4jGraphService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/neo4j")
public class Neo4jTestController {

    private final Neo4jConnectionService neo4jConnectionService;
    private final MavenAnalyzerService mavenAnalyzerService;
    private final Neo4jGraphService neo4jGraphService;

    public Neo4jTestController(
            Neo4jConnectionService neo4jConnectionService,
            MavenAnalyzerService mavenAnalyzerService,
            Neo4jGraphService neo4jGraphService) {

        this.neo4jConnectionService = neo4jConnectionService;
        this.mavenAnalyzerService = mavenAnalyzerService;
        this.neo4jGraphService = neo4jGraphService;
    }

    @GetMapping("/test")
    public String testConnection() {
        return neo4jConnectionService.testConnection();
    }

    @PostMapping("/graph-test")
    public String testGraphCreation(
            @RequestParam String url,
            @RequestParam String projectId,
            @RequestParam String projectName,
            @RequestParam String scanId) {

        MavenAnalysisResult analysisResult =
                mavenAnalyzerService.analyze(url);

        neo4jGraphService.createDependencyGraph(
                projectId,
                projectName,
                scanId,
                analysisResult
        );

        return "Maven analysis completed and dependency graph created in Neo4j.";
    }

    @GetMapping("/direct-dependencies")
    public List<Map<String, Object>> getDirectDependencies(
            @RequestParam String projectId,
            @RequestParam String scanId) {

        return neo4jGraphService.getDirectDependencies(
                projectId,
                scanId
        );
    }

    @GetMapping("/dependency-tree")
    public List<Map<String, Object>> getDependencyTree(
            @RequestParam String projectId,
            @RequestParam String scanId) {

        return neo4jGraphService.getDependencyTree(
                projectId,
                scanId
        );
    }

    @GetMapping("/dependency-paths")
    public List<Map<String, Object>> getDependencyPaths(
            @RequestParam String projectId,
            @RequestParam String scanId,
            @RequestParam String coordinate) {

        return neo4jGraphService.getPathsToDependency(
                projectId,
                scanId,
                coordinate
        );
    }
}