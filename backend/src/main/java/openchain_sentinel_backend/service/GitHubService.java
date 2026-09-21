package openchain_sentinel_backend.service;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubService {

    private final RestClient restClient;

    public GitHubService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("User-Agent", "OpenChain-Sentinel")
                .build();
    }

    public Map<String, Object> validateRepository(String repositoryUrl) {

        Map<String, Object> result = new HashMap<>();

        try {
            URI uri = URI.create(repositoryUrl);

            String host = uri.getHost();

            if (host == null || !host.equalsIgnoreCase("github.com")) {
                result.put("valid", false);
                result.put("message", "URL is not a GitHub repository URL");
                return result;
            }

            String path = uri.getPath();
 
            if (path == null || path.isBlank()) {
                result.put("valid", false);
                result.put("message", "GitHub repository path is missing");
                return result;
            }

            String[] parts = path.split("/");

            if (parts.length < 3) {
                result.put("valid", false);
                result.put("message", "Invalid GitHub repository URL");
                return result;
            }

            String owner = parts[1];
            String repository = parts[2];

            if (repository.endsWith(".git")) {
                repository = repository.substring(
                        0,
                        repository.length() - 4
                );
            }

            String finalRepository = repository;

            Map response = restClient.get()
                    .uri("/repos/{owner}/{repository}", owner, finalRepository)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                result.put("valid", false);
                result.put("message", "No repository information returned");
                return result;
            }

            result.put("valid", true);
            result.put("message", "GitHub repository found");
            result.put("owner", response.get("owner") instanceof Map
                    ? ((Map<?, ?>) response.get("owner")).get("login")
                    : owner);
            result.put("repository", response.get("name"));
            result.put("defaultBranch", response.get("default_branch"));
            result.put("private", response.get("private"));

            return result;

        } catch (Exception e) {

            result.put("valid", false);
            result.put("message", "Unable to access GitHub repository");

            return result;
        }
    }
}