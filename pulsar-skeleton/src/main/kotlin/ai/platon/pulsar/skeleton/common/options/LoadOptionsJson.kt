package ai.platon.pulsar.skeleton.common.options

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.FetchMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * JSON Serialization/Deserialization support for LoadOptions
 * 
 * This class enables converting LoadOptions objects to and from JSON format for storage,
 * configuration sharing, and remote API communication.
 */
class LoadOptionsJson private constructor() {

    companion object {
        private val gson: Gson = createGson()

        /**
         * Creates a customized Gson instance with type adapters for LoadOptions-specific types
         */
        private fun createGson(): Gson {
            return GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .registerTypeAdapter(Duration::class.java, DurationTypeAdapter())
                .registerTypeAdapter(InteractLevel::class.java, InteractLevelTypeAdapter())
                .registerTypeAdapter(FetchMode::class.java, FetchModeTypeAdapter())
                .registerTypeAdapter(Condition::class.java, ConditionTypeAdapter())
                .registerTypeAdapter(BrowserType::class.java, BrowserTypeTypeAdapter())
                .create()
        }

        /**
         * Converts a LoadOptions object to JSON string
         * 
         * @param options The LoadOptions to serialize
         * @return A JSON string representation
         */
        fun toJson(options: LoadOptions): String {
            return gson.toJson(createJsonMap(options))
        }

        /**
         * Converts a JSON string to LoadOptions object
         * 
         * @param json The JSON string to deserialize
         * @param conf Configuration to use (optional)
         * @return The deserialized LoadOptions object
         */
        fun fromJson(json: String, conf: VolatileConfig = VolatileConfig.UNSAFE): LoadOptions {
            val jsonMap = gson.fromJson(json, Map::class.java)
            return mapToLoadOptions(jsonMap as Map<String, Any>, conf)
        }

        /**
         * Creates a map representation of LoadOptions for JSON serialization
         */
        private fun createJsonMap(options: LoadOptions): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            
            // Core properties
            map["entity"] = options.entity
            map["label"] = options.label
            map["taskId"] = options.taskId
            map["taskTime"] = options.taskTime
            map["deadline"] = options.deadline
            map["authToken"] = options.authToken
            map["readonly"] = options.readonly
            map["isResource"] = options.isResource
            map["priority"] = options.priority
            
            // Cache control
            map["expires"] = options.expires
            map["expireAt"] = options.expireAt
            map["refresh"] = options.refresh
            map["ignoreFailure"] = options.ignoreFailure
            
            // Link handling
            map["outLinkSelector"] = options.outLinkSelector
            map["outLinkPattern"] = options.outLinkPattern
            map["topLinks"] = options.topLinks
            
            // Page interaction
            map["interactLevel"] = options.interactLevel
            map["scrollCount"] = options.scrollCount
            map["scrollInterval"] = options.scrollInterval
            map["scriptTimeout"] = options.scriptTimeout
            map["pageLoadTimeout"] = options.pageLoadTimeout
            map["clickTarget"] = options.clickTarget
            map["nextPageSelector"] = options.nextPageSelector
            map["iframe"] = options.iframe
            map["waitNonBlank"] = options.waitNonBlank
            
            // Content validation
            map["requireNotBlank"] = options.requireNotBlank
            map["requireSize"] = options.requireSize
            map["requireImages"] = options.requireImages
            map["requireAnchors"] = options.requireAnchors
            
            // Fetch configuration
            map["fetchMode"] = options.fetchMode
            map["browser"] = options.browser
            map["nMaxRetry"] = options.nMaxRetry
            map["nJitRetry"] = options.nJitRetry
            map["incognito"] = options.incognito
            
            // Storage options
            map["persist"] = options.persist
            map["storeContent"] = options.storeContent
            map["dropContent"] = options.dropContent
            map["lazyFlush"] = options.lazyFlush
            
            // Parse options
            map["parse"] = options.parse
            map["reparseLinks"] = options.reparseLinks
            map["ignoreUrlQuery"] = options.ignoreUrlQuery
            map["noNorm"] = options.noNorm
            map["noFilter"] = options.noFilter
            
            // Item page specific options
            val itemOptions = mutableMapOf<String, Any?>()
            itemOptions["browser"] = options.itemBrowser
            itemOptions["expires"] = options.itemExpires
            itemOptions["expireAt"] = options.itemExpireAt
            itemOptions["scrollCount"] = options.itemScrollCount
            itemOptions["scrollInterval"] = options.itemScrollInterval
            itemOptions["scriptTimeout"] = options.itemScriptTimeout
            itemOptions["pageLoadTimeout"] = options.itemPageLoadTimeout
            itemOptions["waitNonBlank"] = options.itemWaitNonBlank
            itemOptions["requireNotBlank"] = options.itemRequireNotBlank
            itemOptions["requireSize"] = options.itemRequireSize
            itemOptions["requireImages"] = options.itemRequireImages
            itemOptions["requireAnchors"] = options.itemRequireAnchors
            map["itemOptions"] = itemOptions
            
            // Misc
            map["test"] = options.test
            map["version"] = options.version
            
            return map
        }

