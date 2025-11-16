package com.youtube.live.interaction.config;

import com.youtube.core.testfixtures.support.DatabaseCleanup;
import com.youtube.core.testfixtures.support.TestContainer;
import com.youtube.core.testfixtures.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = com.youtube.live.interaction.InteractionTestApplication.class
)
@ActiveProfiles("websocket-test")
public class IntegrationTest extends TestContainer {

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    public void cleanup() {
        databaseCleanup.execute();
    }

    @Autowired
    protected TestSupport testSupport;
}
