/*
 * MIT License
 *
 * Copyright (c) 2020 Alen Turkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ai.platon.pulsar.common.distributed.lock.retry

import ai.platon.pulsar.common.distributed.lock.exception.LockNotAvailableException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.retry.policy.NeverRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@RunWith(MockitoJUnitRunner::class)
class RetriableLockTest {
    @Mock
    private lateinit var lock: ai.platon.pulsar.common.distributed.lock.Lock

    @Test
    fun shouldNotRetryWhenFirstAttemptIsSuccessful() {
        Mockito.`when`(
            lock.acquire(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong()
            )
        )
            .thenReturn("abc")
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(NeverRetryPolicy())
        val retriableLock = RetriableLock(lock, retryTemplate)
        val token = retriableLock.acquire(listOf("key"), "defaultStore", 1000L)
        Assertions.assertThat(token).isEqualTo("abc")
    }

    @Test
    fun shouldRetryWhenFirstAttemptIsNotSuccessful() {
        Mockito.`when`(
            lock.acquire(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong()
            )
        )
            .thenReturn(null)
            .thenReturn("abc")
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(SimpleRetryPolicy(2))
        val retriableLock = RetriableLock(lock, retryTemplate)
        val token = retriableLock.acquire(listOf("key"), "defaultStore", 1000L)
        Assertions.assertThat(token).isEqualTo("abc")
        Mockito.verify(lock, Mockito.times(2))
            .acquire(ArgumentMatchers.anyList(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())
    }

    @Test
    fun shouldFailRetryWhenFirstAttemptIsNotSuccessful() {
        Assertions.assertThatThrownBy {
            Mockito.`when`(
                lock.acquire(
                    ArgumentMatchers.anyList(),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyLong()
                )
            )
                .thenReturn(null)
            val retryTemplate = RetryTemplate()
            retryTemplate.setRetryPolicy(NeverRetryPolicy())
            val retriableLock = RetriableLock(lock, retryTemplate)
            retriableLock.acquire(listOf("key"), "defaultStore", 1000L)
        }.isInstanceOf(LockNotAvailableException::class.java)
    }
}
