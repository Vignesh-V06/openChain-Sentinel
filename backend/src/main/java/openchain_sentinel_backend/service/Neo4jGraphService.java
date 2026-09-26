package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.MavenDependency;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Neo4jGraphService {

    private final Driver driver;
    private final String database;

    public Neo4jGraphService(
            Driver driver,
            @Value("${spring.neo4j.database:openchain-sentinel-db}")
            String database) {

        this.driver = driver;
        this.database = database;
    }

    /**
     * Creates the complete dependency graph for one scan.
     *
     * Graph structure:
     *
     * Project
     *    |
     *    | DEPENDS_ON
     *    v
     * Version
     *    |
     *    | DEPENDS_ON
     *    v
     * Version
     *
     * Package
     *    |
     *    | HAS_VERSION
     *    v
     * Version
     */
    public void createDependencyGraph(
            String projectId,
            String projectName,
            String scanId,
            MavenAnalysisResult analysisResult) {

        if (analysisResult == null) {
            throw new IllegalArgumentException(
                    "Maven analysis result cannot be null"
            );
        }

        List<MavenDependency> dependencies =
                analysisResult.getDependencies();

        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        try (Session session = driver.session(
                SessionConfig.builder()
                        .withDatabase(database)
                        .build())) {

            // Step 1: Create / update the Project node.
            createProjectNode(
                    session,
                    projectId,
                    projectName
            );

            // Step 2: Create package/version nodes
            // and dependency relationships.
            for (MavenDependency dependency : dependencies) {

                createDependencyGraphEntry(
                        session,
                        projectId,
                        scanId,
                        dependency
                );
            }
        }
    }

    /**
     * Creates or updates the Project node.
     */
    private void createProjectNode(
            Session session,
            String projectId,
            String projectName) {

        String query = """
                MERGE (p:Project {
                    projectId: $projectId
                })
                SET p.name = $projectName
                """;

        Map<String, Object> parameters = Map.of(
                "projectId", projectId,
                "projectName", projectName
        );

        session.executeWriteWithoutResult(
                transaction ->
                        transaction.run(query, parameters).consume()
        );
    }

    /**
     * Creates the Package and Version nodes
     * for one Maven dependency.
     *
     * Also creates:
     *
     * Package -[:HAS_VERSION]-> Version
     *
     * and either:
     *
     * Project -[:DEPENDS_ON]-> Version
     *
     * or:
     *
     * ParentVersion -[:DEPENDS_ON]-> ChildVersion
     */
    private void createDependencyGraphEntry(
            Session session,
            String projectId,
            String scanId,
            MavenDependency dependency) {

        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        /*
         * A resolved Maven dependency should have
         * groupId, artifactId and version.
         *
         * Without these values we cannot create
         * a meaningful graph node.
         */
        if (groupId == null || groupId.isBlank()) {
            return;
        }

        if (artifactId == null || artifactId.isBlank()) {
            return;
        }

        if (version == null || version.isBlank()) {
            return;
        }

        String packageKey =
                groupId + ":" + artifactId;

        String coordinate =
                groupId + ":" + artifactId + ":" + version;

        /*
         * Step 1:
         * Create Package and Version nodes.
         *
         * Package:
         *   groupId:artifactId
         *
         * Version:
         *   groupId:artifactId:version
         */
        createPackageAndVersionNodes(
                session,
                groupId,
                artifactId,
                version,
                packageKey,
                coordinate
        );

        /*
         * Step 2:
         *
         * Direct dependency:
         *
         * Project -> Version
         *
         * Transitive dependency:
         *
         * Parent Version -> Child Version
         */
        if (dependency.isDirect()) {

            createProjectDependencyRelationship(
                    session,
                    projectId,
                    scanId,
                    coordinate
            );

        } else {

            String parentCoordinate =
                    dependency.getParentCoordinate();

            if (parentCoordinate == null
                    || parentCoordinate.isBlank()) {

                /*
                 * If the analyzer did not provide a parent
                 * coordinate, we cannot safely create the
                 * transitive edge.
                 *
                 * The dependency itself is still stored.
                 */
                return;
            }

            createTransitiveDependencyRelationship(
                    session,
                    scanId,
                    parentCoordinate,
                    coordinate
            );
        }
    }

    /**
     * Creates:
     *
     * (:Package)-[:HAS_VERSION]->`)
     */
    private void createPackageAndVersionNodes(
            Session session,
            String groupId,
            String artifactId,
            String version,
            String packageKey,
            String coordinate) {

        String query = """
                MERGE (pkg:Package {
                    packageKey: $packageKey
                })

                SET pkg.groupId = $groupId,
                    pkg.artifactId = $artifactId

                MERGE (v:Version {
                    coordinate: $coordinate
                })

                SET v.groupId = $groupId,
                    v.artifactId = $artifactId,
                    v.version = $version

                MERGE (pkg)-[:HAS_VERSION]->(v)
                """;

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("packageKey", packageKey);
        parameters.put("groupId", groupId);
        parameters.put("artifactId", artifactId);
        parameters.put("version", version);
        parameters.put("coordinate", coordinate);

        session.executeWriteWithoutResult(
                transaction ->
                        transaction.run(query, parameters).consume()
        );
    }

    /**
     * Creates the edge for a direct dependency:
     *
     * Project -[:DEPENDS_ON]-> Version
     */
    private void createProjectDependencyRelationship(
            Session session,
            String projectId,
            String scanId,
            String coordinate) {

        String query = """
                MATCH (p:Project {
                    projectId: $projectId
                })

                MATCH (v:Version {
                    coordinate: $coordinate
                })

                MERGE (p)-[r:DEPENDS_ON {
                    scanId: $scanId,
                    coordinate: $coordinate
                }]->(v)

                SET r.direct = true
                """;

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("projectId", projectId);
        parameters.put("scanId", scanId);
        parameters.put("coordinate", coordinate);

        session.executeWriteWithoutResult(
                transaction ->
                        transaction.run(query, parameters).consume()
        );
    }

    /**
     * Creates the edge for a transitive dependency:
     *
     * ParentVersion -[:DEPENDS_ON]-> ChildVersion
     */
    private void createTransitiveDependencyRelationship(
            Session session,
            String scanId,
            String parentCoordinate,
            String childCoordinate) {

        /*
         * parentCoordinate normally comes from the Maven
         * dependency tree and represents:
         *
         * groupId:artifactId:version
         */
        Coordinate parent = parseCoordinate(parentCoordinate);

        if (parent == null) {
            return;
        }

        String query = """
                MATCH (parent:Version {
                    coordinate: $parentCoordinate
                })

                MATCH (child:Version {
                    coordinate: $childCoordinate
                })

                MERGE (parent)-[r:DEPENDS_ON {
                    scanId: $scanId,
                    childCoordinate: $childCoordinate
                }]->(child)

                SET r.direct = false
                """;

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(
                "parentCoordinate",
                parent.toCoordinate()
        );

        parameters.put(
                "childCoordinate",
                childCoordinate
        );

        parameters.put(
                "scanId",
                scanId
        );

        session.executeWriteWithoutResult(
                transaction ->
                        transaction.run(query, parameters).consume()
        );
    }
            /**
         * Returns all direct dependencies of a project for a specific scan.
         *
         * Graph traversal:
         *
         * Project
         *    |
         *    | DEPENDS_ON {direct: true}
         *    ↓
         * Version
         */
        public List<Map<String, Object>> getDirectDependencies(
                String projectId,
                String scanId) {

            String query = """
                    MATCH (p:Project {projectId: $projectId})
                        -[r:DEPENDS_ON]->(v:Version)

                    WHERE r.scanId = $scanId
                    AND r.direct = true

                    RETURN
                        v.groupId AS groupId,
                        v.artifactId AS artifactId,
                        v.version AS version,
                        v.coordinate AS coordinate

                    ORDER BY v.groupId, v.artifactId
                    """;

            Map<String, Object> parameters = Map.of(
                    "projectId", projectId,
                    "scanId", scanId
            );

            try (Session session = driver.session(
                    SessionConfig.builder()
                            .withDatabase(database)
                            .build())) {

                return session.executeRead(transaction ->
                        transaction
                                .run(query, parameters)
                                .list(record -> Map.of(
                                        "groupId",
                                        record.get("groupId").asString(),

                                        "artifactId",
                                        record.get("artifactId").asString(),

                                        "version",
                                        record.get("version").asString(),

                                        "coordinate",
                                        record.get("coordinate").asString()
                                ))
                );
            }
        }


        /**
         * Returns all dependencies reachable from the project
         * for a specific scan.
         *
         * Example:
         *
         * Project
         *   ↓
         * A                    depth 1
         *   ↓
         * B                    depth 2
         *   ↓
         * C                    depth 3
         */
        public List<Map<String, Object>> getDependencyTree(
                String projectId,
                String scanId) {

            String query = """
                    MATCH path =
                        (p:Project {projectId: $projectId})
                        -[:DEPENDS_ON*1..]->
                        (v:Version)

                    WHERE all(
                        relationship IN relationships(path)
                        WHERE relationship.scanId = $scanId
                    )

                    RETURN DISTINCT
                        v.groupId AS groupId,
                        v.artifactId AS artifactId,
                        v.version AS version,
                        v.coordinate AS coordinate,
                        min(length(path)) AS depth

                    ORDER BY depth, groupId, artifactId
                    """;

            Map<String, Object> parameters = Map.of(
                    "projectId", projectId,
                    "scanId", scanId
            );

            try (Session session = driver.session(
                    SessionConfig.builder()
                            .withDatabase(database)
                            .build())) {

                return session.executeRead(transaction ->
                        transaction
                                .run(query, parameters)
                                .list(record -> Map.of(
                                        "groupId",
                                        record.get("groupId").asString(),

                                        "artifactId",
                                        record.get("artifactId").asString(),

                                        "version",
                                        record.get("version").asString(),

                                        "coordinate",
                                        record.get("coordinate").asString(),

                                        "depth",
                                        record.get("depth").asInt()
                                ))
                );
            }
        }


        /**
         * Finds every dependency path from the project
         * to a specific dependency version.
         *
         * This will later be used for:
         *
         * Vulnerability
         *      ↓
         * Vulnerable Version
         *      ↓
         * Neo4j traversal
         *      ↓
         * Affected dependency paths
         */
        public List<Map<String, Object>> getPathsToDependency(
                String projectId,
                String scanId,
                String coordinate) {

            String query = """
                    MATCH path =
                        (p:Project {projectId: $projectId})
                        -[:DEPENDS_ON*1..]->
                        (target:Version {
                            coordinate: $coordinate
                        })

                    WHERE all(
                        relationship IN relationships(path)
                        WHERE relationship.scanId = $scanId
                    )

                    RETURN
                        [
                            node IN nodes(path) |
                            CASE
                                WHEN node:Project
                                    THEN node.projectId
                                ELSE node.coordinate
                            END
                        ] AS dependencyPath,

                        length(path) AS depth

                    ORDER BY depth
                    """;

            Map<String, Object> parameters = Map.of(
                    "projectId", projectId,
                    "scanId", scanId,
                    "coordinate", coordinate
            );

            try (Session session = driver.session(
                    SessionConfig.builder()
                            .withDatabase(database)
                            .build())) {

                return session.executeRead(transaction ->
                        transaction
                                .run(query, parameters)
                                .list(record -> Map.of(
                                        "dependencyPath",
                                        record.get("dependencyPath")
                                                .asList(value -> value.asString()),

                                        "depth",
                                        record.get("depth").asInt()
                                ))
                );
            }
        }

    /**
     * Parses:
     *
     * groupId:artifactId:version
     *
     * into a Coordinate object.
     */
    private Coordinate parseCoordinate(String coordinate) {

        if (coordinate == null || coordinate.isBlank()) {
            return null;
        }

        String[] parts = coordinate.split(":");

        if (parts.length < 3) {
            return null;
        }

        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];

        if (groupId.isBlank()
                || artifactId.isBlank()
                || version.isBlank()) {

            return null;
        }

        return new Coordinate(
                groupId,
                artifactId,
                version
        );
    }

    /**
     * Small internal representation of a Maven coordinate.
     */
    private static class Coordinate {

        private final String groupId;
        private final String artifactId;
        private final String version;

        private Coordinate(
                String groupId,
                String artifactId,
                String version) {

            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        private String toCoordinate() {
            return groupId
                    + ":"
                    + artifactId
                    + ":"
                    + version;
        }
    }
}