package com.youtube.live.interaction.config;

import com.youtube.core.testfixtures.support.DatabaseCleanup;
import com.youtube.core.testfixtures.support.TestPersistSupport;
import com.youtube.core.testfixtures.support.TestContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = com.youtube.live.interaction.InteractionTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.task.scheduling.enabled=false"}
)
@ActiveProfiles("websocket-test")
public abstract class WebSocketStompTest extends TestContainer {

    @LocalServerPort
    protected int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    protected TestPersistSupport testSupport;

    protected String wsUrl;

    @BeforeEach
    public void setUpWebSocket() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
        }

        wsUrl = String.format("ws://localhost:%d%s", port, WebSocketConfig.Destinations.WS_ENDPOINT);
    }

    @AfterEach
    public void cleanup() {
        databaseCleanup.execute();
    }
}
