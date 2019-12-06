package ai.platon.pulsar.net

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_USE_PROXY
import ai.platon.pulsar.net.browser.ManagedWebDriver
import ai.platon.pulsar.net.browser.WebDriverPool
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.CapabilityType
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestWebDriver {
    companion object {
        val log = LoggerFactory.getLogger(TestWebDriver::class.java)

        init {
            System.setProperty(PROXY_USE_PROXY, "no")
        }

        val env = PulsarEnv.initialize()
        val conf = PulsarEnv.unmodifiedConfig
        val pool = PulsarEnv.applicationContext.getBean(WebDriverPool::class.java)
        var quitMultiThreadTesting = false

        @BeforeClass
        fun setup() {

        }

        @AfterClass
        fun teardown() {
            PulsarEnv.initialize().shutdown()
        }
    }

    @Test
    fun testCapabilities() {
        val generalOptions = BrowserControl.createGeneralOptions()
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        var driver: WebDriver = ChromeDriver(generalOptions)

        val chromeOptions = BrowserControl.createChromeOptions()
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false")
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        driver = ChromeDriver(chromeOptions)
    }

    @Test
    fun testWebDriverPool() {
        val workingDrivers = mutableListOf<ManagedWebDriver>()
        repeat(10) {
            val driver = pool.poll(0, conf)
            if (driver != null) {
                assertNotNull(driver)
                workingDrivers.add(driver)
            }
        }

        assertEquals(10, pool.workingSize)
        assertEquals(0, pool.freeSize)
        assertEquals(10, pool.aliveSize)
        assertEquals(10, pool.totalSize)

        workingDrivers.forEachIndexed { i, driver ->
            if (i % 2 == 0) pool.offer(driver)
            else pool.retire(driver, null)
        }

        assertEquals(0, pool.workingSize)
        assertEquals(5, pool.freeSize)
        assertEquals(5, WebDriverPool.numRetired.get())

        pool.closeAll()

        assertEquals(0, pool.workingSize)
        assertEquals(0, pool.freeSize)
        assertEquals(10, WebDriverPool.numQuit.get())

        pool.closeAll()

        assertEquals(0, pool.workingSize)
        assertEquals(0, pool.freeSize)
        assertEquals(10, WebDriverPool.numQuit.get())
    }

    @Test
    fun testWebDriverPoolMultThreaded() {
        val workingDrivers = ArrayBlockingQueue<ManagedWebDriver>(30)

        val consumer = Thread {
            while (!quitMultiThreadTesting) {
                if (workingDrivers.size > 20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                }

                if (workingDrivers.size < 20) {
                    val driver = pool.poll(0, conf)
                    if (driver != null) {
                        assertNotNull(driver)
                        workingDrivers.add(driver)
                    }
                }
            }
        }

        val producer = Thread {
            while (!quitMultiThreadTesting) {
                val i = Random().nextInt()
                val driver = workingDrivers.poll()
                if (driver != null) {
                    if (i % 3 == 0) {
                        log.info("Offer {}", driver)
                        pool.offer(driver)
                    } else {
                        log.info("Retire {}", driver)
                        pool.retire(driver, null)
                    }
                }
            }
        }

        val closer = Thread {
            while (!quitMultiThreadTesting) {
                log.info("Close all")
                pool.closeAll()
                pool.closeAll()
                Thread.sleep(1000)
            }
        }

        consumer.isDaemon = true
        producer.isDaemon = true
        closer.isDaemon = true

        consumer.start()
        producer.start()
        closer.start()

        var n = 360
        while (n-- > 0) {
            TimeUnit.SECONDS.sleep(1)
        }

        log.info("All done.")
        quitMultiThreadTesting = true
        producer.join()
        consumer.join()
        closer.join()
    }
}
