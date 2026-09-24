package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.GitHubRepositoryMetadata;
import openchain_sentinel_backend.model.PomFileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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

    // -----------------------------------------
    // GET REPOSITORY METADATA
    // -----------------------------------------

    public GitHubRepositoryMetadata getRepositoryMetadata(
            String repositoryUrl) {

        String[] repositoryParts =
                extractRepositoryParts(repositoryUrl);

        String owner = repositoryParts[0];
        String repository = repositoryParts[1];

        Map<String, Object> response = restClient.get()
                .uri(
                        "/repos/{owner}/{repository}",
                        owner,
                        repository
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No repository information returned");
        }

        String actualOwner = owner;

        Object ownerObject = response.get("owner");

        if (ownerObject instanceof Map<?, ?> ownerMap) {

            Object login = ownerMap.get("login");

            if (login != null) {
                actualOwner = login.toString();
            }
        }

        return new GitHubRepositoryMetadata(
                actualOwner,
                response.get("name").toString(),
                response.get("default_branch").toString(),
                Boolean.TRUE.equals(response.get("private")),
                response.get("html_url").toString()
        );
    }

    // -----------------------------------------
    // DISCOVER ALL pom.xml FILES
    // -----------------------------------------

    public List<PomFileInfo> findPomFiles(
            GitHubRepositoryMetadata metadata) {

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(
                                "/repos/{owner}/{repository}/git/trees/{branch}"
                        )
                        .queryParam("recursive", "1")
                        .build(
                                metadata.getOwner(),
                                metadata.getRepository(),
                                metadata.getDefaultBranch()
                        )
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No repository tree information returned");
        }

        Object treeObject = response.get("tree");

        if (!(treeObject instanceof List<?> tree)) {
            throw new IllegalStateException(
                    "Repository tree was not returned");
        }

        List<PomFileInfo> pomFiles = new ArrayList<>();

        for (Object itemObject : tree) {

            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            Object typeObject = item.get("type");
            Object pathObject = item.get("path");
            Object shaObject = item.get("sha");
            Object sizeObject = item.get("size");

            if (typeObject == null || pathObject == null) {
                continue;
            }

            String type = typeObject.toString();
            String path = pathObject.toString();

            // We only want actual files.
            if (!"blob".equals(type)) {
                continue;
            }

            // Match pom.xml files regardless of directory.
            if (!path.endsWith("/pom.xml")
                    && !path.equals("pom.xml")) {
                continue;
            }

            String sha =
                    shaObject != null
                            ? shaObject.toString()
                            : null;

            long size = 0;

            if (sizeObject instanceof Number number) {
                size = number.longValue();
            }

            pomFiles.add(
                    new PomFileInfo(
                            path,
                            sha,
                            size
                    )
            );
        }

        return pomFiles;
    }

    // -----------------------------------------
    // GET pom.xml CONTENT USING ITS GIT BLOB
    // -----------------------------------------

    public String getPomXml(
            GitHubRepositoryMetadata metadata,
            PomFileInfo pomFile) {

        if (pomFile.getSha() == null
                || pomFile.getSha().isBlank()) {

            throw new IllegalStateException(
                    "POM file SHA is missing");
        }

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(
                                "/repos/{owner}/{repository}/git/blobs/{sha}"
                        )
                        .build(
                                metadata.getOwner(),
                                metadata.getRepository(),
                                pomFile.getSha()
                        )
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No POM blob information returned");
        }

        Object contentObject = response.get("content");

        if (contentObject == null) {
            throw new IllegalStateException(
                    "POM content was not returned");
        }

        String encodedContent =
                contentObject.toString()
                        .replace("\n", "")
                        .replace("\r", "");

        byte[] decodedContent =
                Base64.getDecoder().decode(encodedContent);

        return new String(decodedContent);
    }

    // -----------------------------------------
    // GET A USABLE POM
    // -----------------------------------------

    public String getPomXml(
            GitHubRepositoryMetadata metadata) {

        List<PomFileInfo> pomFiles =
                findPomFiles(metadata);

        if (pomFiles.isEmpty()) {

            throw new IllegalStateException(
                    "No pom.xml file found in repository");
        }

        /*
         * Prefer the root pom.xml when one exists.
         */
        for (PomFileInfo pomFile : pomFiles) {

            if ("pom.xml".equals(pomFile.getPath())) {

                return getPomXml(
                        metadata,
                        pomFile
                );
            }
        }

        /*
         * If there is exactly one nested POM,
         * it is unambiguous.
         */
        if (pomFiles.size() == 1) {

            return getPomXml(
                    metadata,
                    pomFiles.get(0)
            );
        }

        /*
         * Multiple nested POMs without a root POM
         * require Maven project selection logic.
         */
        throw new IllegalStateException(
                "Multiple pom.xml files found. " +
                "Maven project selection is required."
        );
    }

    // -----------------------------------------
    // VALIDATE REPOSITORY
    // -----------------------------------------

    public Map<String, Object> validateRepository(
            String repositoryUrl) {

        Map<String, Object> result =
                new HashMap<>();

        try {

            GitHubRepositoryMetadata metadata =
                    getRepositoryMetadata(repositoryUrl);

            result.put("valid", true);
            result.put(
                    "message",
                    "GitHub repository found"
            );
            result.put(
                    "owner",
                    metadata.getOwner()
            );
            result.put(
                    "repository",
                    metadata.getRepository()
            );
            result.put(
                    "defaultBranch",
                    metadata.getDefaultBranch()
            );
            result.put(
                    "private",
                    metadata.isPrivate()
            );

            return result;

        } catch (Exception e) {

            result.put("valid", false);
            result.put(
                    "message",
                    e.getMessage()
            );

            return result;
        }
    }

    // -----------------------------------------
    // EXTRACT OWNER + REPOSITORY
    // -----------------------------------------

    private String[] extractRepositoryParts(
            String repositoryUrl) {

        try {

            java.net.URI uri =
                    java.net.URI.create(repositoryUrl);

            String host = uri.getHost();

            if (host == null
                    || !host.equalsIgnoreCase("github.com")) {

                throw new IllegalArgumentException(
                        "URL is not a GitHub repository URL"
                );
            }

            String path = uri.getPath();

            if (path == null || path.isBlank()) {

                throw new IllegalArgumentException(
                        "GitHub repository path is missing"
                );
            }

            String[] parts =
                    path.split("/");

            if (parts.length < 3) {

                throw new IllegalArgumentException(
                        "Invalid GitHub repository URL"
                );
            }

            String owner = parts[1];

            String repository = parts[2];

            if (repository.endsWith(".git")) {

                repository =
                        repository.substring(
                                0,
                                repository.length() - 4
                        );
            }

            return new String[]{
                    owner,
                    repository
            };

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Invalid GitHub repository URL"
            );
        }
    }
}