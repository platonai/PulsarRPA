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
package ai.platon.pulsar.common.distributed.lock.mongo.impl

import ai.platon.pulsar.common.distributed.lock.Lock
import ai.platon.pulsar.common.distributed.lock.mongo.model.LockDocument
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitWithinOffset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataMongoTest
@RunWith(SpringRunner::class)
class SimpleMongoLockTest : InitializingBean {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private lateinit var lock: Lock

    override fun afterPropertiesSet() {
        // instead of writing a custom test configuration, we can just initialize it after autowiring mongoTemplate with a custom tokenSupplier
        lock = SimpleMongoLock({ "abc" }, mongoTemplate)
    }

    @Before
    fun cleanMongoCollection() {
        mongoTemplate.dropCollection("locks")
    }

    @Test
    fun shouldLock() {
        val expectedExpiration: LocalDateTime = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS)
        val token = lock.acquire(listOf("1"), "locks", 1000)
        assertThat(token).isEqualTo("abc")
        val document = mongoTemplate.findById("1", LockDocument::class.java, "locks")
        assertThat(document?.token).isEqualTo("abc")
        assertThat(document?.expireAt).isCloseTo(expectedExpiration, TemporalUnitWithinOffset(100, ChronoUnit.MILLIS))
    }

    @Test
    fun shouldNotLock() {
        mongoTemplate.insert(LockDocument("1", LocalDateTime.now().plusMinutes(1), "def"), "locks")
        val token = lock.acquire(listOf("1"), "locks", 1000)
        assertThat(token).isNull()
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.token).isEqualTo("def")
    }

    @Test
    fun shouldRelease() {
        mongoTemplate.insert(LockDocument("1", LocalDateTime.now().plusMinutes(1), "abc"), "locks")
        val released: Boolean = lock.release(listOf("1"), "locks", "abc")
        assertThat(released).isTrue
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")).isNull()
    }

    @Test
    fun shouldNotRelease() {
        mongoTemplate.insert(LockDocument("1", LocalDateTime.now().plusMinutes(1), "def"), "locks")
        val released: Boolean = lock.release(listOf("1"), "locks", "abc")
        assertThat(released).isFalse
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.token).isEqualTo("def")
    }

    @Test
    @Throws(InterruptedException::class)
    fun shouldRefresh() {
        var expectedExpiration: LocalDateTime = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS)
        val token = lock.acquire(listOf("1"), "locks", 1000)
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.expireAt).isCloseTo(
            expectedExpiration,
            TemporalUnitWithinOffset(100, ChronoUnit.MILLIS)
        )
        Thread.sleep(500)
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.expireAt).isCloseTo(
            expectedExpiration,
            TemporalUnitWithinOffset(100, ChronoUnit.MILLIS)
        )
        expectedExpiration = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS)
        assertThat(lock.refresh(listOf("1"), "locks", token!!, 1000)).isTrue()
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.expireAt).isCloseTo(
            expectedExpiration,
            TemporalUnitWithinOffset(100, ChronoUnit.MILLIS)
        )
    }

    @Test
    fun shouldNotRefreshBecauseTokenDoesNotMatch() {
        val expectedExpiration: LocalDateTime = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS)
        lock.acquire(listOf("1"), "locks", 1000)
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.expireAt).isCloseTo(
            expectedExpiration,
            TemporalUnitWithinOffset(100, ChronoUnit.MILLIS)
        )
        assertThat(lock.refresh(listOf("1"), "locks", "wrong-token", 1000)).isFalse()
        assertThat(mongoTemplate.findById("1", LockDocument::class.java, "locks")?.expireAt).isCloseTo(
            expectedExpiration,
            TemporalUnitWithinOffset(100, ChronoUnit.MILLIS)
        )
    }

    @Test
    fun shouldNotRefreshBecauseKeyExpired() {
        assertThat(lock.refresh(listOf("1"), "locks", "abc", 1000)).isFalse()
        Assertions.assertThat<LockDocument>(mongoTemplate.findAll<LockDocument>(LockDocument::class.java))
            .isNullOrEmpty()
    }

    @SpringBootApplication
    internal open class TestApplication
}
