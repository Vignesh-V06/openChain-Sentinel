package openchain_sentinel_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.AffectedPathMetrics;

@Service
public class AffectedPathService {

    private final Driver driver;
    private final String database;

    public AffectedPathService(
            Driver driver,
            @Value("${spring.data.neo4j.database:openchain-sentinel-db}")
            String database) {

        this.driver = driver;
        this.database = database;
    }

    public AffectedPathMetrics analyzeVulnerability(
            String projectId,
            String scanId,
            String vulnerabilityId) {

        try (Session session = driver.session(
                SessionConfig.builder()
                        .withDatabase(database)
                        .build())) {

            return session.executeRead(tx -> {

                /*
                 * --------------------------------------------------
                 * 1. Find every dependency path from the project
                 *    to the vulnerable version.
                 * --------------------------------------------------
                 *
                 * We use Version because that is the actual
                 * label in our Neo4j graph.
                 */
                List<Record> pathRecords = tx.run(
                        """
                        MATCH path =
                            (p:Project {projectId: $projectId})
                            -[:DEPENDS_ON*1..8]->
                            (version:Version)
                            -[:AFFECTED_BY {scanId: $scanId}]->
                            (v:Vulnerability {
                                vulnId: $vulnerabilityId
                            })
                        RETURN
                            length(path) - 1 AS dependencyDepth,
                            version.coordinate AS affectedPackage,
                            [n IN nodes(path) |
                                coalesce(
                                    n.coordinate,
                                    n.name,
                                    n.projectId,
                                    n.vulnId
                                )
                            ] AS dependencyPath
                        ORDER BY dependencyDepth ASC
                        """,
                        Values.parameters(
                                "projectId",
                                projectId,

                                "scanId",
                                scanId,

                                "vulnerabilityId",
                                vulnerabilityId
                        )
                ).list();

                /*
                 * --------------------------------------------------
                 * 2. Build dependency paths
                 * --------------------------------------------------
                 */

                List<List<String>> dependencyPaths =
                        new ArrayList<>();

                int minimumDepth = 0;

                for (Record record : pathRecords) {

                    int depth =
                            record.get("dependencyDepth")
                                    .asInt();

                    if (minimumDepth == 0
                            || depth < minimumDepth) {

                        minimumDepth = depth;
                    }

                    List<String> path =
                            record.get("dependencyPath")
                                    .asList(
                                            value ->
                                                    value.asString()
                                    );

                    dependencyPaths.add(path);
                }

                /*
                 * --------------------------------------------------
                 * 3. Count affected dependency paths
                 * --------------------------------------------------
                 */

                int affectedPathCount =
                        pathRecords.size();

                /*
                 * --------------------------------------------------
                 * 4. Count distinct affected components
                 * --------------------------------------------------
                 */

                int affectedComponentCount =
                        tx.run(
                                """
                                MATCH
                                    (p:Project {
                                        projectId: $projectId
                                    })
                                    -[:DEPENDS_ON*1..8]->
                                    (version:Version)
                                    -[:AFFECTED_BY {
                                        scanId: $scanId
                                    }]->
                                    (v:Vulnerability {
                                        vulnId: $vulnerabilityId
                                    })
                                RETURN count(
                                    DISTINCT version
                                ) AS affectedComponentCount
                                """,
                                Values.parameters(
                                        "projectId",
                                        projectId,

                                        "scanId",
                                        scanId,

                                        "vulnerabilityId",
                                        vulnerabilityId
                                )
                        )
                        .single()
                        .get("affectedComponentCount")
                        .asInt();

                /*
                 * --------------------------------------------------
                 * 5. Return all metrics required by Risk Engine
                 * --------------------------------------------------
                 */

                return new AffectedPathMetrics(
                        vulnerabilityId,
                        affectedPathCount,
                        affectedComponentCount,
                        minimumDepth,
                        dependencyPaths
                );
            });
        }
    }
}