package ai.platon.pulsar.persist

import kotlin.test.*
import kotlin.test.assertTrue

class TestProtocolStatus {

    @Test
    fun testRetry() {
        val status = ProtocolStatus.retry(RetryScope.PRIVACY, Exception())
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception()))
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception::class.java))

        val e = Exception()
        logPrintln(e.javaClass.name)
        logPrintln(Exception::javaClass.toString())
    }
}
