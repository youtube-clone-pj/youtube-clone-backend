package com.youtube.api.config;

import com.youtube.core.testfixtures.support.DatabaseCleanup;
import com.youtube.core.testfixtures.support.TestPersistSupport;
import com.youtube.core.testfixtures.support.TestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = IntegrationTest.CoreTestConfiguration.class
)
@ActiveProfiles("api-test")
public abstract class IntegrationTest extends TestContainer {

    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"com.youtube.api", "com.youtube.core"})
    static class CoreTestConfiguration {
    }

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    public void cleanup() {
        databaseCleanup.execute();
    }

    @Autowired
    protected TestPersistSupport testSupport;
}
