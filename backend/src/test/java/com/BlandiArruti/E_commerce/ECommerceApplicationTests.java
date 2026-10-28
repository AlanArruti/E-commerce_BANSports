package com.BlandiArruti.E_commerce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "jwt.secret=test-secret-key-long-enough-for-256-bits-hmac-sha256-ok",
        "mercadopago.access-token=TEST_TOKEN",
        "correo-argentino.user-token=test",
        "correo-argentino.password-token=test",
        "correo-argentino.customer-id=test",
        "spring.jpa.hibernate.ddl-auto=create"
})
@Testcontainers
@EnabledIf("dockerDisponible")
class ECommerceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void contextLoads() {
    }
}
