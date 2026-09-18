/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.gomsbook.ai.api.health;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import io.qdrant.client.QdrantClient;
import kr.co.goms.gomsbook.ai.rag.vector.qdrant.QdrantConfiguration;

@Component
public class QdrantHealthIndicator implements HealthIndicator {

    private final QdrantClient qdrantClient;
    private final QdrantConfiguration configuration;

    public QdrantHealthIndicator(QdrantClient qdrantClient, QdrantConfiguration configuration) {

        this.qdrantClient = qdrantClient;
        this.configuration = configuration;
    }

    @Override
    public Health health() {

        try {

            qdrantClient.healthCheckAsync().get(3, TimeUnit.SECONDS);

            boolean collectionExists = qdrantClient.collectionExistsAsync(configuration.getCollectionName()).get(3, TimeUnit.SECONDS);

            return Health.up().withDetail("host", configuration.getHost()).withDetail("grpcPort", configuration.getGrpcPort()).withDetail("collection", configuration.getCollectionName()).withDetail("collectionExists", collectionExists).build();

        } catch (Exception exception) {

            return Health.down(exception).withDetail("host", configuration.getHost()).withDetail("grpcPort", configuration.getGrpcPort()).withDetail("collection", configuration.getCollectionName()).build();
        }
    }
}