package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Freezable
import java.util.concurrent.atomic.AtomicInteger

abstract class PrivacyContext: AutoCloseable, Freezable() {
    val privacyLeakWarnings = AtomicInteger()
    val isPrivacyLeaked get() = privacyLeakWarnings.get() > 3

    fun markSuccess() {
        if (privacyLeakWarnings.get() > 0) {
            privacyLeakWarnings.decrementAndGet()
        }
    }

    fun markWarning() {
        privacyLeakWarnings.incrementAndGet()
    }
}
