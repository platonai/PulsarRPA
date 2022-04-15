package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.context.withContext

/**
 * Use custom configured components.
 * */
fun main() = withContext("classpath:pulsar-beans/app-context.xml") {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val session = it.createSession()
    val pages = session.loadOutPages(portalUrl, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
    val documents = pages.map { session.parse(it) }
}
