package ai.platon.pulsar.test

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.crawl.common.URLUtil
import kotlin.test.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestLoadResources: TestBase() {
    private val resourceUrls = """
        http://www.181hua.com/show/957.html
        http://www.181hua.com/show/947.html
        http://www.181hua.com/show/974.html
        http://www.181hua.com/show/960.html
        https://www.shidaihuayuan.com/a/1834.html
        https://www.shidaihuayuan.com/a/1646.html
        https://www.shidaihuayuan.com/a/1612.html
        https://www.shidaihuayuan.com/a/1597.html
    """.trimIndent().split("\n")

    @BeforeTest
    fun setup() {
        resourceUrls.forEach { webDB.delete(it) }
    }

    @Test
    fun testLoadResource() {
        resourceUrls.forEachIndexed { i, resourceUrl ->
            val referrer = URLUtil.getOrigin(resourceUrl)
            val page = session.loadResource(resourceUrl, referrer, "-refresh")

            val content = page.contentAsString.asSequence()
                .filter { Strings.isCJK(it) }.take(100)
                .joinToString("")
            println("$i.\t" + page.contentLength + "\t" + content)

            assertTrue(resourceUrl) { page.fetchCount > 0 }
            assertTrue(resourceUrl) { page.protocolStatus.isSuccess }
            assertTrue(resourceUrl) { page.contentLength > 0 }
        }
    }
}
