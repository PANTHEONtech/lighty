package io.lighty.applications.bgp.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MainTest {

    private static Main app;

    @BeforeAll
    static void startApp() throws InterruptedException, ExecutionException, TimeoutException, ConfigurationException {
        app = new Main();
        app.start(new String[]{});
    }

    @Test
    void appStartedTest() {
        assertNotNull(app.controller);
        assertNotNull(app.restconf);
        assertNotNull(app.bgpModule);
    }

    @AfterAll
    static void stop() {
        app.stop();
    }

}
