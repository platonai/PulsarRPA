package ai.platon.pulsar.agentic.ai.agent.detail

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration tests for RetryStrategy component.
 */
class RetryStrategyTest {
    
    private lateinit var retryStrategy: RetryStrategy
    
    @BeforeEach
    fun setUp() {
        retryStrategy = RetryStrategy(
            maxRetries = 3,
            baseDelayMs = 100,
            maxDelayMs = 1000
        )
    }
    
    @Test
    fun `should retry transient errors`() {
        assertTrue(retryStrategy.shouldRetry(PerceptiveAgentError.TransientError("test")))
        assertTrue(retryStrategy.shouldRetry(ConnectException("test")))
        assertTrue(retryStrategy.shouldRetry(SocketTimeoutException("test")))
        assertTrue(retryStrategy.shouldRetry(UnknownHostException("test")))
    }
    
    @Test
    fun `should not retry permanent errors`() {
        assertFalse(retryStrategy.shouldRetry(PerceptiveAgentError.PermanentError("test")))
        assertFalse(retryStrategy.shouldRetry(IllegalStateException("test")))
        assertFalse(retryStrategy.shouldRetry(NullPointerException("test")))
    }
    
    @Test
    fun `should calculate increasing delays with exponential backoff`() {
        val delay0 = retryStrategy.calculateDelay(0)
        val delay1 = retryStrategy.calculateDelay(1)
        val delay2 = retryStrategy.calculateDelay(2)
        
        // Each delay should be larger (accounting for jitter range)
        assertTrue(delay1 > delay0)
        assertTrue(delay2 > delay1)
        
        // Delays should respect max
        assertTrue(delay2 <= 1000)
    }
    
    @Test
    fun `should execute action with retry on transient failure`() = runBlocking {
        val attemptCounter = AtomicInteger(0)
        
        val result = retryStrategy.execute(
            action = {
                val count = attemptCounter.incrementAndGet()
                if (count < 3) {
                    throw SocketTimeoutException("Transient error")
                }
                "success"
            }
        )
        
        assertEquals("success", result)
        assertEquals(3, attemptCounter.get())
    }
    
    @Test
    fun `should exhaust retries and throw last error`() = runBlocking {
        val attemptCounter = AtomicInteger(0)
        
        try {
            retryStrategy.execute(
                action = {
                    attemptCounter.incrementAndGet()
                    throw SocketTimeoutException("Persistent error")
                }
            )
            fail("Should have thrown exception")
        } catch (e: SocketTimeoutException) {
            assertEquals("Persistent error", e.message)
            assertEquals(4, attemptCounter.get()) // 1 initial + 3 retries
        }
    }
    
    @Test
    fun `should not retry on permanent error`() = runBlocking {
        val attemptCounter = AtomicInteger(0)
        
        try {
            retryStrategy.execute(
                action = {
                    attemptCounter.incrementAndGet()
                    throw IllegalStateException("Permanent error")
                }
            )
            fail("Should have thrown exception")
        } catch (e: IllegalStateException) {
            assertEquals("Permanent error", e.message)
            assertEquals(1, attemptCounter.get()) // Only 1 attempt, no retries
        }
    }
    
    @Test
    fun `should invoke callbacks on retry`() = runBlocking {
        val retryCallbacks = mutableListOf<Pair<Int, Long>>()
        val errorCallbacks = mutableListOf<Pair<Int, Throwable>>()
        val attemptCounter = AtomicInteger(0)
        
        retryStrategy.execute(
            action = {
                val count = attemptCounter.incrementAndGet()
                if (count < 3) {
                    throw ConnectException("Retry $count")
                }
                "success"
            },
            onRetry = { attempt, delayMs ->
                retryCallbacks.add(attempt to delayMs)
            },
            onError = { attempt, error ->
                errorCallbacks.add(attempt to error)
            }
        )
        
        assertEquals(2, retryCallbacks.size) // 2 retries before success
        assertEquals(2, errorCallbacks.size)
    }
    
    @Test
    fun `should classify errors correctly`() {
        val timeout = retryStrategy.classifyError(SocketTimeoutException("test"), "step 1")
        assertTrue(timeout is PerceptiveAgentError.TimeoutError)
        
        val transient = retryStrategy.classifyError(ConnectException("test"), "step 1")
        assertTrue(transient is PerceptiveAgentError.TransientError)
        
        val validation = retryStrategy.classifyError(IllegalArgumentException("test"), "step 1")
        assertTrue(validation is PerceptiveAgentError.ValidationError)
        
        val permanent = retryStrategy.classifyError(IllegalStateException("test"), "step 1")
        assertTrue(permanent is PerceptiveAgentError.PermanentError)
    }
    
    @Test
    fun `should classify IOException based on message`() {
        val connectionError = retryStrategy.classifyError(
            IOException("Connection refused"), "step 1"
        )
        assertTrue(connectionError is PerceptiveAgentError.TransientError)
        
        val timeoutError = retryStrategy.classifyError(
            IOException("Read timeout"), "step 1"
        )
        assertTrue(timeoutError is PerceptiveAgentError.TimeoutError)
    }
}
