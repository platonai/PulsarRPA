package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_USE_PROXY
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.*

class TestWebDriverPool {
    companion object {
        val logger = LoggerFactory.getLogger(TestWebDriverPool::class.java)

        init {
            System.setProperty(PROXY_USE_PROXY, "no")
        }

        val context = PulsarContexts.create()
        val conf = context.unmodifiedConfig
        val driverPoolManager = DefaultWebDriverPoolManager(conf)
        var quitMultiThreadTesting = false
    }

//    @Test
//    fun testCapabilities() {
//        val generalOptions = driverControl.createGeneralOptions()
//        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
//        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
//    }

    @AfterTest
    fun tearDown() {
        driverPoolManager.close()
    }

    @Test
    fun testWebDriverPool() {
        val driverPool = driverPoolManager.createUnmanagedDriverPool()
        val workingDrivers = mutableListOf<WebDriver>()
        val numDrivers = driverPool.capacity
        repeat(numDrivers) {
            val driver = driverPool.poll()
            require(driver is AbstractWebDriver)
            assertTrue { driver.isWorking }
            workingDrivers.add(driver)
        }

        assertEquals(numDrivers, driverPool.numWorking)
        assertEquals(0, driverPool.numStandby)
        assertEquals(numDrivers, driverPool.numActive)

        workingDrivers.forEachIndexed { i, driver ->
            require(driver is AbstractWebDriver)

            if (i % 2 == 0) {
                driver.retire()
                assertTrue { driver.isRetired }
            } else {
                assertTrue { driver.isWorking }
            }

            driverPool.put(driver)
        }

        assertEquals(0, driverPool.numWorking)
        assertEquals(numDrivers / 2, driverPool.numStandby)
        assertEquals(numDrivers / 2, driverPool.meterClosed.count.toInt())

        driverPool.close()

        assertEquals(0, driverPool.numWorking)
        assertEquals(0, driverPool.numStandby)
//        assertEquals(10, driverPool.counterClosed.count)
    }

    @Ignore("Time consuming (and also bugs)")
    @Test
    fun testWebDriverPoolMultiThreaded() {
        val driverPool = driverPoolManager.createUnmanagedDriverPool()
        val workingDrivers = ArrayBlockingQueue<WebDriver>(30)

        val consumer = Thread {
            while (!quitMultiThreadTesting) {
                if (workingDrivers.size > 20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                }

                if (workingDrivers.size < 20) {
                    driverPool.poll().let { workingDrivers.add(it) }
                }
            }
        }

        val producer = Thread {
            while (!quitMultiThreadTesting) {
                val i = Random().nextInt()
                val driver = workingDrivers.poll()
                if (driver != null) {
                    require(driver is AbstractWebDriver)

                    if (i % 3 == 0) {
                        logger.info("Offer {}", driver)
                        driverPool.put(driver)
                    } else {
                        logger.info("Retire {}", driver)
                        driver.retire()
                        driverPool.put(driver)
                    }
                }
            }
        }

        val closer = Thread {
            while (!quitMultiThreadTesting) {
                // log.info("Close all")
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

        logger.info("All done.")
        quitMultiThreadTesting = true
        producer.join()
        consumer.join()
        closer.join()
    }
}
