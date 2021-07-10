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
package ai.platon.pulsar.common.distributed.lock.configuration

import ai.platon.pulsar.common.distributed.lock.Lock
import ai.platon.pulsar.common.distributed.lock.advice.LockBeanPostProcessor
import ai.platon.pulsar.common.distributed.lock.advice.LockTypeResolver
import ai.platon.pulsar.common.distributed.lock.interval.BeanFactoryAwareIntervalConverter
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import ai.platon.pulsar.common.distributed.lock.key.KeyGenerator
import ai.platon.pulsar.common.distributed.lock.key.SpelKeyGenerator
import ai.platon.pulsar.common.distributed.lock.retry.DefaultRetriableLockFactory
import ai.platon.pulsar.common.distributed.lock.retry.RetriableLockFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import retry.DefaultRetryTemplateConverter

@Configuration
open class DistributedLockConfiguration {
    @Bean
    @ConditionalOnMissingBean
    open fun intervalConverter(@Lazy configurableBeanFactory: ConfigurableBeanFactory): IntervalConverter {
        return BeanFactoryAwareIntervalConverter(configurableBeanFactory)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun retriableLockFactory(@Lazy intervalConverter: IntervalConverter): DefaultRetriableLockFactory {
        return DefaultRetriableLockFactory(DefaultRetryTemplateConverter(intervalConverter))
    }

    @Bean
    @ConditionalOnMissingBean
    open fun spelKeyGenerator(
        @Lazy @Autowired(required = false) conversionService: ConversionService?
    ): KeyGenerator {
        return conversionService?.let { SpelKeyGenerator(it) } ?: SpelKeyGenerator(DefaultConversionService.getSharedInstance())
    }

    @Bean
    @ConditionalOnMissingBean
    open fun lockTypeResolver(@Lazy configurableBeanFactory: ConfigurableBeanFactory): LockTypeResolver {
        return LockTypeResolver { c: Class<out Lock> -> configurableBeanFactory.getBean(c) }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "ai.platon.pulsar.lock.task-scheduler.default",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun taskScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler()
    }

    companion object {
        @Bean
        @ConditionalOnMissingBean
        fun lockBeanPostProcessor(
            @Lazy keyGenerator: KeyGenerator,
            @Lazy lockTypeResolver: LockTypeResolver,
            @Lazy intervalConverter: IntervalConverter,
            @Lazy retriableLockFactory: RetriableLockFactory,
            @Lazy @Autowired(required = false) taskScheduler: TaskScheduler
        ): LockBeanPostProcessor {
            val processor = LockBeanPostProcessor(keyGenerator,
                lockTypeResolver, intervalConverter, retriableLockFactory, taskScheduler)
            processor.setBeforeExistingAdvisors(true)
            return processor
        }
    }
}
