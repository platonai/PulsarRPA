package ai.platon.pulsar.browser.driver.chrome

import com.fasterxml.jackson.annotation.JsonProperty

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

    val isPageType: Boolean
        get() = PAGE_TYPE == type

    companion object {
        const val PAGE_TYPE = "page"
    }
}

class MethodInvocation(
        var id: Long = 0,
        var method: String,
        var params: Map<String, Any>? = null
)
