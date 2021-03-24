package ai.platon.pulsar.test

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarSession: TestBase() {
    val url = "https://www.baidu.com/"

    @Test
    fun testNormalize() {
        val normUrl = session.normalize(url)
        assertNotEquals(session.sessionConfig, normUrl.options.conf)
        val page = session.load(normUrl)
        assertEquals(normUrl.options.conf, page.conf)
    }
}
