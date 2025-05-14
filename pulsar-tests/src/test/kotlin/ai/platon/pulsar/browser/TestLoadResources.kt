package ai.platon.pulsar.browser

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.urls.URLUtils
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assumptions
import kotlin.test.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestLoadResources: WebDriverTestBase() {
    private val resourceUrls get() = """
        $csvTextUrl
        $jsonUrl
        ${"$assetsBaseURL/dom.html"}
        ${"$assetsBaseURL/drag-n-drop.html"}
        ${"$assetsBaseURL/error.html"}
        ${"$assetsBaseURL/injectedfile.js"}
        ${"$assetsBaseURL/simple.json"}
        ${"$generatedAssetsBaseURL/interactive-1.html"}
        ${"$generatedAssetsBaseURL/interactive-2.html"}
        https://www.baidu.com/s?wd=america
    """.trimIndent().split("\n")

    @BeforeTest
    fun setup() {
        resourceUrls.forEach { webDB.delete(it) }
    }

    @Test
    fun testLoadResource() {
        resourceUrls.forEachIndexed { i, resourceUrl ->
            val referrer = URLUtils.getOrigin(resourceUrl)
            val page = session.loadResource(resourceUrl, referrer, "-refresh")
            Assumptions.assumeTrue { page.protocolStatus.isSuccess }

            val content = page.contentAsString.asSequence()
                .filter { Strings.isCJK(it) }.take(100)
                .joinToString("")
            println("$i.\t" + page.contentLength + "\t" + content)

            assertTrue(resourceUrl) { page.fetchCount > 0 }
            assertTrue(resourceUrl) { page.protocolStatus.isSuccess }
            assertTrue(resourceUrl) { page.contentLength > 0 }
        }
    }

    @Ignore("This test is not stable")
    @Test
    fun testLoadResource2() = runWebDriverTest(browser) { driver ->
        val resourceUrl = robotsUrl
//        val resourceUrl = "https://www.amazon.com/robots.txt"
        val referrer = URLUtils.getOrigin(resourceUrl)
        driver.navigateTo(referrer)
        driver.waitForNavigation()

        val response = driver.loadResource(resourceUrl)
        val headers = response.headers
        val body = response.stream
        assertNotNull(headers)
        assertNotNull(body)

        println(body)

//        println(body)
        assertContains(body, "Disallow", ignoreCase = true,
            message = "Disallow should be in body: >>>\n${StringUtils.abbreviateMiddle(body, "...", 100)}\n<<<")

//        val cookies = response.entries.joinToString("; ") { it.key + "=" + it.value }
//        println(cookies)
        // headers.forEach { (name, value) -> println("$name: $value") }
        assertContains(headers.toString(), "Content-Type", ignoreCase = true,
            message = "Content-Type should be in headers: >>>\n$headers\n<<<")
    }

    @Test
    fun testJsoupLoadResource() = runWebDriverTest { driver ->
//        val resourceUrl = "https://www.amazon.com/robots.txt"
        val resourceUrl = robotsUrl

        val referrer = URLUtils.getOrigin(resourceUrl)
        driver.navigateTo(referrer)
        driver.waitForNavigation()

        val response = driver.loadJsoupResource(resourceUrl)
        val headers = response.headers()
        val body = response.body()
        assertNotNull(body)

//        println(body)
        assertContains(body, "Disallow")
        // check cookies and headers
        val cookies = response.cookies().entries.joinToString("; ") { it.key + "=" + it.value }
        println(cookies)
        response.headers().forEach { (name, value) -> println("$name: $value") }

        assertContains(headers.toString(), "Content-Type", ignoreCase = true,
            message = "Content-Type should be in headers: >>>\n$headers\n<<<")
    }
}
