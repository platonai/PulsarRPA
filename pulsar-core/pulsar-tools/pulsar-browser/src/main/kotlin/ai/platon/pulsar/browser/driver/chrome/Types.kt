package ai.platon.pulsar.browser.driver.chrome

import ai.platon.cdt.kt.protocol.types.network.LoadNetworkResourcePageResult
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration
import java.time.Instant

class ChromeVersion {
    @JsonProperty("Browser")
    val browser: String? = null
    @JsonProperty("Protocol-Version")
    val protocolVersion: String? = null
    @JsonProperty("User-Agent")
    val userAgent: String? = null
    @JsonProperty("V8-Version")
    val v8Version: String? = null
    @JsonProperty("WebKit-Version")
    val webKitVersion: String? = null
    @JsonProperty("webSocketDebuggerUrl")
    val webSocketDebuggerUrl: String? = null
}

class ChromeTab {
    var id: String = ""
    var parentId: String? = null
    var description: String? = null
    var title: String? = null
    var type: String? = null
    var url: String? = null
    var devtoolsFrontendUrl: String? = null
    var webSocketDebuggerUrl: String? = null
    var faviconUrl: String? = null

    val createTime = Instant.now()

    val urlOrEmpty get() = url ?: ""

    fun isPageType(): Boolean = PAGE_TYPE == type

    companion object {
        const val PAGE_TYPE = "page"
    }
}

class MethodInvocation(
        var id: Long = 0,
        var method: String,
        var params: Map<String, Any>? = null
) {
    override fun toString(): String {
        val parameters = params?.entries?.joinToString(", ") { it.key + ": " + "..." }
        return if (parameters != null) "$method($parameters)" else "$method()"
    }
}

class DevToolsConfig(
    var readTimeout: Duration = Duration.ofSeconds(READ_TIMEOUT_SECONDS)
) {
    companion object {
        private const val READ_TIMEOUT_PROPERTY = "browser.driver.chrome.readTimeout"
        private val READ_TIMEOUT_SECONDS = System.getProperty(READ_TIMEOUT_PROPERTY, "20").toLong()
    }
}

class NetworkResourceResponse(
    val success: Boolean = false,
    val netError: Int = 0,
    val netErrorName: String = "",
    /** Request isn't made */
    val httpStatusCode: Int = 0,
    val stream: String? = null,
    val headers: Map<String, Any?>? = null,
) {
    companion object {
        fun from(res: LoadNetworkResourcePageResult): NetworkResourceResponse {
            val success = res.success
            val netError = res.netError?.toInt() ?: 0
            val netErrorName = res.netErrorName ?: ""
            val httpStatusCode = res.httpStatusCode?.toInt() ?: 400
            // All pulsar added headers have a prefix Q-
            val headers = res.headers?.toMutableMap() ?: mutableMapOf()
            headers["Q-client"] = "Chrome"
            return NetworkResourceResponse(success, netError, netErrorName, httpStatusCode, res.stream, headers)
        }
    }
}
