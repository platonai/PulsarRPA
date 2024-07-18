package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import org.apache.commons.lang3.StringUtils
import java.time.Duration
import kotlin.test.*

/**
 * Created by vincent on 17-1-14.
 */
class TestConfig {

    @Ignore("Legacy config is deprecated")
    @Test
    fun testConfig() {
        var conf = ImmutableConfig()
        println(conf.toString())
        assertFalse("pulsar-site.xml" in conf.toString())
        conf = ImmutableConfig(profile = "default", loadDefaults = true)
        assertTrue("pulsar-site.xml" in conf.toString())
        assertEquals("test", conf["pulsar.config.id"])
        assertEquals("pulsar_test_crawl_id", conf["storage.crawl.id"])
    }

    @Test
    fun testDuration() {
        val conf = MutableConfig()
        // ISO-8601 format
        conf["d1"] = "p3d"
        conf["d2"] = "pt2h"
        conf["d3"] = "pt3m"
        conf["d4"] = "-pt3s"
        assertEquals("PT72H", conf.getDuration("d1", Duration.ZERO).toString())
        assertEquals("PT2H", conf.getDuration("d2", Duration.ZERO).toString())
        assertEquals("PT3M", conf.getDuration("d3", Duration.ZERO).toString())
        assertEquals("PT-3S", conf.getDuration("d4", Duration.ZERO).toString())

        // Hadoop format
        conf["hd1"] = "1d"
        conf["hd2"] = "3m"
        conf["hd3"] = "5s"
        assertEquals("PT24H", conf.getDuration("hd1", Duration.ZERO).toString())
        assertEquals("PT3M", conf.getDuration("hd2", Duration.ZERO).toString())
        assertEquals("PT5S", conf.getDuration("hd3", Duration.ZERO).toString())
    }

    @Test
    fun testCollection() {
        val conf = MutableConfig()
        conf["test.collection"] = "a\nb,\nc,\nd"
        assertEquals(3, conf.getTrimmedStringCollection("test.collection").size.toLong())
        conf.getTrimmedStringCollection("test.collection").stream().map { l: String -> l + " -> " + l.length }
            .forEach { x: String? -> println(x) }
        conf["test.collection"] = "a,\nb,\nc,\nd"
        assertEquals(4, conf.getTrimmedStringCollection("test.collection").size.toLong())
    }

    @Test
    fun testStrings() {
        val conf = MutableConfig()
        val n1 = "n1"
        val v1 = "a,b,c,d"
        conf[n1] = v1
        assertEquals(v1, conf[n1])
        assertEquals(4, conf.getStrings(n1).size)
    }

    @Test
    fun testStrings2() {
        val conf = VolatileConfig()
        val n1 = "n1"
        val v1 = "a,b,c,d"
        conf[n1] = v1
        assertEquals(v1, conf[n1])
        assertEquals(4, conf.getStrings(n1).size)
    }

    @Test
    fun testFallback() {
        val mutableConfig = MutableConfig()
        val n1 = "n1"
        val v1 = "a,b,c,d"
        mutableConfig[n1] = v1
        val conf = VolatileConfig(mutableConfig)
        assertEquals(v1, conf[n1])
        println(StringUtils.join(conf.getStrings(n1), ", "))
        assertEquals(4, conf.getStrings(n1).size)
    }
}