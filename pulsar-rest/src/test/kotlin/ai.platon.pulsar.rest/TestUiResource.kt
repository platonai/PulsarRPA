package ai.platon.pulsar.rest

import org.junit.Test
import kotlin.test.assertTrue

class TestUiResource : ResourceTestBase() {

    @Test
    fun testWsVersion() {
        val version = javax.ws.rs.core.UriBuilder::class.java.getPackage().toString()
        println(version)
        assertTrue(version.contains("package javax.ws.rs.core, version 2.0"))
    }

    @Test
    fun testHome() {
        val greeting = target("/p").request().get(String::class.java)
        // assertTrue(greeting.startsWith("<html"))
        assertTrue(greeting.endsWith("</html>"))
    }
}
