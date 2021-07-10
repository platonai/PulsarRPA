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

import ai.platon.pulsar.common.distributed.lock.Lock
import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.exception.DistributedLockException
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import ai.platon.pulsar.common.distributed.lock.key.KeyGenerator
import ai.platon.pulsar.common.distributed.lock.retry.RetriableLockFactory
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.scheduling.TaskScheduler
import org.springframework.util.StringUtils
import java.lang.reflect.Method
import java.util.concurrent.ScheduledFuture

class LockMethodInterceptor(
    val keyGenerator: KeyGenerator,
    val lockTypeResolver: LockTypeResolver,
    val intervalConverter: IntervalConverter,
    val retriableLockFactory: RetriableLockFactory,
    val taskScheduler: TaskScheduler,
) : MethodInterceptor {
    private val log = LoggerFactory.getLogger(LockMethodInterceptor::class.java)

    @Throws(Throwable::class)
    override fun invoke(invocation: MethodInvocation): Any? {
        val context = LockContext(invocation)
        return try {
            executeLockedMethod(invocation, context)
        } finally {
            cleanAfterExecution(context)
        }
    }

    @Throws(Throwable::class)
    private fun executeLockedMethod(invocation: MethodInvocation, context: LockContext): Any? {
        val expiration = intervalConverter.toMillis(context.locked.expiration)
        try {
            val lock = retriableLockFactory.generate(context.lock, context.locked)
            context.token = lock.acquire(context.keys, context.locked.storeId, expiration)
        } catch (e: Exception) {
            throw DistributedLockException(
                String.format("Unable to acquire lock with expression: %s", context.locked.expression), e)
        }
        log.debug("Acquired lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)
        scheduleLockRefresh(context, expiration)
        return invocation.proceed()
    }

    private fun scheduleLockRefresh(context: LockContext, expiration: Long) {
        val refresh = intervalConverter.toMillis(context.locked.refresh)
        if (refresh > 0) {
            context.scheduledFuture = taskScheduler.scheduleAtFixedRate(constructRefreshRunnable(context, expiration), refresh)
        }
    }

    private fun constructRefreshRunnable(context: LockContext, expiration: Long): Runnable {
        return Runnable { context.lock.refresh(context.keys, context.locked.storeId, context.token!!, expiration) }
    }

    private fun cleanAfterExecution(context: LockContext) {
        val scheduledFuture = context.scheduledFuture
        if (scheduledFuture != null && !scheduledFuture.isCancelled && !scheduledFuture.isDone) {
            scheduledFuture.cancel(true)
        }

        if (StringUtils.hasText(context.token) && !context.locked.manuallyReleased) {
            val released: Boolean = context.lock.release(context.keys, context.locked.storeId, context.token!!)
            if (released) {
                log.debug("Released lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)
            } else {
                // this could indicate that locks are released before method execution is finished and that locks expire too soon
                // this could also indicate a problem with the store where locks are held, connectivity issues or query problems
                log.error("Couldn't release lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)
            }
        }
    }

    private inner class LockContext(
        val invocation: MethodInvocation,
        var method: Method = AopUtils.getMostSpecificMethod(invocation.method, invocation.getThis()!!.javaClass),
        var locked: Locked = AnnotatedElementUtils.findMergedAnnotation(method, Locked::class.java) ?: throw IllegalArgumentException("No Locked annotation found"),
        var lock: Lock = lockTypeResolver[locked.type.java] ?: throw DistributedLockException(String.format("Lock type %s not configured", locked.type)),
        var token: String? = null,
        var scheduledFuture: ScheduledFuture<*>? = null,
    ) {
        var keys: List<String> = resolveKeys(invocation, method, locked)

        init {
            validateConstructedContext()
        }

        private fun resolveKeys(invocation: MethodInvocation, method: Method, locked: Locked): List<String> {
            return try {
                keyGenerator.resolveKeys(locked.prefix, locked.expression, invocation.getThis()!!, method, invocation.arguments)
            } catch (e: RuntimeException) {
                throw DistributedLockException(
                    String.format("Cannot resolve keys to lock: %s on method %s", locked, method), e)
            }
        }

        private fun validateConstructedContext() {
            if (locked.expression.isEmpty()) {
                throw DistributedLockException(String.format("Missing expression: %s on method %s", locked, method))
            }
        }
    }
}
