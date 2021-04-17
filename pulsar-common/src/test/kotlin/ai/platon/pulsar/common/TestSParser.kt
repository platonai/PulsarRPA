package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.SParser
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-1-14.
 */
class TestSParser {
    private val parser = SParser()

    @Test
    fun testParseCollection() {
        parser.set("a\nb,\nc,\nd")
        assertEquals(3, parser.trimmedStringCollection.size.toLong())
        parser.trimmedStringCollection.stream().map { l: String -> l + " -> " + l.length }
            .forEach { x: String? -> println(x) }
        parser.set("a,\nb,\nc,\nd")
        assertEquals(4, parser.trimmedStringCollection.size.toLong())
    }

    @Test
    fun testParseInstant() {
        val zoneId = ZoneId.systemDefault()
        parser.set("2021-04-17")
        assertEquals("2021-04-17", parser.getInstant().atZone(zoneId).toLocalDate().toString())

        parser.set("2021-04-17T16:00:00")
        assertEquals("2021-04-17T16:00:00", parser.getInstant().atZone(zoneId).toLocalDate().toString())
    }

    @Test
    fun testParseDuration() {
        // Hadoop format
//        conf.set("t1", "1ms");
//        assertEquals(Duration.ofMillis(1).toMillis(), conf.getTimeDuration("t1", Integer.MIN_VALUE, TimeUnit.MILLISECONDS));
        parser.set("1s")
        assertEquals(Duration.ofSeconds(1), parser.duration)
        parser.set("pt1s")
        assertEquals(Duration.ofSeconds(1), parser.duration)
        parser.set("1s")
        assertEquals(Duration.ofSeconds(1), parser.duration)
        parser.set("1h")
        assertEquals(Duration.ofHours(1), parser.duration)
        parser.set("1ms")
        assertEquals(Duration.ofMillis(1), parser.duration)
        parser.set("500ms")
        assertEquals(Duration.ofMillis(500), parser.duration)
    }
}