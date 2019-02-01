package ai.platon.pulsar.crawl.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.CrawlOptions;
import ai.platon.pulsar.common.options.EntityOptions;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestPulsarOptions {
    public static String args1 = "-i pt1s -p 2000 -d 1 -css body " +
            "-amin 4 -amax 45 -umin 44 -umax 200 -ureg .+ " +
            " -en article -ed body" +
            " -Ftitle=#title -Fcontent=#content -Fauthor=#author -Fpublish_time=#publish_time" +
            " -cn comments -cr #comments -ci .comment" +
            " -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time";

    public static String args2 =
            "--fetch-interval 1s" +
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
                    " -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time";
    public static String[] argss = {
            "",
            "-i 1m -d 2 -e article -ed body -Ftitle=h2",
    };
    private static String linkFilterCommandLine = "-amin 2 -amax 45 -umin 44 -umax 200 -ucon news -ureg .+news.+";
    private static String linkFilterCommandLine2 = "-amin 5 -amax 50 -umin 50 -umax 200";
    private List<String> links = Lists.newArrayList(
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
    );

    private ImmutableConfig conf = new ImmutableConfig();

    @Test
    public void testNoProgramOpts() {
        for (String cl : argss) {
            CrawlOptions opts = CrawlOptions.parse(cl, conf);
            // System.out.println(opts.toString());
            // assertEquals(opts.toString(), StringUtils.substringBefore(cl, " "), "");
        }
    }

    /**
     * TODO: Failed to parse CrawlOptions
     * */
    @Test
    @Ignore("Failed to parse CrawlOptions")
    public void testProgramOpts() {
        Stream.of(args1, args2).forEach(args -> {
            CrawlOptions options = CrawlOptions.parse(args, conf);
            System.out.println("====");
            System.out.println(args);
            System.out.println(options.toString());
            System.out.println("====");

            assertEquals(args, Duration.ofSeconds(1), options.getFetchInterval());
            assertEquals(args, 2000, options.getFetchPriority());

            assertEquals(args, "body", options.getLinkOptions().getRestrictCss());
            assertEquals(args, ".+", options.getLinkOptions().getUrlRegex());
            assertEquals(args, 4, options.getLinkOptions().getMinAnchorLength());
            assertEquals(args, 45, options.getLinkOptions().getMaxAnchorLength());
            assertEquals(args, 44, options.getLinkOptions().getMinUrlLength());
            assertEquals(args, 200, options.getLinkOptions().getMaxUrlLength());

            EntityOptions eopts = EntityOptions.parse(args);
            assertEquals("article", eopts.getName());
            assertEquals("body", eopts.getRoot());
            assertTrue(eopts.getCssRules().containsKey("title"));
            assertTrue(eopts.getCssRules().containsValue("#title"));

            assertEquals("comments", eopts.getCollectionOptions().getName());
            assertEquals("#comments", eopts.getCollectionOptions().getRoot());
            assertEquals(".comment", eopts.getCollectionOptions().getItem());
            assertTrue(eopts.getCollectionOptions().getCssRules().containsKey("author"));
            assertTrue(eopts.getCollectionOptions().getCssRules().containsValue(".content"));
        });
    }

    @Test
    public void testRebuildOptions() {
        CrawlOptions options = CrawlOptions.parse(args2, conf);
        CrawlOptions options2 = CrawlOptions.parse(options.toString(), conf);
        CollectionUtils.containsAll(options.getParams().asStringMap().values(), options2.getParams().asStringMap().values());

        System.out.println(args2);
        System.out.println(options.getParams().asStringMap());
        System.out.println(options2.getParams().asStringMap());

        String args = "-ps -rpl -nlf -notSupport";
        LoadOptions loadOptions = LoadOptions.parse(args);
        assertTrue(loadOptions.getParams().asMap().containsKey("-ps"));
        assertTrue(loadOptions.toString().contains("-rpl"));
        assertTrue(!loadOptions.toString().contains("-notSupport"));
    }

    @Test
    public void testNormalize() {
        String args = "-en news -er #content -amin 3 -amax 100 -Fa=b";
        EntityOptions options = EntityOptions.parse(args);
        System.out.println(options);
    }

    @Test
    public void testOverrideOptions() {
        String args = "-amin=1 -amin=2 -amin=3 -amax=100 -amax=200 -amax=300";
        LinkOptions linkOptions = LinkOptions.parse(LinkOptions.normalize(args, "="));
        assertEquals(3, linkOptions.getMinAnchorLength());
        assertEquals(300, linkOptions.getMaxAnchorLength());
    }

    @Test
    public void testEmptyOptions() {
        assertEquals(CrawlOptions.DEFAULT, new CrawlOptions(""));
        assertEquals(CrawlOptions.DEFAULT, new CrawlOptions());
        assertEquals(CrawlOptions.DEFAULT, new CrawlOptions(new String[]{}));
        assertEquals(CrawlOptions.DEFAULT, new CrawlOptions(new String[]{""}));
        assertEquals(CrawlOptions.DEFAULT, new CrawlOptions(new HashMap<>()));

        assertEquals(LinkOptions.DEFAULT, new LinkOptions(""));
        assertEquals(LinkOptions.DEFAULT, new LinkOptions());
        assertEquals(LinkOptions.DEFAULT, new LinkOptions(new String[]{}));
        assertEquals(LinkOptions.DEFAULT, new LinkOptions(new String[]{""}));
        assertEquals(LinkOptions.DEFAULT, new LinkOptions(new HashMap<>()));

        System.out.println(CrawlOptions.DEFAULT);
        System.out.println(LinkOptions.DEFAULT);
    }

    @Test
    public void testLinkFilterOptions() {
        LinkOptions linkOptions = new LinkOptions(linkFilterCommandLine, conf);
        linkOptions.parse();

        System.out.println(linkFilterCommandLine);
        System.out.println(linkOptions);

        List<String> filteredLinks = links.stream()
                .filter(linkOptions.asUrlPredicate())
                .collect(Collectors.toCollection(ArrayList::new));
        System.out.println(linkOptions.build());
        System.out.println(linkOptions.getReport().stream().collect(Collectors.joining("\n")));

        assertTrue(filteredLinks.size() < links.size());
    }
}
