package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.MavenDependency;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OsvService {

    private static final String OSV_QUERY_URL =
            "https://api.osv.dev/v1/query";

    private static final String OSV_BATCH_URL =
            "https://api.osv.dev/v1/querybatch";

    private static final String OSV_VULNERABILITY_URL =
            "https://api.osv.dev/v1/vulns/";

    private static final String MAVEN_ECOSYSTEM =
            "Maven";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;

    public OsvService() {
        this.httpClient = HttpClient.newHttpClient();
        this.jsonMapper = new JsonMapper();
    }

    /**
     * Queries OSV for vulnerabilities affecting
     * one Maven package version.
     *
     * Example:
     *
     * org.apache.logging.log4j
     * log4j-core
     * 2.17.1
     */
    public JsonNode queryVulnerabilities(
            String groupId,
            String artifactId,
            String version) {

        validateDependency(
                groupId,
                artifactId,
                version
        );

        String packageName =
                groupId + ":" + artifactId;

        String requestBody = """
                {
                    "package": {
                        "name": "%s",
                        "ecosystem": "%s"
                    },
                    "version": "%s"
                }
                """.formatted(
                escapeJson(packageName),
                MAVEN_ECOSYSTEM,
                escapeJson(version)
        );

        return sendPostRequest(
                OSV_QUERY_URL,
                requestBody
        );
    }

    /**
     * Queries OSV for multiple Maven dependencies
     * using the /v1/querybatch endpoint.
     *
     * The dependencies are first deduplicated by:
     *
     * groupId:artifactId:version
     *
     * because the Maven analyzer may contain the same
     * dependency multiple times through different paths.
     */
    public List<Map<String, Object>> scanDependencies(
            List<MavenDependency> dependencies) {

        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }

        /*
         * LinkedHashMap keeps insertion order while
         * removing duplicate package versions.
         */
        Map<String, MavenDependency> uniqueDependencies =
                new LinkedHashMap<>();

        for (MavenDependency dependency : dependencies) {

            if (dependency == null) {
                continue;
            }

            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();

            if (groupId == null
                    || groupId.isBlank()
                    || artifactId == null
                    || artifactId.isBlank()
                    || version == null
                    || version.isBlank()) {

                continue;
            }

            String coordinate =
                    groupId
                            + ":"
                            + artifactId
                            + ":"
                            + version;

            uniqueDependencies.putIfAbsent(
                    coordinate,
                    dependency
            );
        }

        if (uniqueDependencies.isEmpty()) {
            return List.of();
        }

        /*
         * Build the OSV batch request.
         *
         * Example:
         *
         * {
         *   "queries": [
         *      {
         *          "package": {
         *              "name": "org.foo:bar",
         *              "ecosystem": "Maven"
         *          },
         *          "version": "1.0.0"
         *      }
         *   ]
         * }
         */
        StringBuilder requestBody =
                new StringBuilder();

        requestBody.append("{\"queries\":[");

        boolean first = true;

        for (MavenDependency dependency
                : uniqueDependencies.values()) {

            if (!first) {
                requestBody.append(",");
            }

            first = false;

            String packageName =
                    dependency.getGroupId()
                            + ":"
                            + dependency.getArtifactId();

            requestBody.append("""
                    {
                        "package": {
                            "name": "%s",
                            "ecosystem": "%s"
                        },
                        "version": "%s"
                    }
                    """.formatted(
                    escapeJson(packageName),
                    MAVEN_ECOSYSTEM,
                    escapeJson(dependency.getVersion())
            ));
        }

        requestBody.append("]}");

        /*
         * Send the batch request.
         */
        JsonNode batchResponse =
                sendPostRequest(
                        OSV_BATCH_URL,
                        requestBody.toString()
                );

        JsonNode results =
                batchResponse.get("results");

        if (results == null || !results.isArray()) {
            return List.of();
        }

        /*
         * OSV guarantees that the response results
         * correspond to the input query order.
         *
         * Therefore we can match result[i]
         * with dependency[i].
         */
        List<MavenDependency> orderedDependencies =
                new ArrayList<>(
                        uniqueDependencies.values()
                );

        /*
         * Cache vulnerability details so that if
         * the same vulnerability affects multiple
         * packages, we fetch its details only once.
         */
        Map<String, JsonNode> vulnerabilityCache =
                new LinkedHashMap<>();

        List<Map<String, Object>> scanResults =
                new ArrayList<>();

        int resultCount =
                Math.min(
                        results.size(),
                        orderedDependencies.size()
                );

        for (int i = 0; i < resultCount; i++) {

            MavenDependency dependency =
                    orderedDependencies.get(i);

            JsonNode result =
                    results.get(i);

            List<String> vulnerabilityIds =
                    new ArrayList<>();

            List<JsonNode> vulnerabilityDetails =
                    new ArrayList<>();

            JsonNode vulnerabilities =
                    result.get("vulns");

            if (vulnerabilities != null
                    && vulnerabilities.isArray()) {

                for (JsonNode vulnerability
                        : vulnerabilities) {

                    JsonNode idNode =
                            vulnerability.get("id");

                    if (idNode == null
                            || idNode.isNull()) {

                        continue;
                    }

                    String vulnerabilityId =
                            idNode.asString();

                    vulnerabilityIds.add(
                            vulnerabilityId
                    );

                    /*
                     * Fetch complete vulnerability details.
                     */
                    JsonNode details =
                            vulnerabilityCache.get(
                                    vulnerabilityId
                            );

                    if (details == null) {

                        details =
                                getVulnerabilityDetails(
                                        vulnerabilityId
                                );

                        vulnerabilityCache.put(
                                vulnerabilityId,
                                details
                        );
                    }

                    vulnerabilityDetails.add(
                            details
                    );
                }
            }

            String coordinate =
                    dependency.getGroupId()
                            + ":"
                            + dependency.getArtifactId()
                            + ":"
                            + dependency.getVersion();

            Map<String, Object> resultMap =
                    new LinkedHashMap<>();

            resultMap.put(
                    "groupId",
                    dependency.getGroupId()
            );

            resultMap.put(
                    "artifactId",
                    dependency.getArtifactId()
            );

            resultMap.put(
                    "version",
                    dependency.getVersion()
            );

            resultMap.put(
                    "coordinate",
                    coordinate
            );

            resultMap.put(
                    "direct",
                    dependency.isDirect()
            );

            resultMap.put(
                    "depth",
                    dependency.getDepth()
            );

            resultMap.put(
                    "vulnerabilityCount",
                    vulnerabilityIds.size()
            );

            resultMap.put(
                    "vulnerabilityIds",
                    vulnerabilityIds
            );

            resultMap.put(
                    "vulnerabilities",
                    vulnerabilityDetails
            );

            scanResults.add(resultMap);
        }

        return scanResults;
    }

    /**
     * Retrieves the complete vulnerability record
     * for a specific OSV vulnerability ID.
     *
     * Example:
     *
     * GET /v1/vulns/GHSA-xxxx-yyyy-zzzz
     */
    public JsonNode getVulnerabilityDetails(
            String vulnerabilityId) {

        if (vulnerabilityId == null
                || vulnerabilityId.isBlank()) {

            throw new IllegalArgumentException(
                    "Vulnerability ID cannot be empty"
            );
        }

        String url =
                OSV_VULNERABILITY_URL
                        + vulnerabilityId;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "OSV vulnerability API returned HTTP "
                                + response.statusCode()
                                + " for "
                                + vulnerabilityId
                );
            }

            return jsonMapper.readTree(
                    response.body()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to retrieve vulnerability details for "
                            + vulnerabilityId,
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "OSV vulnerability request was interrupted",
                    e
            );
        }
    }

    /**
     * Returns the number of vulnerabilities in
     * a single OSV query response.
     */
    public int getVulnerabilityCount(
            JsonNode response) {

        if (response == null
                || !response.has("vulns")
                || !response.get("vulns").isArray()) {

            return 0;
        }

        return response.get("vulns").size();
    }

    /**
     * Sends a POST request containing JSON to OSV.
     */
    private JsonNode sendPostRequest(
            String url,
            String requestBody) {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "OSV API returned HTTP "
                                + response.statusCode()
                );
            }

            return jsonMapper.readTree(
                    response.body()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to communicate with OSV API",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "OSV API request was interrupted",
                    e
            );
        }
    }

    /**
     * Validates Maven dependency coordinates.
     */
    private void validateDependency(
            String groupId,
            String artifactId,
            String version) {

        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException(
                    "Group ID cannot be empty"
            );
        }

        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException(
                    "Artifact ID cannot be empty"
            );
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Version cannot be empty"
            );
        }
    }

    /**
     * Escapes values before putting them into
     * JSON request bodies.
     */
    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}