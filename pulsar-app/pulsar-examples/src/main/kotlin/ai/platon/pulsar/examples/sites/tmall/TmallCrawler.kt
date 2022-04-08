package ai.platon.pulsar.examples.sites.tmall

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.DefaultPulsarEventHandler
import ai.platon.pulsar.crawl.event.LoginHandler

fun main() {
    // general parameters
    val portalUrl = "https://list.tmall.com/search_product.htm?q=大家电"
    val args = "-i 1s -ii 5m -ol a[href~=detail] -ignoreFailure"

    // login parameters
    val loginUrl = "https://login.taobao.com"
    val activateSelector = ".password-login-tab-item"
    val usernameSelector = "input#fm-login-id"
    val username = System.getenv("EXOTIC_TMALL_USERNAME") ?: "ivincent.zhang@gmail.com"
    val passwordSelector = "input#fm-login-password"
    val password = System.getenv("EXOTIC_TMALL_PASSWORD") ?: "Nichang2^taobao"
    val submitSelector = "no-button[type=submit]"

    val eventHandler = if (username != null && password != null) {
        val loginHandler = LoginHandler(loginUrl,
            usernameSelector, username, passwordSelector, password, submitSelector, activateSelector)
        DefaultPulsarEventHandler().also {
            it.loadEventHandler.onAfterBrowserLaunch.addLast(loginHandler)
        }
    } else null

    val session = PulsarContexts.createSession()
    val options = session.options(args, eventHandler)

    session.loadOutPages(portalUrl, options)

    readLine()
}
