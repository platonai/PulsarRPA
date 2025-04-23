package ai.platon.pulsar.protocol.browser.driver.playwright.badcase

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightTestBase.Companion.BAD_PARALLELISM_WARNING
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlaywrightConcurrentLoadTest {
    companion object {
        const val CONTEXT_COUNT = 5           // 模拟 5 个用户
        const val TABS_PER_CONTEXT = 3        // 每个用户开 3 个标签页
        const val THREAD_POOL_SIZE = 10       // 线程池大小
    }

    // val testUrls = List(1000) { "https://example.com/page${it + 1}" }
    private val testUrls = ConcurrentLinkedQueue(LinkExtractors.fromResource("seeds/seeds.txt"))
    private val pagePool = ArrayBlockingQueue<Page>(CONTEXT_COUNT * TABS_PER_CONTEXT)

    fun run() {
        Playwright.create().use { playwright ->
            // Step 1: 启动多个 context，每个打开多个标签页
            repeat(CONTEXT_COUNT) { contextIndex ->
                val userDataDir = Paths.get("user-data-dir-$contextIndex")
                val context = playwright.chromium().launchPersistentContext(
                    userDataDir,
                    BrowserType.LaunchPersistentContextOptions().apply {
                        headless = false
                    }
                )

                repeat(TABS_PER_CONTEXT) {
                    val page = context.newPage()
                    pagePool.add(page)
                }
            }

            // Step 2: 启动线程池，对每个链接进行压力测试
            val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

            try {
                while (testUrls.isNotEmpty()) {
                    while (pagePool.isEmpty()) {
                        sleepSeconds(1)
                    }

                    val url = testUrls.remove()
                    val page = pagePool.poll()

                    executor.submit {
                        fetch(url, page)
                    }

                }
            } catch (e: InterruptedException) {
                println("❌ Thread interrupted: ${e.message}")
            } catch (e: Exception) {
                println("❌ Error occurred: ${e.message}")
            } finally {
                pagePool.forEach { it.context().close() }
                executor.shutdownNow()
            }

            executor.awaitTermination(10, TimeUnit.MINUTES)

            println("🎉 All requests completed.")
        }
    }

    private fun fetch(url: String, page: Page) {
        val asins = mutableListOf<String>()
        try {
            require(!page.isClosed) { "Page is closed" }

            page.navigate(url, Page.NavigateOptions().setTimeout(30_000.0))
            page.waitForSelector("div[data-asin]", Page.WaitForSelectorOptions().setTimeout(30_000.0))
            println("✅ Opened $url in tab ${page.hashCode()}")

            val elements = page.querySelectorAll("div[data-asin]")
            elements.mapTo(asins) { it.getAttribute("data-asin") }
            println(asins.joinToString())

            page.navigate("about:blank")
            // wait for 1 second before reusing the page
            Thread.sleep(1000)
        } catch (e: Exception) {
            if (asins.isEmpty()) {
                println("❌ Failed to open $url: ${e.message}")
            } else {
                println("ASINs: ${asins.joinToString()}, error: ${e.message}")
            }
        } finally {
            pagePool.add(page)
        }
    }
}

fun main() {
    println(BAD_PARALLELISM_WARNING)

    PlaywrightConcurrentLoadTest().run()
}
