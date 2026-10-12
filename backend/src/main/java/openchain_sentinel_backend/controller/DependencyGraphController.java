package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.service.DependencyGraphService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/scans")
@CrossOrigin(origins = "http://localhost:5173")
public class DependencyGraphController {

    private final DependencyGraphService dependencyGraphService;

    public DependencyGraphController(
            DependencyGraphService dependencyGraphService) {

        this.dependencyGraphService =
                dependencyGraphService;
    }

    @GetMapping("/{scanId}/graph")
    public Map<String, Object> getDependencyGraph(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        return dependencyGraphService.getDependencyGraph(
                projectId,
                scanId
        );
    }
}