        /**
         * Converts a map to LoadOptions object
         */
        private fun mapToLoadOptions(map: Map<String, Any>, conf: VolatileConfig): LoadOptions {
            // Start with empty options
            val options = LoadOptions.createUnsafe()
            options.conf = conf
            
            // Apply values from map to options object
            map.forEach { (key, value) ->
                when (key) {
                    // Core properties
                    "entity" -> options.entity = value as String
                    "label" -> options.label = value as String
                    "taskId" -> options.taskId = value as String
                    "taskTime" -> options.taskTime = gson.fromJson(gson.toJson(value), Instant::class.java)
                    "deadline" -> options.deadline = gson.fromJson(gson.toJson(value), Instant::class.java)
                    "authToken" -> options.authToken = value as String
                    "readonly" -> options.readonly = value as Boolean
                    "isResource" -> options.isResource = value as Boolean
                    "priority" -> options.priority = (value as Double).toInt()
                    
                    // Cache control
                    "expires" -> options.expires = gson.fromJson(gson.toJson(value), Duration::class.java)
                    "expireAt" -> options.expireAt = gson.fromJson(gson.toJson(value), Instant::class.java)
                    "refresh" -> options.refresh = value as Boolean
                    "ignoreFailure" -> options.ignoreFailure = value as Boolean
                    
                    // Link handling
                    "outLinkSelector" -> options.outLinkSelector = value as String
                    "outLinkPattern" -> options.outLinkPattern = value as String
                    "topLinks" -> options.topLinks = (value as Double).toInt()
                    
                    // Page interaction
                    "interactLevel" -> options.interactLevel = gson.fromJson(gson.toJson(value), InteractLevel::class.java)
                    "scrollCount" -> options.scrollCount = (value as Double).toInt()
                    "scrollInterval" -> options.scrollInterval = gson.fromJson(gson.toJson(value), Duration::class.java)
                    "scriptTimeout" -> options.scriptTimeout = gson.fromJson(gson.toJson(value), Duration::class.java)
                    "pageLoadTimeout" -> options.pageLoadTimeout = gson.fromJson(gson.toJson(value), Duration::class.java)
                    "clickTarget" -> options.clickTarget = value as String
                    "nextPageSelector" -> options.nextPageSelector = value as String
                    "iframe" -> options.iframe = (value as Double).toInt()
                    "waitNonBlank" -> options.waitNonBlank = value as String
                    
                    // Content validation
                    "requireNotBlank" -> options.requireNotBlank = value as String
                    "requireSize" -> options.requireSize = (value as Double).toInt()
                    "requireImages" -> options.requireImages = (value as Double).toInt()
                    "requireAnchors" -> options.requireAnchors = (value as Double).toInt()
                    
                    // Fetch configuration
                    "fetchMode" -> options.fetchMode = gson.fromJson(gson.toJson(value), FetchMode::class.java)
                    "browser" -> options.browser = gson.fromJson(gson.toJson(value), BrowserType::class.java)
                    "nMaxRetry" -> options.nMaxRetry = (value as Double).toInt()
                    "nJitRetry" -> options.nJitRetry = (value as Double).toInt()
                    "incognito" -> options.incognito = value as Boolean
                    
                    // Storage options
                    "persist" -> options.persist = value as Boolean
                    "storeContent" -> options.storeContent = value as Boolean
                    "dropContent" -> options.dropContent = value as Boolean
                    "lazyFlush" -> options.lazyFlush = value as Boolean
                    
                    // Parse options
                    "parse" -> options.parse = value as Boolean
                    "reparseLinks" -> options.reparseLinks = value as Boolean
                    "ignoreUrlQuery" -> options.ignoreUrlQuery = value as Boolean
                    "noNorm" -> options.noNorm = value as Boolean
                    "noFilter" -> options.noFilter = value as Boolean
                    
                    // Item page specific options
                    "itemOptions" -> {
                        if (value is Map<*, *>) {
                            val itemOptions = value as Map<String, Any>
                            itemOptions["browser"]?.let { options.itemBrowser = gson.fromJson(gson.toJson(it), BrowserType::class.java) }
                            itemOptions["expires"]?.let { options.itemExpires = gson.fromJson(gson.toJson(it), Duration::class.java) }
                            itemOptions["expireAt"]?.let { options.itemExpireAt = gson.fromJson(gson.toJson(it), Instant::class.java) }
                            itemOptions["scrollCount"]?.let { options.itemScrollCount = (it as Double).toInt() }
                            itemOptions["scrollInterval"]?.let { options.itemScrollInterval = gson.fromJson(gson.toJson(it), Duration::class.java) }
                            itemOptions["scriptTimeout"]?.let { options.itemScriptTimeout = gson.fromJson(gson.toJson(it), Duration::class.java) }
                            itemOptions["pageLoadTimeout"]?.let { options.itemPageLoadTimeout = gson.fromJson(gson.toJson(it), Duration::class.java) }
                            itemOptions["waitNonBlank"]?.let { options.itemWaitNonBlank = it as String }
                            itemOptions["requireNotBlank"]?.let { options.itemRequireNotBlank = it as String }
                            itemOptions["requireSize"]?.let { options.itemRequireSize = (it as Double).toInt() }
                            itemOptions["requireImages"]?.let { options.itemRequireImages = (it as Double).toInt() }
                            itemOptions["requireAnchors"]?.let { options.itemRequireAnchors = (it as Double).toInt() }
                        }
                    }
                    
                    // Misc
                    "test" -> options.test = (value as Double).toInt()
                    "version" -> options.version = value as String
                }
            }
            
            return options
        }
    }
}

