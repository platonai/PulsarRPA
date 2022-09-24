package ai.platon.pulsar.persist

import org.junit.Test
import kotlin.test.assertTrue

class TestProtocolStatus {

    @Test
    fun testRetry() {
        val status = ProtocolStatus.retry(RetryScope.PRIVACY, Exception())
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception()))
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception::class.java))

        val e = Exception()
        println(e.javaClass.name)
        println(Exception::javaClass.toString())
    }
}
