/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package ai.platon.pulsar.sdk

/**
 * Reference to a DOM element, matching WebDriver element identifier.
 */
data class ElementRef(
    val elementId: String
)

/**
 * Represents a web page result from load/open operations.
 * Corresponds to the OpenAPI WebPageResult schema.
 */
data class WebPage(
    val url: String,
    val location: String? = null,
    val contentType: String? = null,
    val contentLength: Int = 0,
    val protocolStatus: String? = null,
    val isNil: Boolean = false,
    val html: String? = null
) {
    companion object {
        /**
         * Creates a WebPage from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): WebPage {
            return WebPage(
                url = data["url"] as? String ?: "",
                location = data["location"] as? String,
                contentType = data["contentType"] as? String,
                contentLength = (data["contentLength"] as? Number)?.toInt() ?: 0,
                protocolStatus = data["protocolStatus"] as? String,
                isNil = data["isNil"] as? Boolean ?: false,
                html = data["html"] as? String
            )
        }
    }
}

/**
 * Normalized URL result.
 * Corresponds to the OpenAPI NormalizeResponse schema.
 */
data class NormURL(
    val spec: String,
    val url: String,
    val args: String? = null,
    val isNil: Boolean = false
) {
    companion object {
        /**
         * Creates a NormURL from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): NormURL {
            return NormURL(
                spec = data["spec"] as? String ?: "",
                url = data["url"] as? String ?: "",
                args = data["args"] as? String,
                isNil = data["isNil"] as? Boolean ?: false
            )
        }
    }
}

/**
 * Result from agent run operation.
 * Corresponds to the OpenAPI AgentRunResponse schema.
 */
data class AgentRunResult(
    val success: Boolean = false,
    val message: String = "",
    val historySize: Int = 0,
    val processTraceSize: Int = 0,
    val finalResult: Any? = null,
    val trace: List<String>? = null
) {
    companion object {
        /**
         * Creates an AgentRunResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentRunResult {
            return AgentRunResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                historySize = (data["historySize"] as? Number)?.toInt() ?: 0,
                processTraceSize = (data["processTraceSize"] as? Number)?.toInt() ?: 0,
                finalResult = data["finalResult"],
                trace = data["trace"] as? List<String>
            )
        }
    }
}

/**
 * Result from agent act operation.
 * Corresponds to the OpenAPI AgentActResponse schema.
 */
data class AgentActResult(
    val success: Boolean = false,
    val message: String = "",
    val action: String? = null,
    val isComplete: Boolean = false,
    val expression: String? = null,
    val result: Any? = null,
    val trace: List<String>? = null
) {
    companion object {
        /**
         * Creates an AgentActResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentActResult {
            return AgentActResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                action = data["action"] as? String,
                isComplete = data["isComplete"] as? Boolean ?: false,
                expression = data["expression"] as? String,
                result = data["result"],
                trace = data["trace"] as? List<String>
            )
        }
    }
}

/**
 * Single observation result from agent observe operation.
 * Corresponds to the OpenAPI ObserveResult schema.
 */
data class ObserveResult(
    val locator: String? = null,
    val domain: String? = null,
    val method: String? = null,
    val arguments: Map<String, Any?>? = null,
    val description: String? = null,
    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val nextGoal: String? = null,
    val thinking: String? = null,
    val summary: String? = null,
    val keyFindings: String? = null,
    val nextSuggestions: List<String>? = null
) {
    companion object {
        /**
         * Creates an ObserveResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): ObserveResult {
            return ObserveResult(
                locator = data["locator"] as? String,
                domain = data["domain"] as? String,
                method = data["method"] as? String,
                arguments = data["arguments"] as? Map<String, Any?>,
                description = data["description"] as? String,
                screenshotContentSummary = data["screenshotContentSummary"] as? String,
                currentPageContentSummary = data["currentPageContentSummary"] as? String,
                nextGoal = data["nextGoal"] as? String,
                thinking = data["thinking"] as? String,
                summary = data["summary"] as? String,
                keyFindings = data["keyFindings"] as? String,
                nextSuggestions = data["nextSuggestions"] as? List<String>
            )
        }
    }
}

/**
 * Result from agent observe operation containing multiple observations.
 */
data class AgentObservation(
    val observations: List<ObserveResult> = emptyList()
) {
    companion object {
        /**
         * Creates an AgentObservation from an API response.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromAny(data: Any?): AgentObservation {
            if (data is List<*>) {
                val observations = data.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> ObserveResult.fromMap(item as Map<String, Any?>)
                        else -> null
                    }
                }
                return AgentObservation(observations)
            }
            return AgentObservation()
        }
    }
}

/**
 * Result from agent extract operation.
 * Corresponds to the OpenAPI AgentExtractResponse schema.
 */
data class ExtractionResult(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
) {
    companion object {
        /**
         * Creates an ExtractionResult from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): ExtractionResult {
            return ExtractionResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                data = data["data"]
            )
        }
    }
}

/**
 * Snapshot of a web page, used for capture operations.
 */
data class PageSnapshot(
    val url: String,
    val html: String? = null
)

/**
 * Result of field extraction with CSS selectors.
 */
data class FieldsExtraction(
    val fields: Map<String, Any?> = emptyMap()
)

/**
 * Result of a tool call execution.
 */
data class ToolCallResult(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
) {
    companion object {
        /**
         * Creates a ToolCallResult from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): ToolCallResult {
            return ToolCallResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                data = data["data"]
            )
        }
    }
}

/**
 * Description of an action to be performed.
 */
data class ActionDescription(
    val description: String,
    val parameters: Map<String, Any?>? = null
)

/**
 * Placeholder for page event handlers.
 * This class provides structure for event-driven page interactions.
 */
class PageEventHandlers {
    private val browseEventHandlers: MutableMap<String, Any> = mutableMapOf()
    private val loadEventHandlers: MutableMap<String, Any> = mutableMapOf()
    private val crawlEventHandlers: MutableMap<String, Any> = mutableMapOf()

    /**
     * Gets browse event handlers.
     */
    fun getBrowseEventHandlers(): Map<String, Any> = browseEventHandlers.toMap()

    /**
     * Gets load event handlers.
     */
    fun getLoadEventHandlers(): Map<String, Any> = loadEventHandlers.toMap()

    /**
     * Gets crawl event handlers.
     */
    fun getCrawlEventHandlers(): Map<String, Any> = crawlEventHandlers.toMap()
}
