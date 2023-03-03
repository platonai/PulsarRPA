package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContextManagerTests {
    private val contextPathBase = Files.createTempDirectory("test-")
    private val contextPath = contextPathBase.resolve("cx.5kDMDS2")
    private val contextPath2 = contextPathBase.resolve("cx.7KmtAC2")
    private val conf = ImmutableConfig()
    private val driverPoolManager = DefaultWebDriverPoolManager(conf)

    @Test
    fun testPrivacyContextComparison() {
        val privacyManager = MultiPrivacyContextManager(driverPoolManager, conf)
        val fingerprint = Fingerprint(BrowserType.MOCK_CHROME)

        val pc = privacyManager.computeNextContext(fingerprint)
        assertTrue { pc.isActive }
        privacyManager.close(pc)
        assertTrue { !pc.isActive }

        val pc2 = privacyManager.computeNextContext(fingerprint)
        assertTrue { pc2.isActive }
        assertNotEquals(pc.id, pc2.id)
        assertNotEquals(pc, pc2)
    }
}
