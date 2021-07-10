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
package ai.platon.pulsar.common.distributed.lock.mongo.alias

import ai.platon.pulsar.common.distributed.lock.Interval
import ai.platon.pulsar.common.distributed.lock.Locked
import ai.platon.pulsar.common.distributed.lock.mongo.impl.SimpleMongoLock
import java.util.concurrent.TimeUnit

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Locked(type = SimpleMongoLock::class)
annotation class MongoLocked(
    val manuallyReleased: Boolean = false,
    val storeId: String = "lock",
    val prefix: String = "",
    val expression: String = "#executionPath",
    val expiration: Interval = Interval(value = "10", unit = TimeUnit.SECONDS),
    val timeout: Interval = Interval(value = "1", unit = TimeUnit.SECONDS),
    val retry: Interval = Interval(value = "50"),
    val refresh: Interval = Interval(value = "0")
)
