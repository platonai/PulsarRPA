package ai.platon.pulsar.crawl.common;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.persist.metadata.PageCategory;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.avro.util.Utf8;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestAnything {

    private ImmutableConfig conf = new ImmutableConfig();

    @Test
    @Ignore
    public void generateRegexUrlFilter() throws IOException {
        String[] files = {
                "conf/seeds/aboard.txt",
                "conf/seeds/bbs.txt",
                "conf/seeds/national.txt",
                "conf/seeds/papers.txt"
        };

        List<String> lines = Lists.newArrayList();
        for (String file : files) {
            lines.addAll(Files.readAllLines(Paths.get(file)));
        }

        Set<String> lines2 = Sets.newHashSet();
        lines.forEach(url -> {
            String pattern = StringUtils.substringBetween(url, "://", "/");
            pattern = "+http://" + pattern + "/(.+)";
            lines2.add(pattern);
        });

        Files.write(Paths.get("/tmp/regex-urlfilter.txt"), StringUtils.join(lines2, "\n").getBytes());

        System.out.println(lines2.size());
        System.out.println(StringUtils.join(lines2, ","));
    }

    @Test
    public void testSystem() {
        String username = System.getenv("USER");
        System.out.println(username);
    }

    @Test
    @Ignore
    public void normalizeUrlLists() throws IOException {
        String filename = "/home/vincent/Tmp/novel-list.txt";
        List<String> lines = Files.readAllLines(Paths.get(filename));
        Set<String> urls = Sets.newHashSet();
        Set<String> domains = Sets.newHashSet();
        Set<String> regexes = Sets.newHashSet();

        lines.forEach(url -> {
            int pos = StringUtils.indexOfAny(url, "abcdefjhijklmnopqrstufwxyz");
            if (pos >= 0) {
                url = url.substring(pos);
                urls.add("http://" + url);
                domains.add(url);
                regexes.add("+http://www." + url + "(.+)");
            }
        });

        Files.write(Paths.get("/tmp/domain-urlfilter.txt"), StringUtils.join(domains, "\n").getBytes());
        Files.write(Paths.get("/tmp/novel.seeds.txt"), StringUtils.join(urls, "\n").getBytes());
        Files.write(Paths.get("/tmp/regex-urlfilter.txt"), StringUtils.join(regexes, "\n").getBytes());

        System.out.println(urls.size());
        System.out.println(StringUtils.join(urls, ","));
    }

    @Test
    public void testTreeMap() {
        final Map<Integer, String> ints = new TreeMap<>(Comparator.reverseOrder());
        ints.put(1, "1");
        ints.put(2, "2");
        ints.put(3, "3");
        ints.put(4, "4");
        ints.put(5, "5");

        System.out.println(ints.keySet().iterator().next());
    }

    @Test
    public void testEnum() {
        PageCategory pageCategory;
        try {
            pageCategory = PageCategory.valueOf("APP");
        } catch (Throwable e) {
            System.out.println(e.getLocalizedMessage());
            pageCategory = PageCategory.UNKNOWN;
        }

        assertEquals(pageCategory, PageCategory.UNKNOWN);
    }

    @Test
    public void testUtf8() {
        String s = "";
        Utf8 u = new Utf8(s);
        assertEquals(0, u.length());
    }

    @Test
    public void testAtomic() {
        AtomicInteger counter = new AtomicInteger(100);
        int deleted = 10;
        counter.addAndGet(-deleted);
        System.out.println(counter);
    }

    @Test
    public void testURL() throws MalformedURLException {
        List<String> urls = Lists.newArrayList(
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
        );

        // longer url comes first
        urls.stream().sorted((u1, u2) -> u2.length() - u1.length()).forEach(System.out::println);

        urls.stream().map(URLUtil::getHostName).filter(Objects::nonNull).forEach(System.out::println);

        for (String url : urls) {
            URL u = new URL(url);
            System.out.println(u.hashCode() + ", " + url.hashCode());
        }
    }

    @Test
    public void testUrlUtil() {
        // String configuredUrl = "http://list.mogujie.com/book/jiadian/1005951 -prst --expires PT1S --auto-flush --fetch-mode NATIVE --browser NONE";
        String configuredUrl = "http://list.mogujie.com/book/jiadian/1005951";
        kotlin.Pair<String, String> pair = Urls.splitUrlArgs(configuredUrl);
        System.out.println(pair);

        LoadOptions options = LoadOptions.parse(pair.getSecond());
        System.out.println(options);
    }
}
