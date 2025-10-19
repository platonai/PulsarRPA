package ai.platon.pulsar.protocol.browser.driver.playwright.badcase

import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightTestBase.Companion.BAD_PARALLELISM_WARNING
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Warning: this is a bad case to demonstrate the problem of Playwright's parallelism.
 * */
fun main() {
    logPrintln(BAD_PARALLELISM_WARNING)

    val THREAD_COUNT = 5
    val PAGES_PER_THREAD = 5
    val TOTAL_URLS = 1000

    val testUrls = List(TOTAL_URLS) { "https://example.com/page${it + 1}" }

    Playwright.create().use { playwright ->
        val executor = Executors.newFixedThreadPool(THREAD_COUNT)

        // 将 URLs 平均分片
        val chunks = testUrls.chunked(testUrls.size / THREAD_COUNT)

        repeat(THREAD_COUNT) { threadIndex ->
            executor.submit {
                val contextDir = Paths.get("user-data-dir-$threadIndex")
                val context = playwright.chromium().launchPersistentContext(
                    contextDir,
                    BrowserType.LaunchPersistentContextOptions().apply {
                        headless = false
                    }
                )

                val pages = mutableListOf<Page>()
                for (i in 1..PAGES_PER_THREAD) {
                    logPrintln("$i. Creating new page")
                    val page = context.newPage()
                    pages.add(page)
                }

                val myUrls = chunks.getOrNull(threadIndex) ?: emptyList()

                logPrintln("🚀 Thread-$threadIndex starting with ${myUrls.size} URLs")

                myUrls.forEachIndexed { i, url ->
                    val page = pages[i % pages.size]
                    try {
                        page.navigate(url, Page.NavigateOptions().setTimeout(10_000.0))
                        logPrintln("✅ [Thread-$threadIndex] Opened $url in tab ${page.hashCode()}")
                    } catch (e: Exception) {
                        logPrintln("❌ [Thread-$threadIndex] Failed to open $url: ${e.message}")
                    }
                }

                context.close()  // 关闭该线程的浏览器上下文
                logPrintln("✅ Thread-$threadIndex done")
            }
        }

        executor.awaitTermination(30, TimeUnit.MINUTES)
        logPrintln("🎉 All threads complete.")

        executor.shutdown()
    }
}

