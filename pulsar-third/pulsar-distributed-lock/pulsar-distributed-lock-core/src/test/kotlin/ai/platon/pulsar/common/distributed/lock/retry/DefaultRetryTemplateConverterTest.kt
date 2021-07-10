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

import ai.platon.pulsar.common.distributed.lock.Interval
import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.interval.BeanFactoryAwareIntervalConverter
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import org.assertj.core.api.Assertions
import org.junit.Test
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.CompositeRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.policy.TimeoutRetryPolicy
import org.springframework.retry.support.RetryTemplate
import retry.DefaultRetryTemplateConverter

class DefaultRetryTemplateConverterTest {

    private val intervalConverter: IntervalConverter = BeanFactoryAwareIntervalConverter(DefaultListableBeanFactory())

    @Test
    @Locked
    fun shouldConstructDefaultRetryTemplate() {
        val locked = object : Any() {}.javaClass.enclosingMethod.getAnnotation(Locked::class.java)
        val converter: RetryTemplateConverter = DefaultRetryTemplateConverter(intervalConverter)
        val retryTemplate = converter.construct(locked)
        assertRetryTemplateConstruction(retryTemplate, 1000L, 50L)
    }

    @Test
    @Locked(retry = Interval("100"), timeout = Interval("2000"))
    fun shouldConstructCustomizedRetryTemplate() {
        val locked = object : Any() {}.javaClass.enclosingMethod.getAnnotation(Locked::class.java)
        val converter: RetryTemplateConverter = DefaultRetryTemplateConverter(intervalConverter)
        val retryTemplate = converter.construct(locked)
        assertRetryTemplateConstruction(retryTemplate, 2000L, 100L)
    }

    // is there a better way to test the RetryTemplate construction?
    private fun assertRetryTemplateConstruction(retryTemplate: RetryTemplate, timeout: Long, backOff: Long) {
        val wrapper = PropertyAccessorFactory.forDirectFieldAccess(retryTemplate)
        Assertions.assertThat(wrapper.getPropertyValue("retryPolicy")).isInstanceOf(CompositeRetryPolicy::class.java)
        Assertions.assertThat(wrapper.getPropertyValue("retryPolicy.policies") as Array<RetryPolicy>)
                .hasSize(2)
                .allMatch { policy: RetryPolicy? ->
                    if (policy is TimeoutRetryPolicy) {
                        Assertions.assertThat(policy.timeout).isEqualTo(timeout)
                        return@allMatch true
                    }
                    if (policy is SimpleRetryPolicy) {
                        Assertions.assertThat(policy.maxAttempts).isEqualTo(Int.MAX_VALUE)
                        return@allMatch true
                    }
                    false
                }
        Assertions.assertThat(wrapper.getPropertyValue("backOffPolicy")).isInstanceOf(FixedBackOffPolicy::class.java)
        Assertions.assertThat((wrapper.getPropertyValue("backOffPolicy") as FixedBackOffPolicy).backOffPeriod).isEqualTo(backOff)
    }
}