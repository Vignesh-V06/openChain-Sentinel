package openchain_sentinel_backend.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Neo4jConnectionService {

    private final Driver driver;
    private final String database;

    public Neo4jConnectionService(
            Driver driver,
            @Value("${spring.neo4j.database:openchain-sentinel-db}")
            String database) {

        this.driver = driver;
        this.database = database;
    }

    public String testConnection() {

        try (Session session = driver.session(
                SessionConfig.builder()
                        .withDatabase(database)
                        .build())) {

            return session.executeRead(transaction ->
                    transaction
                            .run("RETURN 'Neo4j connection successful!' AS message")
                            .single()
                            .get("message")
                            .asString()
            );
        }
    }
}