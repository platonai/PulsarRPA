package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import org.junit.jupiter.api.Assertions.*

class PrivacyAgentTest {
    @org.junit.jupiter.api.Test
    fun testGetFingerprint() {
        val privacyAgent = PrivacyAgent.createRandomTemp()
        val fingerprint = privacyAgent.fingerprint
        assertNotNull(fingerprint)
    }
    @org.junit.jupiter.api.Test
    fun testGetId() {
        val privacyAgent = PrivacyAgent.createRandomTemp()
        val id = privacyAgent.id
        assertNotNull(id)
    }
    
    @org.junit.jupiter.api.Test
    fun testToJSON() {
        val privacyAgent = PrivacyAgent.createRandomTemp()
        privacyAgent.fingerprint = Fingerprint.EXAMPLE
        val json = prettyPulsarObjectMapper().writeValueAsString(privacyAgent)
        val obj = prettyPulsarObjectMapper().readValue(json, PrivacyAgent::class.java)
        assertEquals(privacyAgent, obj)
    }
}
