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
package ai.platon.pulsar.common.distributed.lock.advice

import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import ai.platon.pulsar.common.distributed.lock.key.KeyGenerator
import ai.platon.pulsar.common.distributed.lock.retry.RetriableLockFactory
import org.aopalliance.intercept.Interceptor
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.TaskScheduler

/**
 * [org.springframework.beans.factory.config.BeanPostProcessor] for beans with [Locked] methods.
 */
class LockBeanPostProcessor(
    val keyGenerator: KeyGenerator,
    val lockTypeResolver: LockTypeResolver,
    val intervalConverter: IntervalConverter,
    val retriableLockFactory: RetriableLockFactory,
    val taskScheduler: TaskScheduler
) : AbstractAdvisingBeanPostProcessor(), InitializingBean {

    override fun afterPropertiesSet() {
        val pointcut = AnnotationMatchingPointcut(null, Locked::class.java, true)
        val interceptor: Interceptor = LockMethodInterceptor(
            keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, taskScheduler)
        advisor = DefaultPointcutAdvisor(pointcut, interceptor)
    }
}
