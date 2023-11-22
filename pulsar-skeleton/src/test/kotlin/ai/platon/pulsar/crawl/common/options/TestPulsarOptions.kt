package ai.platon.pulsar.crawl.common.options

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.*
import ai.platon.pulsar.common.options.deprecated.CrawlOptions
import com.google.common.collect.Lists
import org.apache.commons.collections4.CollectionUtils
import org.junit.Assert.assertEquals
import kotlin.test.*
import java.util.*
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarOptions {
    private val links = Lists.newArrayList(
        "http://www.news.cn/comments/index.htm",
        "http://xinhuanet.com/silkroad/index.htm",
        "http://www.news.cn/video/xhwsp/index.htm",
        "http://www.xinhuanet.com/company/legal.htm",
        "http://www.xinhuanet.com/politics/xhll.htm",
        "http://www.xinhuanet.com/datanews/index.htm",
        "http://www.news.cn/politics/leaders/index.htm",
        "http://www.xinhuanet.com/company/copyright.htm",
        "http://www.xinhuanet.com/company/contact-us.htm",
        "http://forum.home.news.cn/detail/140976763/1.html",
        "http://www.bjnews.com.cn/news/2017/06/20/447390.html",
        "http://www.bjnews.com.cn/news/2017/06/20/447403.html",
        "http://www.bjnews.com.cn/news/2017/06/19/447269.html",
        "http://www.bjnews.com.cn/news/2017/06/20/447414.html",
        "http://www.bjnews.com.cn/news/2017/06/20/447354.html",
        "http://www.bjnews.com.cn/news/2017/06/19/447316.html"
    )

    private val conf = VolatileConfig()

    @Test
    fun testRebuildOptions() {
        val options = CrawlOptions.parse(args2, conf)
        val options2 = CrawlOptions.parse(options.toString(), conf)
        CollectionUtils.containsAll(options.params.asStringMap().values, options2.params.asStringMap().values)

        println(args2)
        println(options.params.asStringMap())
        println(options2.params.asStringMap())

        val args = "-ps -rpl -nlf -notSupport"
        val loadOptions = LoadOptions.parse(args, conf)
        val finalArgs = loadOptions.toString()
        assertTrue(loadOptions.params.asMap().containsKey("-persist"))
        assertTrue(finalArgs.contains("-reparseLinks"))
        assertTrue(!finalArgs.contains("-notSupport"))
    }

    @Test
    fun testQuotedArguments() {
        val args = "-expires PT1S -incognito -outlinkSelector \".products a\" -retry"
        val argv = PulsarOptions.split(args)
        assertEquals("-expires", argv[0])
        assertEquals("PT1S", argv[1])

        assertEquals("-outlinkSelector", argv[3])
        assertEquals("\".products a\"", argv[4])
    }

    @Test
    fun testOverrideOptions() {
        val args = "-amin=1 -amin=2 -amin=3 -amax=100 -amax=200 -amax=300"
        val linkOptions = LinkOptions.parse(PulsarOptions.normalize(args, "="))
        assertEquals(3, linkOptions.minAnchorLength.toLong())
        assertEquals(300, linkOptions.maxAnchorLength.toLong())
    }

    @Test
    fun testEmptyOptions() {
        assertEquals(CrawlOptions.DEFAULT, CrawlOptions(""))
        assertEquals(CrawlOptions.DEFAULT, CrawlOptions())
        assertEquals(CrawlOptions.DEFAULT, CrawlOptions(arrayOf()))
        assertEquals(CrawlOptions.DEFAULT, CrawlOptions(arrayOf("")))

        assertEquals(LinkOptions.DEFAULT, LinkOptions(""))
        assertEquals(LinkOptions.DEFAULT, LinkOptions())
        assertEquals(LinkOptions.DEFAULT, LinkOptions(arrayOf()))
        assertEquals(LinkOptions.DEFAULT, LinkOptions(arrayOf("")))
        assertEquals(LinkOptions.DEFAULT, LinkOptions(HashMap()))

        println(CrawlOptions.DEFAULT)
        println(LinkOptions.DEFAULT)
    }

    @Test
    fun testLinkFilterOptions1() {
        val linkOptions = LinkOptions(linkFilterCommandLine, conf)
        // linkOptions.parse()
    }

    @Test
    fun testLinkFilterOptions() {
        val linkOptions = LinkOptions(linkFilterCommandLine, conf)
        linkOptions.parse()

        println(linkFilterCommandLine)
        println(linkOptions)

        val filteredLinks = links.filter { linkOptions.asUrlPredicate().test(it) }
        println(linkOptions.build())
        println(linkOptions.getReport())

        assertTrue(filteredLinks.size < links.size)
    }

    @Test
    fun testOptionHelp() {
        LoadOptions.helpList.forEach {
            println(it)
        }
    }

    @Test
    fun testFixArity1() {
        val search = "-cacheContent"
        val argsList = listOf(
            "-label test $search",
            "-label test $search -parse",
            "$search -parse"
        )
        argsList.forEach { args ->
            val args1 = OptionUtils.arity0ToArity1(args, search)
            assertTrue("$search true" in args1, args)
        }
    }

    companion object {
        var args1 = "-i pt1s -p 2000 -d 1 -css body " +
                "-amin 4 -amax 45 -umin 44 -umax 200 -ureg .+ " +
                " -en article -ed body" +
                " -Ftitle=#title -Fcontent=#content -Fauthor=#author -Fpublish_time=#publish_time" +
                " -cn comments -cr #comments -ci .comment" +
                " -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time"

        var args2 = "--fetch-interval 1s" +
                " --fetch-priority 2000" +
                " --depth 2 " +
                //      " --seed-dom=body" +
                //      " --seed-url=.+" +
                //      " --seed-anchor=2,4" +
                //      " --seed-sequence=100,1000" +
                //      " --seed-fetch-interval=30m" +
                " -log 4" +
                " --restrict-css body" +
                " --url-regex .+" +
                " --url-min-length 44" +
                " --url-max-length 200" +
                " --anchor-min-length 4" +
                " --anchor-max-length 45" +
                " --entity-name article" +
                " --entity-root body" +
                " -Ftitle=#title -Fcontent=#content -Fauthor=#author -Fpublish_time=#publish_time" +
                " --collection-name comments" +
                " --collection-root #comments" +
                " --collection-item .comment" +
                " -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time"

        var argv = arrayOf("", "-i 1m -d 2 -e article -ed body -Ftitle=h2")

        private val linkFilterCommandLine = "-amin 2 -amax 45 -umin 44 -umax 200 -ucon news -ureg .+news.+"

        private val linkFilterCommandLine2 = "-amin 5 -amax 50 -umin 50 -umax 200"
    }
}
