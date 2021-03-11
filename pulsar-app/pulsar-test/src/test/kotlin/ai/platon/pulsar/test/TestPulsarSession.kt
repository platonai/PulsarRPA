package ai.platon.pulsar.test

import ai.platon.pulsar.context.PulsarContexts
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarSession: TestBase() {
    val url = "https://www.baidu.com/"

    @Test
    fun testNormalize() {
        val normUrl = session.normalize(url)
        assertEquals(session.sessionConfig, normUrl.options.volatileConfig)
        val page = session.load(normUrl)
        assertEquals(normUrl.options.volatileConfig, page.volatileConfig)
    }
}
