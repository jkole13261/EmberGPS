package com.embergps;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads correctly.
 * Uses an H2 in-memory database so no external PostgreSQL is required.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin-api-key=test-admin-key",
        "app.cors.allowed-origins=http://localhost"
})
class EmberGpsApplicationTests {

    @Test
    void contextLoads() {
        // If the application context starts without error the test passes.
    }
}
