package ai.platon.pulsar.basic.session

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants.LOCAL_FILE_BASE_URL
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.basic.TestBase
import com.google.gson.Gson
import org.junit.jupiter.api.Assumptions
import java.nio.file.Files
import kotlin.test.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class PulsarSessionTests: TestBase() {
    private val timestamp = System.currentTimeMillis()
    private val url = "https://www.amazon.com/Best-Sellers/zgbs?t=$timestamp"
    private val url2 = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty?t=$timestamp"

    private val resourceUrl get() = "https://www.example.com/"

    @BeforeTest
    fun setup() {
        // The data store is FileStore, and delete does not work
//        webDB.delete(url)
//        webDB.delete(url2)
    }

    @Test
    fun testNormalize() {
        val normURL = session.normalize(url)
        assertNotEquals(session.sessionConfig, normURL.options.conf)
        val page = session.load(normURL)
        assertEquals(normURL.options.conf, page.conf)
    }

    @Test
    fun testLoad() {
        val page = session.load(url)
        Assumptions.assumeTrue(page.protocolStatus.isSuccess)

        val page2 = webDB.getOrNull(url)

        assertNotNull(page2)
        assertTrue { page2.fetchCount > 0 }
        assertTrue { page2.protocolStatus.isSuccess }

        println(WebPageFormatter(page2))
        println(page2.vividLinks)

        val gson = Gson()
        println(gson.toJson(page2.activeDOMStatus))
        println(gson.toJson(page2.activeDOMStatTrace))
    }

    @Test
    fun testLoadResource() {
        val page = session.loadResource(resourceUrl, url, "-refresh")
        Assumptions.assumeTrue(page.protocolStatus.isSuccess)

        assertTrue { page.fetchCount > 0 }
        assertTrue { page.protocolStatus.isSuccess }

        println(WebPageFormatter(page))
        val path = session.export(page)
        println("Webpage exported | $path")
    }

    @Test
    fun testLoadLocalFile() {
        val path = AppPaths.getTmpDirectory("test.html")
        println(path)
        val html = """
            <html>
            <head>
            <title>Test</title>
            </head>
            <body>
            <h1>Hello</h1>
            <a href="http://www.example.com">Example</a>
            </body>
            </html>
        """.trimIndent()
        Files.writeString(path, html)
        val url = URLUtils.pathToLocalURL(path)
        assertTrue { url.startsWith(LOCAL_FILE_BASE_URL) }
        val document = session.loadDocument(url, "-refresh")
        assertEquals("Hello", document.selectFirstTextOrNull("h1"))
    }
}
