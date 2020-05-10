package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.PulsarSession
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

open class Crawler(
        val session: PulsarSession = PulsarContext.createSession(),
        val autoClose: Boolean = true
): AutoCloseable {
    val log = LoggerFactory.getLogger(Crawler::class.java)
    val closed = AtomicBoolean()
    val isAlive = !closed.get() && PulsarEnv.isActive

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            session.use { it.close() }
            // PulsarEnv.shutdown()
        }
    }
}
