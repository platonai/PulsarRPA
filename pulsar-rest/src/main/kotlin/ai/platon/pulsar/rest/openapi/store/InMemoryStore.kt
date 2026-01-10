package ai.platon.pulsar.rest.openapi.store

import ai.platon.pulsar.rest.openapi.dto.Event
import ai.platon.pulsar.rest.openapi.dto.EventConfig
import ai.platon.pulsar.rest.openapi.dto.EventConfigWithId
import ai.platon.pulsar.rest.openapi.dto.SubscriptionData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory storage for elements, event configurations, and subscriptions.
 * This store is used alongside SessionManager to track element IDs and event data.
 * Session lifecycle is managed by SessionManager; this store only provides
 * supplementary data storage keyed by session ID.
 */
@Component
class InMemoryStore {

    private val logger = LoggerFactory.getLogger(InMemoryStore::class.java)

    /**
     * Session supplementary data class holding elements, events, and subscriptions.
     */
    data class SessionSupplementaryData(
        val sessionId: String,
        val elements: MutableMap<String, ElementData> = ConcurrentHashMap(),
        val eventConfigs: MutableMap<String, EventConfigWithId> = ConcurrentHashMap(),
        val events: MutableList<Event> = Collections.synchronizedList(mutableListOf()),
        val subscriptions: MutableMap<String, SubscriptionData> = ConcurrentHashMap()
    )

    /**
     * Element data class holding element metadata.
     */
    data class ElementData(
        val elementId: String,
        val selector: String,
        val strategy: String = "css"
    )

    private val sessionData: MutableMap<String, SessionSupplementaryData> = ConcurrentHashMap()

    /**
     * Gets or creates supplementary data for a session.
     *
     * @param sessionId The session identifier.
     * @return The session supplementary data.
     */
    private fun getOrCreateSessionData(sessionId: String): SessionSupplementaryData {
        return sessionData.getOrPut(sessionId) {
            SessionSupplementaryData(sessionId = sessionId)
        }
    }

    /**
     * Generates an element ID by hashing the selector string.
     *
     * @param selector The selector string.
     * @return A unique element identifier based on the selector hash.
     */
    fun generateElementId(selector: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(selector.toByteArray())
        return hashBytes.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates or retrieves an element for a session.
     *
     * @param sessionId The session identifier.
     * @param selector The CSS selector or XPath.
     * @param strategy The selector strategy (css or xpath).
     * @return The element data.
     */
    fun getOrCreateElement(sessionId: String, selector: String, strategy: String = "css"): ElementData {
        val data = getOrCreateSessionData(sessionId)
        val elementId = generateElementId(selector)

        return data.elements.getOrPut(elementId) {
            val element = ElementData(
                elementId = elementId,
                selector = selector,
                strategy = strategy
            )
            logger.debug("Created element {} for selector: {}", elementId, selector)
            element
        }
    }

    /**
     * Retrieves an element by ID for a session.
     *
     * @param sessionId The session identifier.
     * @param elementId The element identifier.
     * @return The element data, or null if not found.
     */
    fun getElement(sessionId: String, elementId: String): ElementData? {
        return sessionData[sessionId]?.elements?.get(elementId)
    }

    /**
     * Adds an event configuration to a session.
     *
     * @param sessionId The session identifier.
     * @param eventConfig The event configuration.
     * @return The created config with ID.
     */
    fun addEventConfig(sessionId: String, eventConfig: EventConfig): EventConfigWithId {
        val data = getOrCreateSessionData(sessionId)
        val configId = UUID.randomUUID().toString()
        val configWithId = EventConfigWithId(
            configId = configId,
            eventType = eventConfig.eventType,
            enabled = eventConfig.enabled
        )
        data.eventConfigs[configId] = configWithId
        logger.debug("Added event config {} for session: {}", configId, sessionId)
        return configWithId
    }

    /**
     * Retrieves all event configurations for a session.
     *
     * @param sessionId The session identifier.
     * @return List of event configurations.
     */
    fun getEventConfigs(sessionId: String): List<EventConfigWithId> {
        return sessionData[sessionId]?.eventConfigs?.values?.toList() ?: emptyList()
    }

    /**
     * Adds an event to a session.
     *
     * @param sessionId The session identifier.
     * @param event The event to add.
     */
    fun addEvent(sessionId: String, event: Event) {
        val data = getOrCreateSessionData(sessionId)
        // events is already a synchronized list, so direct add is thread-safe
        data.events.add(event)
    }

    /**
     * Retrieves all events for a session.
     *
     * @param sessionId The session identifier.
     * @return List of events.
     */
    fun getEvents(sessionId: String): List<Event> {
        val events = sessionData[sessionId]?.events ?: return emptyList()
        // Synchronized iteration for thread safety with synchronizedList
        synchronized(events) {
            return events.toList()
        }
    }

    /**
     * Creates a subscription for a session.
     *
     * @param sessionId The session identifier.
     * @param eventTypes List of event types to subscribe to.
     * @return The subscription data.
     */
    fun createSubscription(sessionId: String, eventTypes: List<String>): SubscriptionData {
        val data = getOrCreateSessionData(sessionId)
        val subscriptionId = UUID.randomUUID().toString()
        val subscription = SubscriptionData(
            subscriptionId = subscriptionId,
            eventTypes = eventTypes
        )
        data.subscriptions[subscriptionId] = subscription
        logger.debug("Created subscription {} for session: {}", subscriptionId, sessionId)
        return subscription
    }

    /**
     * Cleans up supplementary data for a deleted session.
     *
     * @param sessionId The session identifier.
     */
    fun cleanupSession(sessionId: String) {
        sessionData.remove(sessionId)
        logger.debug("Cleaned up supplementary data for session: {}", sessionId)
    }
}
