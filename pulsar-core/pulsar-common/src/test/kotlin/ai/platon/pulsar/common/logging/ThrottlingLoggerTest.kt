package ai.platon.pulsar.common.logging

import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import org.slf4j.Logger
import org.slf4j.helpers.MessageFormatter
import java.time.Duration

class ThrottlingLoggerTest {

    private lateinit var mockLogger: Logger
    private lateinit var logger: ThrottlingLogger

    @BeforeEach
    fun setUp() {
        mockLogger = mock<Logger>()
        logger = ThrottlingLogger(mockLogger, Duration.ofMillis(100), enableSuppressedCount = true)
    }

    @Test
    fun `should log message only once within TTL`() {
        val format = "User %s logged in"
        val args = arrayOf("Alice")

        logger.info(format, *args)
        logger.info(format, *args)

        verify(mockLogger, times(1)).info(anyString())
    }

    @Test
    fun `should allow logging after TTL expires`() {
        val format = "System warning: %s"
        val args = arrayOf("low memory")

        logger.info(format, *args)

        Thread.sleep(150) // Wait past TTL

        logger.info(format, *args)

        verify(mockLogger, times(2)).info(anyString())
    }

    @Test
    fun `should count suppressed messages when enabled`() {
        val format = "Configuration reload failed: {}"
        val args = arrayOf("timeout")

        logger.info(format, *args)
        logger.info(format, *args)
        logger.info(format, *args)

        val counts = logger.getSuppressedCounts()
        val key = MessageFormatter.arrayFormat(format, args).message

        Assertions.assertTrue(counts?.containsKey(key) == true)
        Assertions.assertEquals(2, counts?.get(key))
    }

    @Test
    fun `should reset suppression count and cache`() {
        val format = "Error processing request: {}"
        val args = arrayOf("404")

        logger.info(format, *args)
        logger.reset()

        logger.info(format, *args)

        verify(mockLogger, times(2)).info(anyString()) // First before reset, second after reset
    }

    @Test
    fun `should log error with exception and throttle correctly`() {
        val throwable = RuntimeException("Database connection failed")
        val format = "Failed to connect to {}"
        val args = arrayOf("main-db")

        logger.error(throwable, format, *args)
        logger.error(throwable, format, *args)

        verify(mockLogger, times(1)).error(anyString(), eq(throwable))
    }

    @Test
    fun `should handle multiple different messages independently`() {
        logger.info("Message A")
        logger.info("Message B")
        logger.info("Message A")

        verify(mockLogger, times(1)).info(eq("Message A"))
        verify(mockLogger, times(1)).info(eq("Message B"))
    }

    @Test
    fun `should not track suppressed count if disabled`() {
        val simpleLogger = ThrottlingLogger(mockLogger, Duration.ofMinutes(1), enableSuppressedCount = false)

        simpleLogger.info("Repeated message")
        simpleLogger.info("Repeated message")

        Assertions.assertNull(simpleLogger.getSuppressedCounts())
    }
}
