package ai.platon.pulsar.crawl.common.options

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.*
import com.google.common.collect.Lists
import junit.framework.Assert.assertTrue
import org.apache.commons.collections4.CollectionUtils
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.time.Duration
import java.util.*
import java.util.stream.Stream

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

    private val conf = ImmutableConfig()

    @Test
    fun testNoProgramOpts() {
        for (cl in argss) {
            val opts = CrawlOptions.parse(cl, conf)
            // System.out.println(opts.toString());
            // assertEquals(opts.toString(), StringUtils.substringBefore(cl, " "), "");
        }
    }

    /**
     * TODO: Failed to parse CrawlOptions
     */
    @Test
    @Ignore("Failed to parse CrawlOptions")
    fun testProgramOpts() {
        Stream.of(args1, args2).forEach { args ->
            val options = CrawlOptions.parse(args, conf)
            println("====")
            println(args)
            println(options.toString())
            println("====")

            assertEquals(args, Duration.ofSeconds(1), options.fetchInterval)
            assertEquals(args, 2000, options.fetchPriority.toLong())

            assertEquals(args, "body", options.linkOptions.restrictCss)
            assertEquals(args, ".+", options.linkOptions.urlRegex)
            assertEquals(args, 4, options.linkOptions.minAnchorLength.toLong())
            assertEquals(args, 45, options.linkOptions.maxAnchorLength.toLong())
            assertEquals(args, 44, options.linkOptions.minUrlLength.toLong())
            assertEquals(args, 200, options.linkOptions.maxUrlLength.toLong())

            val eopts = EntityOptions.parse(args)
            assertEquals("article", eopts.getName())
            assertEquals("body", eopts.getRoot())
            assertTrue(eopts.getCssRules().containsKey("title"))
            assertTrue(eopts.getCssRules().containsValue("#title"))

            assertEquals("comments", eopts.getCollectionOptions().getName())
            assertEquals("#comments", eopts.getCollectionOptions().getRoot())
            assertEquals(".comment", eopts.getCollectionOptions().getItem())
            assertTrue(eopts.getCollectionOptions().getCssRules().containsKey("author"))
            assertTrue(eopts.getCollectionOptions().getCssRules().containsValue(".content"))
        }
    }

    @Test
    fun testRebuildOptions() {
        val options = CrawlOptions.parse(args2, conf)
        val options2 = CrawlOptions.parse(options.toString(), conf)
        CollectionUtils.containsAll(options.params.asStringMap().values, options2.params.asStringMap().values)

        println(args2)
        println(options.params.asStringMap())
        println(options2.params.asStringMap())

        val args = "-ps -rpl -nlf -notSupport"
        val loadOptions = LoadOptions.parse(args)
        assertTrue(loadOptions.params.asMap().containsKey("-persist"))
        assertTrue(loadOptions.toString().contains("-rpl"))
        assertTrue(!loadOptions.toString().contains("-notSupport"))
    }

    @Test
    fun testNormalize() {
        val args = "-en news -er #content -amin 3 -amax 100 -Fa=b"
        val options = EntityOptions.parse(args)
        println(options)
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
        assertEquals(CrawlOptions.DEFAULT, CrawlOptions(HashMap()))

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

        var argss = arrayOf("", "-i 1m -d 2 -e article -ed body -Ftitle=h2")

        private val linkFilterCommandLine = "-amin 2 -amax 45 -umin 44 -umax 200 -ucon news -ureg .+news.+"

        private val linkFilterCommandLine2 = "-amin 5 -amax 50 -umin 50 -umax 200"
    }
}
