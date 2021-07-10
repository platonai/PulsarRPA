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
package ai.platon.pulsar.common.distributed.lock.interval

import ai.platon.pulsar.common.distributed.lock.Interval
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.util.StringUtils

/**
 * [IntervalConverter] capable of resolving properties.
 */
class BeanFactoryAwareIntervalConverter(
        val beanFactory: ConfigurableBeanFactory
) : IntervalConverter {

    override fun toMillis(interval: Interval): Long {
        return convertToMilliseconds(interval, resolveMilliseconds(interval))
    }

    private fun resolveMilliseconds(interval: Interval): String {
        val value = beanFactory.resolveEmbeddedValue(interval.value)
        require(StringUtils.hasText(value)) { "Cannot convert interval $interval to milliseconds" }
        return value!!
    }

    private fun convertToMilliseconds(interval: Interval, value: String): Long {
        return try {
            interval.unit.toMillis(value.toLong())
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Cannot convert interval $interval to milliseconds", e)
        }
    }
}
