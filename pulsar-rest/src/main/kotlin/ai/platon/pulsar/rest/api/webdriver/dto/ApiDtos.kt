package ai.platon.pulsar.rest.api.webdriver.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request to create a new WebDriver session.
 */
data class NewSessionRequest(
    val capabilities: Map<String, Any?>? = null
)

/**
 * Response after creating a new session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NewSessionResponse(
    val value: SessionValue
) {
    data class SessionValue(
        val sessionId: String,
        val capabilities: Map<String, Any?>? = null
    )
}

/**
 * Response with session details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SessionDetails(
    val value: SessionDetailsValue
) {
    data class SessionDetailsValue(
        val sessionId: String,
        val url: String? = null,
        val status: String = "active",
        val capabilities: Map<String, Any?>? = null
    )
}

/**
 * Generic WebDriver response wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebDriverResponse<T>(
    val value: T? = null
)

/**
 * Error response following WebDriver spec.
 */
data class ErrorResponse(
    val value: ErrorValue
) {
    data class ErrorValue(
        val error: String,
        val message: String,
        val stacktrace: String? = null
    )
}

/**
 * Request to set/navigate to a URL.
 */
data class SetUrlRequest(
    val url: String
)

/**
 * Response containing a URL.
 */
data class UrlResponse(
    val value: String?
)

/**
 * Selector reference for selector-first operations.
 */
data class SelectorRef(
    val selector: String,
    val strategy: String = "css"
)

/**
 * Request to wait for a selector.
 */
data class WaitForRequest(
    val selector: String,
    val strategy: String = "css",
    val timeout: Int = 30000
)

/**
 * Request to find element using WebDriver strategy.
 */
data class FindElementRequest(
    val using: String,
    val value: String
)

/**
 * WebDriver element reference.
 * Uses the W3C WebDriver element identifier key.
 */
data class ElementRef(
    @JsonProperty("element-6066-11e4-a52e-4f735466cecf")
    val elementId: String
)

/**
 * Response containing a single element.
 */
data class ElementResponse(
    val value: ElementRef
)

/**
 * Response containing multiple elements.
 */
data class ElementsResponse(
    val value: List<ElementRef>
)

/**
 * Response for existence check.
 */
data class ExistsResponse(
    val value: ExistsValue
) {
    data class ExistsValue(
        val exists: Boolean
    )
}

/**
 * Request to fill an input element.
 */
data class FillRequest(
    val selector: String,
    val strategy: String = "css",
    val value: String
)

/**
 * Request to press a key on an element.
 */
data class PressRequest(
    val selector: String,
    val strategy: String = "css",
    val key: String
)

/**
 * Response containing HTML content.
 */
data class HtmlResponse(
    val value: String?
)

/**
 * Response containing a screenshot.
 */
data class ScreenshotResponse(
    val value: String?
)

/**
 * Request to send keys to an element.
 */
data class SendKeysRequest(
    val text: String
)

/**
 * Response containing an attribute value.
 */
data class AttributeResponse(
    val value: String?
)

/**
 * Response containing text content.
 */
data class TextResponse(
    val value: String?
)

/**
 * Request to execute a script.
 */
data class ScriptRequest(
    val script: String,
    val args: List<Any?>? = null
)

/**
 * Response from script execution.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScriptResponse(
    val value: Any? = null
)

/**
 * Request to delay execution.
 */
data class DelayRequest(
    val ms: Int
)

/**
 * Event configuration.
 */
data class EventConfig(
    val eventType: String,
    val selector: String? = null,
    val enabled: Boolean = true
)

/**
 * Event configuration with ID.
 */
data class EventConfigWithId(
    val configId: String,
    val eventType: String,
    val enabled: Boolean = true
)

/**
 * Response for event config creation.
 */
data class EventConfigResponse(
    val value: EventConfigWithId
)

/**
 * Response for listing event configs.
 */
data class EventConfigsResponse(
    val value: List<EventConfigWithId>
)

/**
 * Event data.
 */
data class Event(
    val eventId: String,
    val eventType: String,
    val timestamp: Long,
    val data: Map<String, Any?>? = null
)

/**
 * Response for listing events.
 */
data class EventsResponse(
    val value: List<Event>
)

/**
 * Request to subscribe to events.
 */
data class SubscribeRequest(
    val eventTypes: List<String>
)

/**
 * Subscription data.
 */
data class SubscriptionData(
    val subscriptionId: String,
    val eventTypes: List<String>
)

/**
 * Response for subscription.
 */
data class SubscriptionResponse(
    val value: SubscriptionData
)
