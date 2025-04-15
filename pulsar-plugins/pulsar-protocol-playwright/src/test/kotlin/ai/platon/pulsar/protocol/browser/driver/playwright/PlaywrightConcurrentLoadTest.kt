package ai.platon.pulsar.protocol.browser.driver.playwright

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlaywrightConcurrentLoadTest {
    companion object {
        const val CONTEXT_COUNT = 5           // 模拟 5 个用户
        const val TABS_PER_CONTEXT = 3        // 每个用户开 3 个标签页
        const val THREAD_POOL_SIZE = 10       // 线程池大小
    }

    val testUrls = List(1000) { "https://example.com/page${it + 1}" }
    val pagePool = ArrayBlockingQueue<Page>(CONTEXT_COUNT * TABS_PER_CONTEXT)

    fun run() {
        Playwright.create().use { playwright ->
            // Step 1: 启动多个 context，每个打开多个标签页
            repeat(CONTEXT_COUNT) { contextIndex ->
                val userDataDir = Paths.get("user-data-dir-$contextIndex")
                val context = playwright.chromium().launchPersistentContext(
                    userDataDir,
                    BrowserType.LaunchPersistentContextOptions().apply {
                        headless = true
                    }
                )

                repeat(TABS_PER_CONTEXT) {
                    val page = context.newPage()
                    pagePool.add(page)
                }
            }

            // Step 2: 启动线程池，对每个链接进行压力测试
            val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

            for (url in testUrls) {
                executor.submit {
                    val page = pagePool.poll()
                    try {
                        page.navigate(url, Page.NavigateOptions().setTimeout(10_000.0))
                        println("✅ Opened $url in tab ${page.hashCode()}")
                        Thread.sleep(2000)
                        page.navigate("about:blank")
                    } catch (e: Exception) {
                        println("❌ Failed to open $url: ${e.message}")
                    } finally {
                        pagePool.add(page)
                    }
                }
            }

            executor.awaitTermination(10, TimeUnit.MINUTES)
            executor.shutdown()

            println("🎉 All requests completed.")
        }
    }
}

fun main() {
    PlaywrightConcurrentLoadTest().run()
}
