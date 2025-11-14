package ai.platon.pulsar.persist

import ai.platon.pulsar.common.printlnPro
import kotlin.test.*
import kotlin.test.assertTrue

class TestProtocolStatus {

    @Test
    fun testRetry() {
        val status = ProtocolStatus.retry(RetryScope.PRIVACY, Exception())
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception()))
        assertTrue(status.isRetry(RetryScope.PRIVACY, Exception::class.java))

        val e = Exception()
        printlnPro(e.javaClass.name)
        printlnPro(Exception::javaClass.toString())
    }
}
