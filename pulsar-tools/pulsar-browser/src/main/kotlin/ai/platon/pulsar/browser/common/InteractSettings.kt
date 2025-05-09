package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.core.JacksonException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * The interaction settings.
 * */
data class InteractSettings(
    /**
     * Page positions to scroll to, these numbers are percentages of the total height,
     * e.g., 0.2 means to scroll to 20% of the height of the page.
     *
     * Some typical positions are:
     * * 0.3,0.75,0.4,0.5
     * * 0.2, 0.3, 0.5, 0.75, 0.5, 0.4, 0.5, 0.75
     * 0.3,0.75,0.4,0.5
     * */
    var initScrollPositions: String = "0.3,0.75",
    /**
     * The number of scroll downs on the page.
     * */
    var scrollCount: Int = 1,
    /**
     * The time interval to scroll down on the page.
     * */
    var scrollInterval: Duration = Duration.ofMillis(500),
    /**
     * Timeout for executing custom scripts on the page.
     * */
    var scriptTimeout: Duration = Duration.ofMinutes(1),
    /**
     * Timeout for loading a webpage.
     * */
    var pageLoadTimeout: Duration = Duration.ofMinutes(3),
    /**
     * Whether to bring the page to the front.
     * */
    var bringToFront: Boolean = false,
) {
    /**
     * The minimum delay time in milliseconds.
     * */
    var minDelayMillis = 100
    /**
     * The minimum delay time in milliseconds.
     * */
    var maxDelayMillis = 2000
    /**
     * The delay policy for each action.
     * The delay policy is a map from action to a range of delay time in milliseconds.
     * */
    var delayPolicy = mutableMapOf(
        "gap" to 200..700,
        "click" to 500..1500,
        "delete" to 30..80,
        "keyUpDown" to 50..150,
        "press" to 100..400,
        "type" to 50..550,
        "fill" to 10..50,
        "mouseWheel" to 800..1300,
        "dragAndDrop" to 800..1300,
        "waitForNavigation" to 500..1000,
        "waitForSelector" to 500..1000,
        "waitUntil" to 500..1000,
        "default" to 100..600,
        "" to 100..600
    )
    /**
     * The minimum delay time in milliseconds.
     * */
    var minTimeout = Duration.ofSeconds(1)
    /**
     * The minimum delay time in milliseconds.
     * */
    var maxTimeout = Duration.ofMinutes(3)
    /**
     * Timeout policy for each action in seconds.
     * */
    var timeoutPolicy = mutableMapOf(
        "pageLoad" to pageLoadTimeout,
        "script" to scriptTimeout,
        "waitForNavigation" to Duration.ofSeconds(60),
        "waitForSelector" to Duration.ofSeconds(60),
        "waitUntil" to Duration.ofSeconds(60),
        "default" to Duration.ofSeconds(60),
        "" to Duration.ofSeconds(60)
    )

    /**
     * The delay policy for each action.
     * The delay policy is a map from action to a range of delay time in milliseconds.
     *
     * The map should contain the following keys:
     * * gap
     * * click
     * * delete
     * * keyUpDown
     * * press
     * * type
     * * mouseWheel
     * * dragAndDrop
     * * waitForNavigation
     * * waitForSelector
     * * waitUntil
     * * default
     * * ""(empty key)
     *
     * @return a map from action to a range of delay time in milliseconds.
     * */
    fun generateRestrictedDelayPolicy(): Map<String, IntRange> {
        val fallback = (minDelayMillis..maxDelayMillis)
        
        delayPolicy.forEach { (action, delay) ->
            if (delay.first < minDelayMillis) {
                delayPolicy[action] = minDelayMillis..delay.last.coerceAtLeast(minDelayMillis)
            } else if (delay.last > maxDelayMillis) {
                delayPolicy[action] = delay.first.coerceAtMost(maxDelayMillis)..maxDelayMillis
            }
        }
        
        delayPolicy["default"] = delayPolicy["default"] ?: fallback
        delayPolicy[""] = delayPolicy["default"] ?: fallback
        
        return delayPolicy
    }
    
    /**
     * Timeout policy for each action.
     *
     * The map should contain the following keys:
     * * waitForNavigation
     * * waitForSelector
     * * waitUntil
     * * default
     * * ""(empty key)
     *
     * @return a map from action to a range of delay time in milliseconds.
     * */
    fun generateRestrictedTimeoutPolicy(): Map<String, Duration> {
        val fallback = Duration.ofSeconds(60)
        
        timeoutPolicy.forEach { (action, timeout) ->
            if (timeout < minTimeout) {
                timeoutPolicy[action] = minTimeout
            } else if (timeout > maxTimeout) {
                timeoutPolicy[action] = maxTimeout
            }
        }
        
        timeoutPolicy["default"] = timeoutPolicy["default"] ?: fallback
        timeoutPolicy[""] = timeoutPolicy["default"] ?: fallback
        
        return timeoutPolicy
    }

    fun overrideSystemProperties(): InteractSettings {
        Systems.setProperty(CapabilityTypes.BROWSER_INTERACT_SETTINGS, toJson())
        
        Systems.setProperty(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, scrollCount)
        Systems.setProperty(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        Systems.setProperty(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        Systems.setProperty(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
        
        return this
    }
    
    fun overrideConfiguration(conf: MutableConfig): InteractSettings {
        conf[CapabilityTypes.BROWSER_INTERACT_SETTINGS] = toJson()

        conf.setInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, scrollCount)
        conf.setDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        conf.setDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        conf.setDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
        
        return this
    }

    /**
     * Do not scroll the page by default.
     * */
    fun noScroll(): InteractSettings {
        initScrollPositions = ""
        scrollCount = 0
        return this
    }

    /**
     * Build the initial scroll positions.
     * */
    fun buildInitScrollPositions(): List<Double> {
        if (initScrollPositions.isBlank()) {
            return listOf()
        }

        return initScrollPositions.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    /**
     * Convert the object to a json string.
     *
     * @return a json string
     * */
    @Throws(JacksonException::class)
    fun toJson(): String {
        return pulsarObjectMapper().writeValueAsString(this)
    }

    companion object {
        private val OBJECT_CACHE = ConcurrentHashMap<String, InteractSettings>()

        /**
         * Interaction behavior to visit pages at fastest speed.
         * */
        val FASTEST get() = InteractSettings(
            scrollInterval = Duration.ofMillis(500),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(2),
            bringToFront = false
        ).noScroll()

        /**
         * Interaction behavior to visit pages at faster speed.
         * */
        val FASTER get() = InteractSettings(
            scrollCount = 0,
            scrollInterval = Duration.ofMillis(500),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(2),
            bringToFront = false,
            initScrollPositions = "0.2"
        )

        /**
         * Interaction behavior to visit pages at faster speed.
         * */
        val FAST get() = InteractSettings(
            scrollCount = 0,
            scrollInterval = Duration.ofMillis(500),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(2),
            bringToFront = false,
            initScrollPositions = "0.2,0.5"
        )

        /**
         * Default interaction behavior.
         * */
        val DEFAULT get() = InteractSettings()

        /**
         * Interaction behavior for good data.
         * */
        val GOOD_DATA get() = InteractSettings(
            scrollCount = 2,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(3),
            bringToFront = true,
            initScrollPositions = "0.3,0.75,0.4,0.5"
        )

        /**
         * Interaction behavior for better data.
         * */
        val BETTER_DATA get() = InteractSettings(
            scrollCount = 3,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(3),
            bringToFront = true,
            initScrollPositions = "0.3,0.75,0.4,0.5"
        )

        /**
         * Interaction behavior for best data.
         * */
        val BEST_DATA get() = InteractSettings(
            scrollCount = 5,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofSeconds(30),
            pageLoadTimeout = Duration.ofMinutes(3),
            bringToFront = true,
            initScrollPositions = "0.3,0.75,0.3,0.5,0.75"
        )

        fun create(level: InteractLevel): InteractSettings {
            return when  (level) {
                InteractLevel.FASTEST -> FASTEST
                InteractLevel.FASTER -> FASTER
                InteractLevel.FAST -> FAST
                InteractLevel.DEFAULT -> DEFAULT
                InteractLevel.GOOD_DATA -> GOOD_DATA
                InteractLevel.BETTER_DATA -> BETTER_DATA
                InteractLevel.BEST_DATA -> BEST_DATA
            }
        }

        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @return an InteractSettings object
         * */
        @Throws(JacksonException::class)
        fun fromJson(json: String): InteractSettings {
            return OBJECT_CACHE.computeIfAbsent(json) {
                pulsarObjectMapper().readValue(json, InteractSettings::class.java)
            }
        }

        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @param defaultValue the default value
         * @return an InteractSettings object
         * */
        fun fromJson(json: String?, defaultValue: InteractSettings): InteractSettings {
            return fromJsonOrNull(json) ?: defaultValue
        }
        
        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @return an InteractSettings object, or null if the json string is null, or the json string is invalid
         * */
        fun fromJsonOrNull(json: String?): InteractSettings? = json?.runCatching { fromJson(json) }?.getOrNull()
    }
}