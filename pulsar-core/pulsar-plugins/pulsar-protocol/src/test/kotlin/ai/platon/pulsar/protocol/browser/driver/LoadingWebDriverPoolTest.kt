package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.protocol.browser.DefaultWebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import ai.platon.pulsar.skeleton.common.AppSystemInfo
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import java.util.concurrent.Executors
import kotlin.test.Test

class LoadingWebDriverPoolTest {
    private val config = ImmutableConfig()
    private lateinit var browserId: BrowserId
    private val poolManager = DefaultWebDriverPoolManager(config)
    private lateinit var pool: LoadingWebDriverPool
    private val seeds = LinkExtractors.fromResource("seeds/seeds.txt")

    fun checkPlaywrightAvailable(): Boolean {
        val outputs = Runtimes.exec("playwright --version")
        return outputs.any { it.contains("Version") }
    }

    @BeforeEach
    fun setup() {
        browserId = if (checkPlaywrightAvailable()) {
            BrowserId.createRandomTemp(BrowserType.PLAYWRIGHT_CHROME)
        } else {
            BrowserId.createRandomTemp()
        }

        pool = poolManager.createUnmanagedDriverPool(browserId)
    }

    @AfterEach
    fun tearDown() {
        pool.close()
        poolManager.close()
    }

    @Test
    fun test_pollWebDrivers() {
        while(pool.numDriverSlots > 0 && !AppSystemInfo.isSystemOverCriticalLoad) {
            val driver = pool.poll()

            if (driver is PlaywrightDriver) {
                println("Created WebDriver #${driver.id} | ${pool.takeSnapshot()} | ${driver.guid} | ${driver::class.qualifiedName}")
            } else {
                println("Created WebDriver #${driver.id} | ${pool.takeSnapshot()} | ${driver::class.qualifiedName}")
            }

            runBlocking {
                driver.navigateTo(seeds.random())
                driver.waitForSelector("body")
                driver.stop()
            }
        }
    }

    @Tag("TimeConsumingTest")
    @Test
    fun test_pollAndPutWebDrivers() {
        val drivers = mutableListOf<WebDriver>()
        val executor = Executors.newFixedThreadPool(pool.numDriverSlots)

        var i = 0
        while(i++ < 120) {
            if (pool.numDriverSlots == 0) {
                sleepSeconds(1)
                continue
            }

            println("$i. Round $i polling a driver")
            val driver = pool.poll()
            drivers += driver

            if (driver is PlaywrightDriver) {
                println("Created WebDriver #${driver.id} | ${pool.takeSnapshot()} | ${driver.guid} | ${driver::class.qualifiedName}")
            } else {
                println("Created WebDriver #${driver.id} | ${pool.takeSnapshot()} | ${driver::class.qualifiedName}")
            }

            executor.submit {
                val url = seeds.random()
                navigateTo(url, driver)

                println("Navigated, put driver #${driver.id} | $url")
                pool.put(driver)
            }
        }

        drivers.forEach { it.close() }
    }

    private fun navigateTo(url: String, driver: WebDriver) {
        println("Navigating to $url")

        runBlocking {
            try {
                driver.navigateTo(url)
                // driver.waitForSelector("body")
                driver.delay(5000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
