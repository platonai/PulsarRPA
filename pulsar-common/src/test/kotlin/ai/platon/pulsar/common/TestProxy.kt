package ai.platon.pulsar.common

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.url.Urls
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 */
class TestProxy {

    @Test
    fun testProxyEntry() {
        ResourceLoader
                .readAllLines(ProxyEntry.PROXY_TEST_WEB_SITES_FILE)
                .forEach {
                    println(it)
                    val url = Urls.getURLOrNull(it)
                    println(url)
                    if (url != null)  {
                        ProxyEntry.TEST_URLS.add(url)
                    }
                }
//        ResourceLoader.readAllLines(ProxyEntry.PROXY_TEST_WEB_SITES_FILE).mapNotNullTo(ProxyEntry.TEST_URLS) { Urls.getURLOrNull(it) }
//        ProxyEntry.TEST_URLS.forEach { println(it) }

        println("hello")
//        val testProxy = ProxyEntry("117.90.220.193", 4216)
//        println(testProxy)
    }
}
