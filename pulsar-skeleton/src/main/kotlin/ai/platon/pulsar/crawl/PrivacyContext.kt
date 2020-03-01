package ai.platon.pulsar.crawl

interface PrivacyContext: AutoCloseable {
    val isPrivacyLeaked: Boolean
}
