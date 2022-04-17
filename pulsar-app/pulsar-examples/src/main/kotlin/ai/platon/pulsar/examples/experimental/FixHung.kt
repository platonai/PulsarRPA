package ai.platon.pulsar.examples.experimental

import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.context.PulsarContexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object FixHung {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"

    val context = PulsarContexts.create("classpath:pulsar-beans/app-context.xml")
    val session = context.createSession()

    /**
     * the main thread hung, caused by the runBlocking calling in BrowserEmulatedFetcher
     * */
    fun hung() {
        session.load(portalUrl, "-i 1s")
    }

    fun notHung() {
        val job = CoroutineScope(Dispatchers.Default).launch {
            session.loadDeferred(PlainUrl(portalUrl, "-i 1s"))
        }
        runBlocking { job.join() }
    }

    fun notHung2() {
        val page = session.submit(PlainUrl(portalUrl, "-i 1s"))
        context.await()
    }
}

fun main() {
    FixHung.notHung()
    // FixHung.hung()
}
