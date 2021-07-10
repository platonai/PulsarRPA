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

import ai.platon.pulsar.common.distributed.lock.Lock
import ai.platon.pulsar.common.distributed.lock.Locked
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.retry.support.RetryTemplate

@RunWith(MockitoJUnitRunner::class)
class DefaultRetriableLockFactoryTest {

    @Mock
    private lateinit var retryTemplateConverter: RetryTemplateConverter

    @Mock
    private lateinit var lock: Lock

    @Mock
    private lateinit var locked: Locked

    @Mock
    private lateinit var retryTemplate: RetryTemplate

    @Test
    fun shouldGenerateRetriableLock() {
        Mockito.`when`(retryTemplateConverter.construct(locked)).thenReturn(retryTemplate)
        val factory: RetriableLockFactory = DefaultRetriableLockFactory(retryTemplateConverter)
        val retriableLock = factory.generate(lock, locked)
        Assertions.assertThat(retriableLock.lock).isEqualTo(lock)
        Assertions.assertThat(retriableLock.retryTemplate).isEqualTo(retryTemplate)
    }
}
