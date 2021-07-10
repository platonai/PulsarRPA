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
package retry

import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.exception.LockNotAvailableException
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import ai.platon.pulsar.common.distributed.lock.retry.RetryTemplateConverter
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.CompositeRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.policy.TimeoutRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.util.*

class DefaultRetryTemplateConverter(
        val intervalConverter: IntervalConverter
) : RetryTemplateConverter {

    override fun construct(locked: Locked): RetryTemplate {
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(resolveLockRetryPolicy(locked))
        retryTemplate.setBackOffPolicy(resolveBackOffPolicy(locked))
        return retryTemplate
    }

    private fun resolveLockRetryPolicy(locked: Locked): CompositeRetryPolicy {
        val compositeRetryPolicy = CompositeRetryPolicy()
        val timeoutRetryPolicy = resolveTimeoutRetryPolicy(locked)
        val exceptionTypeRetryPolicy = resolveExceptionTypeRetryPolicy()
        compositeRetryPolicy.setPolicies(arrayOf(timeoutRetryPolicy, exceptionTypeRetryPolicy))
        return compositeRetryPolicy
    }

    private fun resolveTimeoutRetryPolicy(locked: Locked): RetryPolicy {
        val timeoutRetryPolicy = TimeoutRetryPolicy()
        timeoutRetryPolicy.timeout = intervalConverter.toMillis(locked.timeout)
        return timeoutRetryPolicy
    }

    private fun resolveExceptionTypeRetryPolicy(): RetryPolicy {
        return SimpleRetryPolicy(Int.MAX_VALUE, Collections.singletonMap(LockNotAvailableException::class.java, true))
    }

    private fun resolveBackOffPolicy(locked: Locked): FixedBackOffPolicy {
        val fixedBackOffPolicy = FixedBackOffPolicy()
        fixedBackOffPolicy.backOffPeriod = intervalConverter.toMillis(locked.retry)
        return fixedBackOffPolicy
    }
}
