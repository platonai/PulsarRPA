package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.context.PulsarContexts

/**
 * Use custom configured components.
 * */
fun main() {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"

    val context = PulsarContexts.create("classpath:pulsar-beans/app-context.xml")
    val session = context.createSession()
    // TODO: the main thread hung, caused by the runBlocking calling in BrowserEmulatedFetcher
    session.load(portalUrl, "-i 1s")
//    val page = session.submit(PlainUrl(portalUrl, "-i 1s"))
//    val pages = session.submitOutPages(portalUrl, "-expires 1s -itemExpires 7d -outLink a[href~=item]")
//    val documents = pages.map { session.parse(it) }

//    val job = CoroutineScope(Dispatchers.Default).launch {
//        session.loadDeferred(PlainUrl(portalUrl, "-i 1s"))
//    }
//    runBlocking { job.join() }
}
