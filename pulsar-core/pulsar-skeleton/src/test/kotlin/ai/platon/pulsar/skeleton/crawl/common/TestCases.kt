package ai.platon.pulsar.skeleton.crawl.common


import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.urls.URLUtils.splitUrlArgs
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.skeleton.common.options.LoadOptionDefaults
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.test.TestResourceUtil
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.avro.util.Utf8
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestCases {

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
        printlnPro(lines2.size)
        printlnPro(StringUtils.join(lines2, ","))
    }

    @Test
    fun testTreeMap() {
        val ints: MutableMap<Int, String> = TreeMap(Comparator.reverseOrder())
        ints[1] = "1"
        ints[2] = "2"
        ints[3] = "3"
        ints[4] = "4"
        ints[5] = "5"
        printlnPro(ints.keys.iterator().next())
    }

    @Test
    fun testEnum() {
        val pageCategory = try {
            PageCategory.parse("APP")
        } catch (e: Throwable) {
            printlnPro(e.localizedMessage)
            PageCategory.UNKNOWN
        }
        assertEquals(pageCategory, PageCategory.UNKNOWN)
    }

    @Test
    fun testUtf8() {
        val s = ""
        val u = Utf8(s)
        assertEquals(0, u.length.toLong())
    }

    @Test
    fun testAtomic() {
        val counter = AtomicInteger(100)
        val deleted = 10
        counter.addAndGet(-deleted)
        printlnPro(counter)
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
        val urls = listOf(
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
        urls.sortedByDescending { it.length }.forEach { printlnPro(it) }
        urls.map { InternalURLUtil.getHostName(it) }.forEach { printlnPro(it) }
        for (url in urls) {
            val u = URL(url)
            printlnPro(u.hashCode().toString() + ", " + url.hashCode())
        }
    }

    @Test
    fun testSplitUrlArgs() {
        assertTrue { LoadOptionDefaults.storeContent }
//        val configuredUrl = "https://www.amazon.com/dp/B08PP5MSVB -prst --expires PT1S --auto-flush --fetch-mode NATIVE --browser NONE"
        val configuredUrl = TestResourceUtil.PRODUCT_DETAIL_URL
        val (url, args) = splitUrlArgs(configuredUrl)
        assertEquals(configuredUrl, url)
        assertEquals("", args)
        val options = LoadOptions.parse(args, VolatileConfig())
        assertEquals(Instant.EPOCH, options.taskTime)
        assertEquals("", options.toString())
    }
}

