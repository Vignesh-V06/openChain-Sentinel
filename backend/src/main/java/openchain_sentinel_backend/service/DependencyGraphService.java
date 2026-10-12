package openchain_sentinel_backend.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyGraphService {

    private final Driver driver;

    private final String database;

    public DependencyGraphService(
            Driver driver,
            @Value("${spring.data.neo4j.database:openchain-sentinel-db}") String database) {

        this.driver = driver;
        this.database = database;
    }

    /**
     * Returns the complete dependency graph for one project + scan.
     *
     * Important:
     * - Project is the root node.
     * - Version nodes represent resolved dependency versions.
     * - DEPENDS_ON relationships are filtered by scanId.
     * - Vulnerability relationships are also filtered by scanId.
     * - Historical scan relationships are therefore not mixed.
     */
    public Map<String, Object> getDependencyGraph(
            String projectId,
            String scanId) {

        try (Session session = driver.session(
                SessionConfig.forDatabase(database))) {

            Map<String, Object> graph = session.executeRead(tx -> {

                /*
                 * -------------------------------------------------
                 * STEP 1
                 * Get project + every dependency path.
                 * -------------------------------------------------
                 *
                 * We deliberately retrieve paths instead of
                 * simply querying individual nodes.
                 *
                 * This allows us to reconstruct:
                 *
                 * Project
                 *      ↓
                 * Version
                 *      ↓
                 * Version
                 *      ↓
                 * Version
                 */

                String pathQuery = """
                        MATCH path =
                            (project:Project {projectId: $projectId})
                            -[deps:DEPENDS_ON*1..8]->
                            (version:Version)

                        WHERE all(
                            relationship IN deps
                            WHERE relationship.scanId = $scanId
                        )

                        RETURN path
                        """;

                List<Record> pathRecords = tx.run(
                        pathQuery,
                        Map.of(
                                "projectId", projectId,
                                "scanId", scanId))
                        .list();

                /*
                 * -------------------------------------------------
                 * Graph containers
                 * -------------------------------------------------
                 */

                Map<String, Map<String, Object>> nodes =
                        new LinkedHashMap<>();

                Map<String, Map<String, Object>> edges =
                        new LinkedHashMap<>();

                /*
                 * -------------------------------------------------
                 * STEP 2
                 * Always include the Project node.
                 * -------------------------------------------------
                 */

                String projectQuery = """
                        MATCH (project:Project {projectId: $projectId})
                        RETURN project
                        """;

                List<Record> projectRecords = tx.run(
                        projectQuery,
                        Map.of(
                                "projectId", projectId))
                        .list();

                if (!projectRecords.isEmpty()) {

                    Node projectNode = projectRecords.get(0)
                            .get("project")
                            .asNode();

                    addNode(
                            nodes,
                            projectNode,
                            "project",
                            0,
                            false);
                }

                /*
                 * -------------------------------------------------
                 * STEP 3
                 * Convert every dependency path into graph nodes
                 * and relationships.
                 * -------------------------------------------------
                 */

                for (Record record : pathRecords) {

                    Path path = record.get("path").asPath();

                    int depth = 0;

                    for (Node node : path.nodes()) {

                        String nodeId = String.valueOf(node.id());

                        boolean vulnerable = false;

                        /*
                         * We determine vulnerability later using
                         * AFFECTED_BY relationships.
                         */

                        String nodeType = getNodeType(node);

                        addNode(
                                nodes,
                                node,
                                nodeType,
                                depth,
                                vulnerable);

                        depth++;
                    }

                    for (Relationship relationship : path.relationships()) {

                        /*
                         * Only relationships belonging to the
                         * selected scan are allowed.
                         */

                        org.neo4j.driver.Value scanValue =
                                relationship.get("scanId");

                        if (!scanValue.isNull()
                                && !scanId.equals(
                                scanValue.asString())) {

                            continue;
                        }

                        String edgeId = String.valueOf(
                                relationship.id());

                        Map<String, Object> edge =
                                new LinkedHashMap<>();

                        edge.put(
                                "id",
                                edgeId);

                        edge.put(
                                "source",
                                String.valueOf(
                                        relationship.startNodeId()));

                        edge.put(
                                "target",
                                String.valueOf(
                                        relationship.endNodeId()));

                        edge.put(
                                "type",
                                relationship.type());

                        edge.put(
                                "scanId",
                                scanId);

                        edges.put(
                                edgeId,
                                edge);
                    }
                }

                /*
                 * -------------------------------------------------
                 * STEP 4
                 * Find vulnerable Version nodes.
                 * -------------------------------------------------
                 */

                String vulnerabilityQuery = """
                        MATCH
                            (version:Version)
                            -[affected:AFFECTED_BY]->
                            (vulnerability:Vulnerability)

                        WHERE affected.scanId = $scanId

                        RETURN version, vulnerability, affected
                        """;

                List<Record> vulnerabilityRecords = tx.run(
                        vulnerabilityQuery,
                        Map.of(
                                "scanId", scanId))
                        .list();

                Set<String> vulnerableVersionIds =
                        new HashSet<>();

                for (Record record : vulnerabilityRecords) {

                    Node version = record.get("version")
                            .asNode();

                    Node vulnerability = record.get("vulnerability")
                            .asNode();

                    Relationship affected = record.get("affected")
                            .asRelationship();

                    String versionId = String.valueOf(version.id());

                    /*
                     * Only mark the vulnerability if that version
                     * actually belongs to this project's graph.
                     */

                    if (nodes.containsKey(versionId)) {

                        vulnerableVersionIds.add(
                                versionId);

                        Map<String, Object> versionData =
                                nodes.get(versionId);

                        versionData.put(
                                "vulnerable",
                                true);

                        /*
                         * Add vulnerability node.
                         */

                        String vulnerabilityId = String.valueOf(
                                vulnerability.id());

                        Map<String, Object> vulnerabilityNode =
                                new LinkedHashMap<>();

                        vulnerabilityNode.put(
                                "id",
                                vulnerabilityId);

                        vulnerabilityNode.put(
                                "type",
                                "vulnerability");

                        vulnerabilityNode.put(
                                "label",
                                getStringProperty(
                                        vulnerability,
                                        "vulnId",
                                        vulnerabilityId));

                        vulnerabilityNode.put(
                                "vulnerabilityId",
                                getStringProperty(
                                        vulnerability,
                                        "vulnId",
                                        vulnerabilityId));

                        vulnerabilityNode.put(
                                "severity",
                                getStringProperty(
                                        vulnerability,
                                        "severity",
                                        "UNKNOWN"));

                        vulnerabilityNode.put(
                                "aliases",
                                getStringListProperty(
                                        vulnerability,
                                        "aliases"));

                        nodes.put(
                                vulnerabilityId,
                                vulnerabilityNode);

                        /*
                         * Add AFFECTED_BY edge.
                         */

                        String edgeId = "affected-" +
                                affected.id();

                        Map<String, Object> edge =
                                new LinkedHashMap<>();

                        edge.put(
                                "id",
                                edgeId);

                        edge.put(
                                "source",
                                versionId);

                        edge.put(
                                "target",
                                vulnerabilityId);

                        edge.put(
                                "type",
                                "AFFECTED_BY");

                        edge.put(
                                "scanId",
                                scanId);

                        edges.put(
                                edgeId,
                                edge);
                    }
                }

                /*
                 * -------------------------------------------------
                 * STEP 5
                 * Calculate statistics.
                 * -------------------------------------------------
                 */

                int dependencyCount = 0;
                int directDependencyCount = 0;
                int transitiveDependencyCount = 0;
                int vulnerableDependencyCount = 0;
                int maxDepth = 0;

                for (Map<String, Object> node : nodes.values()) {

                    String type = String.valueOf(
                            node.get("type"));

                    if (!"version".equals(type)) {
                        continue;
                    }

                    dependencyCount++;

                    int nodeDepth = ((Number) node.getOrDefault(
                            "depth",
                            0)).intValue();

                    maxDepth = Math.max(
                            maxDepth,
                            nodeDepth);

                    if (nodeDepth == 1) {
                        directDependencyCount++;
                    } else {
                        transitiveDependencyCount++;
                    }

                    boolean vulnerable = Boolean.TRUE.equals(
                            node.get("vulnerable"));

                    if (vulnerable) {
                        vulnerableDependencyCount++;
                    }
                }

                /*
                 * -------------------------------------------------
                 * Final response
                 * -------------------------------------------------
                 */

                Map<String, Object> result =
                        new LinkedHashMap<>();

                result.put(
                        "projectId",
                        projectId);

                result.put(
                        "scanId",
                        scanId);

                result.put(
                        "dependencyCount",
                        dependencyCount);

                result.put(
                        "directDependencyCount",
                        directDependencyCount);

                result.put(
                        "transitiveDependencyCount",
                        transitiveDependencyCount);

                result.put(
                        "vulnerableDependencyCount",
                        vulnerableDependencyCount);

                result.put(
                        "maxDepth",
                        maxDepth);

                result.put(
                        "nodes",
                        new ArrayList<>(
                                nodes.values()));

                result.put(
                        "edges",
                        new ArrayList<>(
                                edges.values()));

                return result;
            });

            return graph;
        }
    }

    /**
     * Add a Neo4j node to the response only once.
     */
    private void addNode(
            Map<String, Map<String, Object>> nodes,
            Node node,
            String type,
            int depth,
            boolean vulnerable) {

        String id = String.valueOf(node.id());

        if (nodes.containsKey(id)) {

            Map<String, Object> existing = nodes.get(id);

            int existingDepth = ((Number) existing.getOrDefault(
                    "depth",
                    Integer.MAX_VALUE)).intValue();

            if (depth < existingDepth) {
                existing.put(
                        "depth",
                        depth);
            }

            return;
        }

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                id);

        data.put(
                "type",
                type);

        data.put(
                "depth",
                depth);

        data.put(
                "vulnerable",
                vulnerable);

        if ("project".equals(type)) {

            data.put(
                    "projectId",
                    getStringProperty(
                            node,
                            "projectId",
                            ""));

            data.put(
                    "label",
                    getStringProperty(
                            node,
                            "name",
                            "Project"));

        } else if ("version".equals(type)) {

            String coordinate = getStringProperty(
                    node,
                    "coordinate",
                    "");

            data.put(
                    "coordinate",
                    coordinate);

            data.put(
                    "label",
                    coordinate);

            data.put(
                    "version",
                    getStringProperty(
                            node,
                            "version",
                            ""));

        } else {

            data.put(
                    "label",
                    getStringProperty(
                            node,
                            "name",
                            id));
        }

        nodes.put(
                id,
                data);
    }

    /**
     * Convert Neo4j labels to frontend node types.
     */
    private String getNodeType(Node node) {

        if (node.hasLabel("Project")) {
            return "project";
        }

        if (node.hasLabel("Version")) {
            return "version";
        }

        if (node.hasLabel("Package")) {
            return "package";
        }

        if (node.hasLabel("Vulnerability")) {
            return "vulnerability";
        }

        return "unknown";
    }

    private String getStringProperty(
            Node node,
            String property,
            String defaultValue) {

        if (!node.containsKey(property)) {
            return defaultValue;
        }

        org.neo4j.driver.Value value =
                node.get(property);

        if (value.isNull()) {
            return defaultValue;
        }

        return value.asString();
    }

    private List<String> getStringListProperty(
            Node node,
            String property) {

        if (!node.containsKey(property)) {
            return new ArrayList<>();
        }

        /*
         * IMPORTANT:
         * This must be Neo4j's Value, not Spring's @Value.
         */
        org.neo4j.driver.Value value =
                node.get(property);

        if (value.isNull()) {
            return new ArrayList<>();
        }

        try {
            return value.asList(
                    org.neo4j.driver.Value::asString);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }
}