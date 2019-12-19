package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.PulsarOptions.Companion.split
import ai.platon.pulsar.common.options.WeightedKeywordsConverter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestJCommander {
    private val conf = ImmutableConfig()

    @Test
    fun testQuotedOptions() {
        class Cmd {
            @Parameter(names = ["-instance", "-ins"], required = true, description = "Instance ID")
            var instances: List<String> = LinkedList()
        }

        val cmd = Cmd()
        JCommander.newBuilder().addObject(cmd)
                .args(arrayOf("-ins", "\"string one\"", "-ins", "\"string two\""))
                .build()
        assertEquals(cmd.instances.size.toLong(), 2)

        val args = "-ins \"string one\" -ins \"ul li > a[href~=item]\""
        val argv = split(args)
        val cmd2 = Cmd()
        JCommander.newBuilder().addObject(cmd2).args(argv).build()
        assertEquals(cmd2.instances.size.toLong(), 2)
        assertEquals("ul li > a[href~=item]", cmd2.instances[1])
        // System.out.println(String.join(" | ", argv));
    }

    @Test
    fun testWeightedKeywordsConverter() {
        val converter = WeightedKeywordsConverter()
        val answer: MutableMap<String, Double> = HashMap()
        answer["a"] = 1.1
        answer["b"] = 2.0
        answer["c"] = 0.2
        answer["d"] = 1.0
        answer["e^"] = 1.0
        answer["^1"] = 1.0
        answer["^"] = 1.0

        val question = converter.convert("a^1.1,     b^2.0,c^0.2,d,e^,^1,^,")
        assertEquals(answer["a"], question["a"])
    }
}