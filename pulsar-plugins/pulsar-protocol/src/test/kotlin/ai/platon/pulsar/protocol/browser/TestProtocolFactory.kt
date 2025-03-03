
package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolFactory
import kotlin.test.*

/**
 * Unit test for new protocol plugin.
 */
class TestProtocolFactory {
    private var conf = ImmutableConfig()
    private var protocolFactory = ProtocolFactory(conf)
    /**
     * Inits the Test Case with the test parse-plugin file
     */
    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
    }

    @Test
    @Throws(Exception::class)
    fun testGetProtocol() {
        //        assertEquals(Http.class.getName(),
//                protocolFactory.getProtocol("http://example.com").getClass().getName());
//        assertEquals(Http.class.getName(),
//                protocolFactory.getProtocol("https://example.com").getClass().getName());
        assertEquals(ForwardingProtocol::class.java.name,
                protocolFactory.getProtocol("crowd:http://example.com")?.javaClass?.name)
        assertEquals(
            BrowserEmulatorProtocol::class.java.name,
                protocolFactory.getProtocol("browser:http://example.com")?.javaClass?.name)
    }
}
