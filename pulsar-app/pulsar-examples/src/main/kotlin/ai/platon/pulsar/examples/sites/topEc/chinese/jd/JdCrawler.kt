package ai.platon.pulsar.examples.sites.topEc.chinese.jd

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts

fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val args = "-i 1h -ii 5s -ol a[href~=item] -ignoreFailure"

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
