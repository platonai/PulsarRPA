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
package ai.platon.pulsar.common.distributed.lock

import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class Locked(
    /**
     * Flag to indicate if lock will be manually released.
     * By default, lock will be released after method execution.
     */
    val manuallyReleased: Boolean = false,
    /**
     * Id of a specific store for lock to use.
     * For JDBC, this would be a lock table.
     * For Mongo, this would be a collection name.
     */
    val storeId: String = "lock",
    /**
     * Prefix of all generated lock keys.
     */
    val prefix: String = "",
    /**
     * SpEL expression with all arguments passed as SpEL variables `args` and available execution context.
     * By default, it will evaluate to the absolute method path (class + method) by evaluating a special 'executionPath' variable.
     */
    val expression: String = "#executionPath",
    /**
     * Lock expiration interval. This indicates how long the lock should be considered locked after acquiring and when it should be invalidated.
     * If [.refresh] is positive, lock expiration will periodically be refreshed. This is useful for tasks that can occasionally hang for
     * longer than their expiration. This enables long-running task to keep the lock for a long time, but release it relatively quickly in case they fail.
     */
    val expiration: Interval = Interval(
        value = "10",
        unit = TimeUnit.SECONDS
    ),
    /**
     * Lock timeout interval. The maximum time to wait for lock. If lock is not acquired in this interval, lock is considered to be taken and
     * lock cannot be given to the annotated method.
     */
    val timeout: Interval = Interval(
        value = "1",
        unit = TimeUnit.SECONDS
    ),
    /**
     * Lock retry interval. How long to wait before trying to acquire the lock again after it was not acquired.
     */
    val retry: Interval = Interval(
        value = "50"
    ),
    /**
     * Lock refresh interval indicated how often should the lock be refreshed during method execution. If it is non-positive, lock will not
     * be refreshed during the execution and maximum time the lock can be held is defined by the [.expiration] in this case.
     */
    val refresh: Interval = Interval(
        value = "0"
    ),
    /**
     * Lock type, see implementations of [Lock].
     */
    val type: KClass<out Lock> = Lock::class
)
