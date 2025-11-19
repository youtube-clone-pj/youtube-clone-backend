package com.youtube.api.config;

import com.youtube.core.testfixtures.support.DatabaseCleanup;
import com.youtube.core.testfixtures.support.TestContainer;
import com.youtube.core.testfixtures.support.TestSupport;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-test")
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live", "com.youtube.notification"})
public class RestAssuredTest extends TestContainer {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    protected TestSupport testSupport;

    @BeforeEach
    public void setUp() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
        }
    }

    @AfterEach
    public void cleanup() {
        databaseCleanup.execute();
    }
}
