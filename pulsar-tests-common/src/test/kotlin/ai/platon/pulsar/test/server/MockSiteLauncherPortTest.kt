package ai.platon.pulsar.test.server

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Duration

@Tag("TestInfraCheck")
class MockSiteLauncherPortTest {

    @AfterEach
    fun tearDown() {
        MockSiteLauncher.stop()
    }

    @Test
    fun `start with explicit port enforces that port`() {
        val port = 19091
        MockSiteLauncher.start(port = port, enforcePort = true)
        val ready = MockSiteLauncher.awaitReady(Duration.ofSeconds(6))
        assertTrue(ready, "Server not ready on expected port $port")
        assertEquals(port, MockSiteLauncher.port(), "Launcher bound port should equal requested port")
    }

    @Test
    fun `demo site starter extracts port from url and starts server`() {
        val port = 19092
        val url = "http://localhost:$port/generated/tta/act/act-demo.html"
        DemoSiteStarter().start(url)
        val ready = MockSiteLauncher.awaitReady(Duration.ofSeconds(6))
        assertTrue(ready, "Mock site not ready on extracted port $port")
        assertEquals(port, MockSiteLauncher.port(), "DemoSiteStarter should start server on extracted port")
    }
}

