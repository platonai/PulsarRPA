package ai.platon.pulsar.crawl

import ai.platon.pulsar.context.PulsarContexts
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarSession {
    val cx = PulsarContexts.activate()
    val url = "https://www.baidu.com/"

    @Test
    fun testNormalize() {
        val i = cx.createSession()
        val normUrl = i.normalize(url)
        assertEquals(i.sessionConfig, normUrl.options.volatileConfig)
        val page = i.load(normUrl)
        assertEquals(normUrl.options.volatileConfig, page.volatileConfig)
    }
}
