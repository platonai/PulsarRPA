package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlUtils.splitUrlArgs
import ai.platon.pulsar.persist.metadata.PageCategory
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.avro.util.Utf8
import org.apache.commons.lang3.StringUtils
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.test.assertEquals

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestCases {
    private val volatileConfig = VolatileConfig()

    @Test
    @Ignore
    @Throws(IOException::class)
    fun generateRegexUrlFilter() {
        val files = arrayOf(
            "config/seeds/aboard.txt",
            "config/seeds/bbs.txt",
            "config/seeds/national.txt",
            "config/seeds/papers.txt"
        )
        val lines: MutableList<String> = Lists.newArrayList()
        for (file in files) {
            lines.addAll(Files.readAllLines(Paths.get(file)))
        }
        val lines2: MutableSet<String?> = Sets.newHashSet()
        lines.forEach(Consumer { url: String? ->
            var pattern = StringUtils.substringBetween(url, "://", "/")
            pattern = "+http://$pattern/(.+)"
            lines2.add(pattern)
        })
        Files.write(Paths.get("/tmp/regex-urlfilter.txt"), StringUtils.join(lines2, "\n").toByteArray())
        println(lines2.size)
        println(StringUtils.join(lines2, ","))
    }

    @Test
    @Ignore
    @Throws(IOException::class)
    fun normalizeUrlLists() {
        val filename = "/home/vincent/Tmp/novel-list.txt"
        val lines = Files.readAllLines(Paths.get(filename))
        val urls: MutableSet<String?> = Sets.newHashSet()
        val domains: MutableSet<String?> = Sets.newHashSet()
        val regexes: MutableSet<String?> = Sets.newHashSet()
        lines.forEach {
            val pos = StringUtils.indexOfAny(it, "abcdefjhijklmnopqrstufwxyz")
            if (pos >= 0) {
                val url = it.substring(pos)
                urls.add("http://$url")
                domains.add(url)
                regexes.add("+http://www.$url(.+)")
            }
        }
        Files.write(Paths.get("/tmp/domain-urlfilter.txt"), StringUtils.join(domains, "\n").toByteArray())
        Files.write(Paths.get("/tmp/novel.seeds.txt"), StringUtils.join(urls, "\n").toByteArray())
        Files.write(Paths.get("/tmp/regex-urlfilter.txt"), StringUtils.join(regexes, "\n").toByteArray())
        println(urls.size)
        println(StringUtils.join(urls, ","))
    }

    @Test
    fun testTreeMap() {
        val ints: MutableMap<Int, String> = TreeMap(Comparator.reverseOrder())
        ints[1] = "1"
        ints[2] = "2"
        ints[3] = "3"
        ints[4] = "4"
        ints[5] = "5"
        println(ints.keys.iterator().next())
    }

    @Test
    fun testEnum() {
        val pageCategory: PageCategory
        pageCategory = try {
            PageCategory.parse("APP")
        } catch (e: Throwable) {
            println(e.localizedMessage)
            PageCategory.UNKNOWN
        }
        Assert.assertEquals(pageCategory, PageCategory.UNKNOWN)
    }

    @Test
    fun testUtf8() {
        val s = ""
        val u = Utf8(s)
        Assert.assertEquals(0, u.length.toLong())
    }

    @Test
    fun testAtomic() {
        val counter = AtomicInteger(100)
        val deleted = 10
        counter.addAndGet(-deleted)
        println(counter)
    }

    @Test
    fun testReturnToLabel() {
        var i = 0
        IntRange(1, 10).forEach {
            i = it
            if (i == 5) {
                return@forEach
            }
        }
        assertEquals(10, i)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testURL() {
        val urls: List<String> = Lists.newArrayList(
            "http://bond.eastmoney.com/news/1326,20160811671616734.html",
            "http://bond.eastmoney.com/news/1326,20161011671616734.html",
            "http://tech.huanqiu.com/photo/2016-09/2847279.html",
            "http://tech.hexun.com/2016-09-12/186368492.html",
            "http://opinion.cntv.cn/2016/04/17/ARTI1397735301366288.shtml",
            "http://tech.hexun.com/2016-11-12/186368492.html",
            "http://ac.cheaa.com/2016/0905/488888.shtml",
            "http://ankang.hsw.cn/system/2016/0927/16538.shtml",
            "http://auto.nbd.com.cn/articles/2016-09-28/1042037.html",
            "http://bank.cnfol.com/pinglunfenxi/20160901/23399283.shtml",
            "http://bank.cnfol.com/yinhanglicai/20160905/23418323.shtml"
        )
        // longer url comes first
        urls.sortedByDescending { it.length }.forEach { println(it) }
        urls.mapNotNull { URLUtil.getHostName(it) }.forEach { println(it) }
        for (url in urls) {
            val u = URL(url)
            println(u.hashCode().toString() + ", " + url.hashCode())
        }
    }

    @Test
    fun testSplitUrlArgs() {
        // String configuredUrl = "http://list.mogujie.com/book/jiadian/1005951 -prst --expires PT1S --auto-flush --fetch-mode NATIVE --browser NONE";
        val configuredUrl = "http://list.mogujie.com/book/jiadian/1005951"
        val (url, args) = splitUrlArgs(configuredUrl)
        assertEquals(configuredUrl, url)
        assertEquals("", args)
        val options = LoadOptions.parse(args, volatileConfig)
        assertEquals("", options.toString())
    }
}
