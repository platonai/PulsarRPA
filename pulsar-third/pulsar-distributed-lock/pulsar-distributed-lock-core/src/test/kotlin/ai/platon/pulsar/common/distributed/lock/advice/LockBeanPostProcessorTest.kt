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

import ai.platon.pulsar.common.distributed.lock.Interval
import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.advice.support.SimpleLock
import ai.platon.pulsar.common.distributed.lock.advice.support.SimpleLocked
import ai.platon.pulsar.common.distributed.lock.interval.BeanFactoryAwareIntervalConverter
import ai.platon.pulsar.common.distributed.lock.interval.IntervalConverter
import ai.platon.pulsar.common.distributed.lock.key.SpelKeyGenerator
import ai.platon.pulsar.common.distributed.lock.retry.DefaultRetriableLockFactory
import ai.platon.pulsar.common.distributed.lock.retry.RetriableLockFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import retry.DefaultRetryTemplateConverter
import java.util.concurrent.TimeUnit

class LockBeanPostProcessorTest {
    private lateinit var lockedInterface: LockedInterface
    private lateinit var lock: SimpleLock

    @Before
    fun setUp() {
        val beanFactory = DefaultListableBeanFactory()
        lock = SimpleLock()
        val lockTypeResolver = Mockito.mock(LockTypeResolver::class.java)
        Mockito.`when`(lockTypeResolver[SimpleLock::class.java]).thenReturn(lock)
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.afterPropertiesSet()
        val keyGenerator = SpelKeyGenerator(DefaultConversionService())
        val intervalConverter: IntervalConverter = BeanFactoryAwareIntervalConverter(beanFactory)
        val retriableLockFactory: RetriableLockFactory =
            DefaultRetriableLockFactory(DefaultRetryTemplateConverter(intervalConverter))
        val processor =
            LockBeanPostProcessor(keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, scheduler)
        processor.afterPropertiesSet()
        beanFactory.addBeanPostProcessor(processor)
        beanFactory.registerBeanDefinition(
            "lockedService",
            RootBeanDefinition(LockedInterface::class.java) { LockedInterfaceImpl() })
        lockedInterface = beanFactory.getBean(LockedInterface::class.java)
        beanFactory.preInstantiateSingletons()
    }

    @Test
    fun shouldLockInheritedFromInterface() {
        lockedInterface.doLocked(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("lock:hello")
    }

    @Test
    fun shouldLockInheritedFromInterfaceWithAlias() {
        lockedInterface.doLockedWithAlias(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("hello") // @SimpleLock does not add prefix
    }

    @Test
    fun shouldLockOverridenFromInterface() {
        lockedInterface.doLockedOverriden(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("lock:1")
    }

    @Test
    fun shouldLockOverridenFromInterfaceWithAlias() {
        lockedInterface.doLockedOverridenWithAlias(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("1") // @SimpleLock does not add prefix
    }

    @Test
    fun shouldLockFromImplementation() {
        lockedInterface.doLockedFromImplementation(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("lock:hello")
    }

    @Test
    fun shouldLockFromImplementationWithAlias() {
        lockedInterface.doLockedFromImplementationWithAlias(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("hello") // @SimpleLock does not add prefix
    }

    @Test
    fun shouldLockWithImplementationDetail() {
        lockedInterface.doLockedWithImplementationDetail(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("lock:4")
    }

    @Test
    fun shouldLockWithExecutionPath() {
        lockedInterface.doLockedWithExecutionPath()
        assertThat(lock.getLockedKeys("lock")).containsExactly("ai.platon.pulsar.common.distributed.lock.advice.LockBeanPostProcessorTest.LockedInterfaceImpl.doLockedWithExecutionPath")
    }

    @Test
    fun shouldLockFromImplementationWithImplementationDetail() {
        lockedInterface.doLockedFromImplementationWithImplementationDetail(1, "hello")
        assertThat(lock.getLockedKeys("lock")).containsExactly("lock:4")
    }

    @Test
    @Throws(InterruptedException::class)
    fun shouldRefreshLock() {
        lockedInterface.sleep()
        val lockedKey: SimpleLock.LockedKey = lock.lockMap["lock"]!![0]
        assertThat(lockedKey.updatedAt).withFailMessage(lockedKey.toString())
            .isCloseTo(System.currentTimeMillis(), Offset.offset(200L))
        assertThat(lockedKey.key).isEqualTo("ai.platon.pulsar.common.distributed.lock.advice.LockBeanPostProcessorTest.LockedInterfaceImpl.sleep")

        // sleep method sleeps for 1 seconds and lock is refreshed every 100ms for a total of 10 refreshes
        // sometimes the refresh will execute slightly before releasing the lock so additional refresh will be fired, but lock will be released immediately after it is refreshed
        assertThat(lockedKey.updateCounter).isIn(10L, 11L)
        lock.lockMap.clear()
    }

    private interface LockedInterface {
        @Locked(prefix = "lock:", expression = "#s", type = SimpleLock::class)
        fun doLocked(num: Int, s: String?)

        @SimpleLocked(expression = "#s")
        fun doLockedWithAlias(num: Int, s: String?)

        @Locked(prefix = "lock:", expression = "#s", type = SimpleLock::class)
        fun doLockedOverriden(num: Int, s: String?)

        @SimpleLocked(expression = "#s")
        fun doLockedOverridenWithAlias(num: Int, s: String?)
        fun doLockedFromImplementation(num: Int, s: String?)
        fun doLockedFromImplementationWithAlias(num: Int, s: String?)

        @Locked(prefix = "lock:", expression = "getStaticValue()", type = SimpleLock::class)
        fun doLockedWithImplementationDetail(num: Int, s: String?)
        fun doLockedFromImplementationWithImplementationDetail(num: Int, s: String?)

        @SimpleLocked
        fun doLockedWithExecutionPath()

        @SimpleLocked(
            refresh = Interval("100"), expiration = Interval(
                "200"
            )
        )
        @Throws(InterruptedException::class)
        fun sleep()
    }

    private inner class LockedInterfaceImpl : LockedInterface {
        override fun doLocked(num: Int, s: String?) {}
        override fun doLockedWithAlias(num: Int, s: String?) {}

        @Locked(prefix = "lock:", expression = "#num", type = SimpleLock::class)
        override fun doLockedOverriden(num: Int, s: String?) {
        }

        @SimpleLocked(expression = "#num")
        override fun doLockedOverridenWithAlias(num: Int, s: String?) {
        }

        @Locked(prefix = "lock:", expression = "#s", type = SimpleLock::class)
        override fun doLockedFromImplementation(num: Int, s: String?) {
        }

        @SimpleLocked(expression = "#s")
        override fun doLockedFromImplementationWithAlias(num: Int, s: String?) {
        }

        override fun doLockedWithImplementationDetail(num: Int, s: String?) {}

        @Locked(prefix = "lock:", expression = "getStaticValue()", type = SimpleLock::class)
        override fun doLockedFromImplementationWithImplementationDetail(num: Int, s: String?) {
        }

        override fun doLockedWithExecutionPath() {}

        @Throws(InterruptedException::class)
        override fun sleep() {
            TimeUnit.SECONDS.sleep(1)
        }

        val staticValue: Int
            get() = 4
    }
}