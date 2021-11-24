package io.lighty.applications.bgp.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MainTest {

    private static Main app;

    @BeforeAll
    static void startApp() throws Exception {
        app = new Main();
        app.start(new String[]{});
    }

    @Test
    void appStartedTest() {
        assertTrue(app.isRunning());
    }

    @AfterAll
    static void stop() {
        app.stop();
        assertFalse(app.isRunning());
    }

}