/**
 * Type adapter for Instant serialization/deserialization
 */
private class InstantTypeAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        return if (json.isJsonPrimitive) {
            Instant.parse(json.asString)
        } else {
            Instant.EPOCH
        }
    }
}

/**
 * Type adapter for Duration serialization/deserialization
 */
private class DurationTypeAdapter : JsonSerializer<Duration>, JsonDeserializer<Duration> {
    override fun serialize(src: Duration, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Duration {
        return if (json.isJsonPrimitive) {
            Duration.parse(json.asString)
        } else {
            Duration.ZERO
        }
    }
}

/**
 * Type adapter for InteractLevel serialization/deserialization
 */
private class InteractLevelTypeAdapter : JsonSerializer<InteractLevel>, JsonDeserializer<InteractLevel> {
    override fun serialize(src: InteractLevel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): InteractLevel {
        return if (json.isJsonPrimitive) {
            InteractLevel.valueOf(json.asString)
        } else {
            InteractLevel.DEFAULT
        }
    }
}

/**
 * Type adapter for FetchMode serialization/deserialization
 */
private class FetchModeTypeAdapter : JsonSerializer<FetchMode>, JsonDeserializer<FetchMode> {
    override fun serialize(src: FetchMode, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FetchMode {
        return if (json.isJsonPrimitive) {
            FetchMode.valueOf(json.asString)
        } else {
            FetchMode.BROWSER
        }
    }
}

/**
 * Type adapter for Condition serialization/deserialization
 */
private class ConditionTypeAdapter : JsonSerializer<Condition>, JsonDeserializer<Condition> {
    override fun serialize(src: Condition, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Condition {
        return if (json.isJsonPrimitive) {
            Condition.valueOf(json.asString)
        } else {
            Condition.GOOD
        }
    }
}

/**
 * Type adapter for BrowserType serialization/deserialization
 */
private class BrowserTypeTypeAdapter : JsonSerializer<BrowserType>, JsonDeserializer<BrowserType> {
    override fun serialize(src: BrowserType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BrowserType {
        return if (json.isJsonPrimitive) {
            BrowserType.valueOf(json.asString)
        } else {
            LoadOptionDefaults.browser
        }
    }
} 