package ai.platon.pulsar.net

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_USE_PROXY
import ai.platon.pulsar.protocol.browser.driver.LoadingWebDriverPool
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
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

/**
 * TODO: move to pulsar-skeleton module
 * */
class TestWebDriver {
    companion object {
        val log = LoggerFactory.getLogger(TestWebDriver::class.java)

        init {
            System.setProperty(PROXY_USE_PROXY, "no")
        }

        val context = PulsarContext.getOrCreate()
        val conf = context.unmodifiedConfig
        val driverControl = context.getBean(WebDriverControl::class.java)
        val driverPool = context.getBean(LoadingWebDriverPool::class.java)
        var quitMultiThreadTesting = false

        @BeforeClass
        fun setup() {

        }

        @AfterClass
        fun teardown() {
            PulsarContext.shutdown()
        }
    }

    @Test
    fun testCapabilities() {
        val generalOptions = driverControl.createGeneralOptions()
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        var driver: WebDriver = ChromeDriver(generalOptions)

        val chromeOptions = driverControl.createChromeOptions()
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false")
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        driver = ChromeDriver(chromeOptions)
    }

    @Test
    fun testWebDriverPool() {
        val workingDrivers = mutableListOf<ManagedWebDriver>()
        repeat(10) {
            val driver = driverPool.take(conf.toVolatileConfig())
            workingDrivers.add(driver)
        }

        assertEquals(10, driverPool.numWorking.get())
        assertEquals(0, driverPool.numFree)
        assertEquals(10, driverPool.numActive)
        assertEquals(10, driverPool.numOnline)

        workingDrivers.forEachIndexed { i, driver ->
            if (i % 2 == 0) driver.retire()
            driverPool.put(driver)
        }

        assertEquals(0, driverPool.numWorking.get())
        assertEquals(5, driverPool.numFree)
        assertEquals(5, driverPool.counterRetired.count)

        driverPool.close()

        assertEquals(0, driverPool.numWorking.get())
        assertEquals(0, driverPool.numFree)
        assertEquals(10, driverPool.counterQuit.count)
    }

    @Test
    fun testWebDriverPoolMultiThreaded() {
        val workingDrivers = ArrayBlockingQueue<ManagedWebDriver>(30)

        val consumer = Thread {
            while (!quitMultiThreadTesting) {
                if (workingDrivers.size > 20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                }

                if (workingDrivers.size < 20) {
                    val driver = driverPool.take(conf.toVolatileConfig())
                    assertNotNull(driver)
                    workingDrivers.add(driver)
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
                        driverPool.put(driver)
                    } else {
                        log.info("Retire {}", driver)
                        driver.retire()
                        driverPool.put(driver)
                    }
                }
            }
        }

        val closer = Thread {
            while (!quitMultiThreadTesting) {
                log.info("Close all")
                driverPool.close()
                driverPool.close()
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
