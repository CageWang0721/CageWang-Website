package com.example.blog;

import java.util.stream.Stream;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared disposable infrastructure for Spring integration tests.
 *
 * <p>The containers are started once for the test JVM so the application
 * context can be safely reused across integration test classes.</p>
 */
abstract class ContainerIntegrationTest {

    private static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:9.7"))
                    .withDatabaseName("personal_blog")
                    .withUsername("blog")
                    .withPassword("blog_test_password");

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.8-alpine"))
                    .withExposedPorts(6379);

    static {
        Startables.deepStart(Stream.of(MYSQL, REDIS)).join();
    }

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");
        registry.add("blog.mail.enabled", () -> false);
    }
}
