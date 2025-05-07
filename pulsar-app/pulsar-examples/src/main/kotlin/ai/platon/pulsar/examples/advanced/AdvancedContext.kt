package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.skeleton.context.PulsarContexts

/**
 * Use custom configured components.
 * */
fun main() {
    val context = PulsarContexts.create("classpath:pulsar-beans/test-app-context.xml")
    val session = context.createSession()

    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val pages = session.submitForOutPages(portalUrl, "-expires 1s -itemExpires 7d -outLink a[href~=item]")

    PulsarContexts.await()
}
