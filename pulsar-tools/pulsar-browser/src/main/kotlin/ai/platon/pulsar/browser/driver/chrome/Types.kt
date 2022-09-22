package ai.platon.pulsar.browser.driver.chrome

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
        var readTimeout: Duration = Duration.ofMinutes(READ_TIMEOUT_MINUTES)
) {
    companion object {
        private const val READ_TIMEOUT_PROPERTY = "browser.driver.chrome.readTimeout"
        private val READ_TIMEOUT_MINUTES = System.getProperty(READ_TIMEOUT_PROPERTY, "1").toLong()
    }
}
