package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.service.GitHubService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/validate")
    public Map<String, Object> validateRepository(
            @RequestParam String url) {

        return gitHubService.validateRepository(url);
    }
}