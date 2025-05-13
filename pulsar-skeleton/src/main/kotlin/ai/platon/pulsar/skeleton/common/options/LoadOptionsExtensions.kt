package ai.platon.pulsar.skeleton.common.options

import ai.platon.pulsar.common.config.VolatileConfig

/**
 * Extension functions for LoadOptions to support JSON serialization
 * 
 * These extension functions make it easier to convert LoadOptions objects
 * to and from JSON format using the LoadOptionsJson utility class.
 */

/**
 * Converts a LoadOptions object to a JSON string
 * 
 * @return A JSON representation of this LoadOptions object
 */
fun LoadOptions.toJson(): String {
    return LoadOptionsJson.toJson(this)
}

/**
 * Creates a LoadOptions object from a JSON string
 * 
 * @param json The JSON string to parse
 * @param conf Optional configuration to use (defaults to VolatileConfig.UNSAFE)
 * @return A new LoadOptions object with values from the JSON
 */
fun LoadOptions.Companion.fromJson(json: String, conf: VolatileConfig = VolatileConfig.UNSAFE): LoadOptions {
    return LoadOptionsJson.fromJson(json, conf)
} 