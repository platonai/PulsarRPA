package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.collections4.ComparatorUtils
import org.apache.http.client.utils.URIBuilder
import java.net.URI

/**
 * The website account.
 *
 * @property domain the domain of the website.
 * @property loginURL the login URL of the website.
 * @property username the username of the account.
 * @property password the password of the account.
 * @property redirectURL the redirect URL after login.
 * @property email the email of the account.
 * @property phone the phone number of the account.
 * @property data the additional data of the account.
 * @property usernameInputSelector the selector of the username input element.
 * @property passwordInputSelector the selector of the password input element.
 * @property submitButtonSelector the selector of the submit button.
 * */
data class WebsiteAccount(
    val domain: String,
    val loginURL: String,
    val username: String,
    val password: String,
    val usernameInputSelector: String,
    val passwordInputSelector: String,
    val submitButtonSelector: String,
    val redirectURL: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val data: MutableMap<String, String> = mutableMapOf(),
)

/**
 * The browser fingerprint.
 *
 * @property browserType the browser type.
 * @property proxyURI the proxy server URI.
 * @property username the username of the target website.
 * @property password the password of the target website.
 * @property userAgent the user agent of the browser.
 * @property source the full path of the source file of the fingerprint.
 * */
data class Fingerprint(
    val browserType: BrowserType,
    var proxyURI: URI? = null,
    @Deprecated("Use websiteAccounts instead")
    var username: String? = null,
    @Deprecated("Use websiteAccounts instead")
    var password: String? = null,
    var userAgent: String? = null,
    val websiteAccounts: MutableMap<String, WebsiteAccount> = mutableMapOf(),
    var source: String? = null,
) : Comparable<Fingerprint> {
    private val comp = ComparatorUtils.nullLowComparator { o1: String, o2: String ->
        o1.compareTo(o2)
    }
    
    @get:JsonIgnore
    val isLoaded get() = source != null
    
    @get:JsonIgnore
    val proxyEntry get() = proxyURI?.let { ProxyEntry.fromURI(it) }
    
    constructor(
        browserType: BrowserType,
        proxyURI: String,
        username: String? = null,
        password: String? = null,
        userAgent: String? = null
    ) : this(browserType, URI(proxyURI), username, password, userAgent)
    
    constructor(
        browserType: BrowserType,
        proxy: ProxyEntry,
        username: String? = null,
        password: String? = null,
        userAgent: String? = null
    ) : this(browserType, proxy.toURI(), username, password, userAgent)
    
    /**
     * Set the proxy server.
     * @param protocol the protocol of the proxy server, e.g. http, https, socks5.
     * @param hostPort the host and port of the proxy server, e.g. localhost:8080
     * */
    fun setProxy(protocol: String, hostPort: String, username: String?, password: String?) {
        proxyURI = URIBuilder().apply {
            scheme = protocol
            host = hostPort
            if (username != null && password != null) {
                userInfo = "$username:$password"
            }
        }.build()
    }
    
    fun setProxy(proxy: ProxyEntry) = setProxy(proxy.protocol, proxy.hostPort, proxy.username, proxy.password)
    
    override fun compareTo(other: Fingerprint): Int {
        var r = 0
        listOfNotNull(
            browserType.name to other.browserType.name,
            proxyURI?.toString() to other.proxyURI?.toString(),
            username to other.username,
            userAgent to other.userAgent,
        ).forEach {
            r = comp.compare(it.first, it.second)
            
            if (r != 0) {
                return r
            }
        }
        
        return r
    }
    
    override fun hashCode() = toString().hashCode()
    
    /**
     * TODO: review the equality logic
     * */
    override fun equals(other: Any?): Boolean {
        return other is Fingerprint && listOf(
            browserType to other.browserType,
            proxyURI?.toString() to other.proxyURI?.toString(),
            username to other.username,
        ).all { it.first == it.second }
    }
    
    override fun toString(): String = listOfNotNull(
        browserType, proxyURI, username
    ).joinToString()
    
    companion object {
        val DEFAULT = Fingerprint(BrowserType.PULSAR_CHROME)
        val EXAMPLE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
        val EXAMPLE =
            Fingerprint(BrowserType.PULSAR_CHROME, URI("http://localhost:8080"), "John", "abc", EXAMPLE_USER_AGENT)
        
        fun fromJson(json: String): Fingerprint {
            return pulsarObjectMapper().readValue(json, Fingerprint::class.java)
        }
    }
}
