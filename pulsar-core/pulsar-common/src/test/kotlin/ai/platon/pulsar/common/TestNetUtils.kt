
package ai.platon.pulsar.common

import java.net.InetAddress
import kotlin.test.*

/** Unit tests for StringUtil methods.  */
class TestNetUtils {
    @Test
    fun testUriBuilder() {
//    URI uri = UriBuilder.fromUri("http://www.163.com").path("q").queryParam("a", 1).queryParam("utf8", "✓").build();
//    assertEquals("http://www.163.com/q?a=1&utf8=%E2%9C%93", uri.toASCIIString());
    }

    @Test
    fun testLocalAddress() {
        val localHost = InetAddress.getLocalHost()
        println(localHost)
    }
}
