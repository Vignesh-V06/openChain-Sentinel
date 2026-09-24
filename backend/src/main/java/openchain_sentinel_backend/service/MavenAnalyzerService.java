package openchain_sentinel_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import openchain_sentinel_backend.model.GitHubRepositoryMetadata;
import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.MavenDependency;
import openchain_sentinel_backend.model.MavenProject;
import openchain_sentinel_backend.model.PomFileInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MavenAnalyzerService {

    private static final String DEPENDENCY_PLUGIN =
            "org.apache.maven.plugins:" +
            "maven-dependency-plugin:3.11.0:tree";

    private static final String OUTPUT_FILE =
            "target/openchain-sentinel-dependency-tree.json";

    private final GitHubService gitHubService;
    private final MavenPomParser mavenPomParser;
    private final JsonMapper jsonMapper;
    

    private final HttpClient httpClient;

    public MavenAnalyzerService(
            GitHubService gitHubService,
            MavenPomParser mavenPomParser,
            JsonMapper jsonMapper) {

        this.gitHubService = gitHubService;
        this.mavenPomParser = mavenPomParser;
        this.jsonMapper = jsonMapper;

        this.httpClient =
                HttpClient.newBuilder()
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();
    }

    // -----------------------------------------
    // MAIN ANALYSIS
    // -----------------------------------------

    public MavenAnalysisResult analyze(
            String repositoryUrl) {

        GitHubRepositoryMetadata metadata =
                gitHubService.getRepositoryMetadata(
                        repositoryUrl
                );

        List<PomFileInfo> pomFiles =
                gitHubService.findPomFiles(
                        metadata
                );

        if (pomFiles.isEmpty()) {

            throw new IllegalStateException(
                    "No pom.xml file found in repository"
            );
        }

        Path workspace = null;

        try {

            workspace =
                    Files.createTempDirectory(
                            "openchain-sentinel-"
                    );

            Path repositoryRoot =
                    downloadAndExtractRepository(
                            metadata,
                            workspace
                    );

            MavenAnalysisResult result =
                    new MavenAnalysisResult();

            result.setRepository(metadata);
            result.setPomFiles(pomFiles);

            // ---------------------------------
            // Parse discovered POM files
            // ---------------------------------

            for (PomFileInfo pomFile : pomFiles) {

                Path localPom =
                        repositoryRoot.resolve(
                                pomFile.getPath()
                        );

                if (!Files.exists(localPom)) {
                    continue;
                }

                String pomXml =
                        Files.readString(
                                localPom
                        );

                MavenProject project =
                        mavenPomParser.parse(
                                pomXml,
                                pomFile.getPath()
                        );

                result.getMavenProjects()
                        .add(project);
            }

            // ---------------------------------
            // Run Maven dependency analysis
            // ---------------------------------

            Path analysisPom =
                    chooseAnalysisPom(
                            repositoryRoot,
                            pomFiles
                    );

            runMavenDependencyTree(
                    repositoryRoot,
                    analysisPom
            );

            // ---------------------------------
            // Read generated JSON files
            // ---------------------------------

            List<Path> dependencyReports =
                    findDependencyReports(
                            repositoryRoot
                    );

            if (dependencyReports.isEmpty()) {

                throw new IllegalStateException(
                        "Maven did not generate a dependency tree report"
                );
            }

            Set<String> seenDependencies =
                    new HashSet<>();

            for (Path report :
                    dependencyReports) {

                String pomPath =
                        repositoryRoot
                                .relativize(
                                        report.getParent()
                                                .getParent()
                                )
                                .toString()
                                .replace(
                                        "\\",
                                        "/"
                                );

                parseDependencyReport(
                        report,
                        pomPath,
                        result,
                        seenDependencies
                );
            }

            result.setDirectDependencyCount(
                    (int) result.getDependencies()
                            .stream()
                            .filter(
                                    MavenDependency::isDirect
                            )
                            .count()
            );

            result.setTransitiveDependencyCount(
                    (int) result.getDependencies()
                            .stream()
                            .filter(
                                    dependency ->
                                            !dependency.isDirect()
                            )
                            .count()
            );

            return result;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Maven analysis failed: "
                            + e.getMessage(),
                    e
            );

        } finally {

            deleteDirectory(workspace);
        }
    }

    // -----------------------------------------
    // DOWNLOAD + EXTRACT REPOSITORY
    // -----------------------------------------

    private Path downloadAndExtractRepository(
            GitHubRepositoryMetadata metadata,
            Path workspace)
            throws IOException, InterruptedException {

        String url =
                "https://api.github.com/repos/"
                        + metadata.getOwner()
                        + "/"
                        + metadata.getRepository()
                        + "/zipball";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Accept",
                                "application/vnd.github+json"
                        )
                        .header(
                                "User-Agent",
                                "OpenChain-Sentinel"
                        )
                        .GET()
                        .build();

        HttpResponse<InputStream> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

        if (response.statusCode() != 200) {

            throw new IllegalStateException(
                    "GitHub repository download failed. HTTP "
                            + response.statusCode()
            );
        }

        Path zipFile =
                workspace.resolve(
                        "repository.zip"
                );

        try (InputStream input =
                     response.body()) {

            Files.copy(
                    input,
                    zipFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        Path extractDirectory =
                workspace.resolve(
                        "repository"
                );

        Files.createDirectories(
                extractDirectory
        );

        unzip(
                zipFile,
                extractDirectory
        );

        /*
         * GitHub archives normally contain one
         * top-level directory such as:
         *
         * Vignesh-V06-openChain-Sentinel-abc123/
         *
         * We locate it instead of assuming its name.
         */

        try (var stream =
                     Files.list(extractDirectory)) {

            List<Path> directories =
                    stream
                            .filter(
                                    Files::isDirectory
                            )
                            .toList();

            if (directories.size() == 1) {
                return directories.get(0);
            }
        }

        return extractDirectory;
    }

    // -----------------------------------------
    // SAFE ZIP EXTRACTION
    // -----------------------------------------

    private void unzip(
            Path zipFile,
            Path destination)
            throws IOException {

        Path normalizedDestination =
                destination
                        .toAbsolutePath()
                        .normalize();

        try (
                InputStream input =
                        Files.newInputStream(zipFile);

                ZipInputStream zip =
                        new ZipInputStream(input)
        ) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry())
                    != null) {

                Path target =
                        normalizedDestination
                                .resolve(
                                        entry.getName()
                                )
                                .normalize();

                /*
                 * Prevent ZIP path traversal.
                 */

                if (!target.startsWith(
                        normalizedDestination)) {

                    throw new IllegalStateException(
                            "Unsafe ZIP entry detected: "
                                    + entry.getName()
                    );
                }

                if (entry.isDirectory()) {

                    Files.createDirectories(
                            target
                    );

                } else {

                    Files.createDirectories(
                            target.getParent()
                    );

                    try (
                            OutputStream output =
                                    Files.newOutputStream(
                                            target
                                    )
                    ) {

                        zip.transferTo(output);
                    }
                }

                zip.closeEntry();
            }
        }
    }

    // -----------------------------------------
    // CHOOSE ANALYSIS POM
    // -----------------------------------------

    private Path chooseAnalysisPom(
            Path repositoryRoot,
            List<PomFileInfo> pomFiles) {

        /*
         * Root pom gets priority.
         */

        for (PomFileInfo pom :
                pomFiles) {

            if ("pom.xml".equals(
                    pom.getPath())) {

                return repositoryRoot.resolve(
                        pom.getPath()
                );
            }
        }

        /*
         * If only one POM exists,
         * analysis is unambiguous.
         */

        if (pomFiles.size() == 1) {

            return repositoryRoot.resolve(
                    pomFiles.get(0).getPath()
            );
        }

        /*
         * Multiple nested POMs without a
         * root aggregator require more
         * project-selection logic.
         */

        throw new IllegalStateException(
                "Multiple Maven POMs found without " +
                "a root pom.xml. " +
                "Maven project selection is required."
        );
    }

    // -----------------------------------------
    // RUN MAVEN
    // -----------------------------------------

    private void runMavenDependencyTree(
            Path repositoryRoot,
            Path analysisPom)
            throws IOException, InterruptedException {

        Path mavenWrapper =
                repositoryRoot.resolve(
                        isWindows()
                                ? "mvnw.cmd"
                                : "mvnw"
                );

        String mavenCommand;

        if (Files.exists(mavenWrapper)) {

            mavenCommand =
                    mavenWrapper.toString();

        } else {

            mavenCommand =
                    isWindows()
                            ? "mvn.cmd"
                            : "mvn";
        }

        Path pomDirectory =
                analysisPom.getParent();

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        mavenCommand,
                        "-B",
                        "-f",
                        analysisPom.toString(),
                        DEPENDENCY_PLUGIN,
                        "-DoutputType=json",
                        "-DoutputFile="
                                + OUTPUT_FILE
                );

        processBuilder
                .directory(
                        pomDirectory.toFile()
                );

        processBuilder
                .redirectErrorStream(true);

        Process process =
                processBuilder.start();

        String output;

        try (InputStream input =
                     process.getInputStream()) {

            output =
                    new String(
                            input.readAllBytes(),
                            StandardCharsets.UTF_8
                    );
        }

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IllegalStateException(
                    "Maven dependency analysis failed.\n"
                            + output
            );
        }
    }

    // -----------------------------------------
    // FIND JSON REPORTS
    // -----------------------------------------

    private List<Path> findDependencyReports(
            Path repositoryRoot)
            throws IOException {

        try (var stream =
                     Files.walk(repositoryRoot)) {

            return stream
                    .filter(
                            Files::isRegularFile
                    )
                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .equals(
                                                    "openchain-sentinel-dependency-tree.json"
                                            )
                    )
                    .toList();
        }
    }

    // -----------------------------------------
    // PARSE DEPENDENCY JSON
    // -----------------------------------------

    private void parseDependencyReport(
            Path report,
            String pomPath,
            MavenAnalysisResult result,
            Set<String> seenDependencies)
            throws IOException {

        JsonNode root =
                jsonMapper.readTree(
                        report.toFile()
                );

        if (root == null
                || root.isMissingNode()) {

            return;
        }

        String rootCoordinate =
                coordinate(root);

        JsonNode children =
                root.get("children");

        if (children == null
                || !children.isArray()) {

            return;
        }

        for (JsonNode child :
                children) {

            flattenDependency(
                    child,
                    1,
                    rootCoordinate,
                    List.of(rootCoordinate),
                    pomPath,
                    result,
                    seenDependencies
            );
        }
    }

    // -----------------------------------------
    // FLATTEN TREE
    // -----------------------------------------

    private void flattenDependency(
            JsonNode node,
            int depth,
            String parentCoordinate,
            List<String> currentPath,
            String pomPath,
            MavenAnalysisResult result,
            Set<String> seenDependencies) {

        String currentCoordinate =
                coordinate(node);

        List<String> dependencyPath =
                new ArrayList<>(
                        currentPath
                );

        dependencyPath.add(
                currentCoordinate
        );

        MavenDependency dependency =
                new MavenDependency();

        dependency.setGroupId(
                text(node, "groupId")
        );

        dependency.setArtifactId(
                text(node, "artifactId")
        );

        dependency.setVersion(
                text(node, "version")
        );

        dependency.setType(
                text(node, "type")
        );

        dependency.setScope(
                text(node, "scope")
        );

        dependency.setClassifier(
                text(node, "classifier")
        );

        dependency.setOptional(
                booleanValue(
                        node,
                        "optional"
                )
        );

        dependency.setDepth(depth);

        dependency.setDirect(
                depth == 1
        );

        dependency.setParentCoordinate(
                parentCoordinate
        );

        dependency.setPomPath(
                pomPath
        );

        dependency.setDependencyPath(
                dependencyPath
        );

        /*
         * A dependency may appear through multiple
         * paths. We preserve every path later,
         * but avoid inserting the exact same
         * dependency/path combination twice.
         */

        String uniqueKey =
                pomPath
                        + "|"
                        + String.join(
                                "->",
                                dependencyPath
                        );

        if (seenDependencies.add(
                uniqueKey
        )) {

            result.getDependencies()
                    .add(dependency);
        }

        JsonNode children =
                node.get("children");

        if (children == null
                || !children.isArray()) {

            return;
        }

        for (JsonNode child :
                children) {

            flattenDependency(
                    child,
                    depth + 1,
                    currentCoordinate,
                    dependencyPath,
                    pomPath,
                    result,
                    seenDependencies
            );
        }
    }

    // -----------------------------------------
    // JSON HELPERS
    // -----------------------------------------

    private String coordinate(
            JsonNode node) {

        String groupId =
                text(node, "groupId");

        String artifactId =
                text(node, "artifactId");

        String version =
                text(node, "version");

        return groupId
                + ":"
                + artifactId
                + ":"
                + version;
    }

    private String text(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {

            return null;
        }

        return value.asText();
    }

    private boolean booleanValue(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {

            return false;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        return Boolean.parseBoolean(
                value.asText()
        );
    }

    // -----------------------------------------
    // CLEAN TEMP DIRECTORY
    // -----------------------------------------

    private void deleteDirectory(
            Path directory) {

        if (directory == null) {
            return;
        }

        try {

            if (!Files.exists(directory)) {
                return;
            }

            try (var stream =
                         Files.walk(directory)) {

                stream
                        .sorted(
                                Comparator.reverseOrder()
                        )
                        .forEach(
                                path -> {

                                    try {
                                        Files.deleteIfExists(
                                                path
                                        );
                                    } catch (IOException ignored) {
                                    }
                                }
                        );
            }

        } catch (IOException ignored) {
        }
    }

    private boolean isWindows() {

        return System
                .getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }
}