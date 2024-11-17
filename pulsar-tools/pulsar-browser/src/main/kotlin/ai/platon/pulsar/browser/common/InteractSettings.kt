package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.Systems
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
     * The number of scroll downs on the page.
     * */
    var scrollCount: Int = 3,
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
    /**
     * Page positions to scroll to, these numbers are percentages of the total height,
     * e.g., 0.2 means to scroll to 20% of the height of the page.
     *
     * Some typical positions are:
     * * 0.3,0.75,0.4,0.5
     * * 0.2, 0.3, 0.5, 0.75, 0.5, 0.4, 0.5, 0.75
     * */
    var initScrollPositions: String = "0.3,0.75,0.4,0.5"
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
    
    /**
     * TODO: just use an InteractSettings object, instead of separate properties
     * */
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
        /**
         * TODO: just use an InteractSettings object, instead of separate properties
         * */
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
     * Build the scroll positions.
     * */
    fun buildScrollPositions(): List<Double> {
        val positions = buildInitScrollPositions().toMutableList()

        if (scrollCount <= 0) {
            return positions
        }

        val random = Random.nextInt(3)
        val enhancedScrollCount = (scrollCount + random - 1).coerceAtLeast(1)
        // some website show lazy content only when the page is in the front.
        repeat(enhancedScrollCount) { i ->
            val ratio = (0.6 + 0.1 * i).coerceAtMost(0.8)
            positions.add(ratio)
        }

        return positions
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
         * Default settings for Web page interaction behavior.
         * */
        val DEFAULT get() = InteractSettings()

        /**
         * Web page interaction behavior settings under good network conditions, in which case we perform
         * each action faster.
         * */
        val GOOD_NET_SETTINGS get() = InteractSettings()

        /**
         * Web page interaction behavior settings under worse network conditions, in which case we perform
         * each action more slowly.
         * */
        val WORSE_NET_SETTINGS get() = InteractSettings(
            scrollCount = 10,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofMinutes(2),
            Duration.ofMinutes(3),
        )

        /**
         * Web page interaction behavior settings under worst network conditions, in which case we perform
         * each action very slowly.
         * */
        val WORST_NET_SETTINGS get() = InteractSettings(
            scrollCount = 15,
            scrollInterval = Duration.ofSeconds(3),
            scriptTimeout = Duration.ofMinutes(3),
            Duration.ofMinutes(4),
        )
        
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