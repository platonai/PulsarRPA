package ai.platon.pulsar.boilerpipe;

import ai.platon.pulsar.common.DateTimeDetector;
import com.google.common.collect.Lists;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.utils.Scent;
import ai.platon.pulsar.boilerpipe.utils.ScentUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertNotNull;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestScent {

    public static Pattern INDEX_PAGE_URL_PATTERN = Pattern.compile(".+(index|tags|chanel).+");

    public static Pattern SEARCH_PAGE_URL_PATTERN = Pattern.compile(".+(search|query|select).+");

    public static Pattern DETAIL_PAGE_URL_PATTERN = Pattern.compile(".+(detail|item|article|book|good|product|thread|view|post|content|/20[012][0-9]/{0,1}[01][0-9]/|/20[012]-[0-9]{0,1}-[01][0-9]/|/\\d{2,}/\\d{5,}|\\d{7,}).+");

    public static Pattern MEDIA_PAGE_URL_PATTERN = Pattern.compile(".+(pic|picture|video).+");

    public void extractMetadataLikeTextBlock(TextDocument doc) {
        for (TextBlock tb : doc.getTextBlocks()) {
            if (tb.getText().length() < 100) {
                if (tb.getText().contains("记者") || tb.getText().contains("作者") || tb.getText().contains("来源") || tb.getText().contains("编辑")) {
                    String message = tb.getText();
                    // message += "\t\t" + ScentUtils.extract(tb.getTextContent(), REGEX_FIELD_RULES);

                    System.out.println(message);
                    break;
                } // if
            } // if
        } // for
    }

    @Test
    public void testDateInUrl() {
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

        Set<String> oldMonth = IntStream.range(1, 10).mapToObj(m -> String.format("%02d", m)).collect(Collectors.toSet());
        Pattern invalidUrlDatePattern = Pattern.compile(".+" + 2016 + "[/\\.-]?(" + StringUtils.join(oldMonth, "|") + ").+");

        System.out.println(invalidUrlDatePattern);

        urls.stream().filter(invalidUrlDatePattern.asPredicate()).forEach(System.out::println);
    }

    public static String[] dateStrings = {
            "20170124成都分院开展党委书记纪委书记述责述廉工作",
            "成都20170124成都分院开展党委书记纪委书记述责述廉工作",
            "http://www.bjnews.com.cn/finance/20151230/432945.html",
            "2017-01-24成都分院开展党委书记纪委书记述责述廉工作",
            "17-01-24成都分院开展党委书记纪委书记述责述廉工作",
            "2017/01/24兰州分院学习贯彻中纪委七次会议精神",
            "17/01/24兰州分院学习贯彻中纪委七次会议精神",
            "兰州17/01/24兰州分院学习贯彻中纪委七次会议精神",
            "2015年08月26日上海分院召开分院系统党委书记述职述廉会",
            "千年2015年08月26日上海分院召开分院系统党委书记述职述廉会",
            "15年08月26日上海分院召开分院系统党委书记述职述廉会",
            "发布时间：15年08月26日上海分院召开分院系统党委书记述职述廉会"
    };

    public static String[] dateTimeStrings = {
            "2015年08月26日 09:33上海分院召开分院系统党委书记述职述廉会",
            "2013年07月25日 09:33",
            "2016-03-05 20:07:51    昆明信息港 查看：139",
            "白银大赛千万实盘资金派送中 2013年07月25日 09:33   来源： 中国证券报-中证网   网友评论（ 人参与 ）2014年08月26日 03:10",
            "2016-10-20 15:21:38 来源：新浪创事记 评论：",
            "时间2016-10-20 15:21:38 来源：新浪创事记 评论：",
            "2016/08/15 12:12来源： 搜狐焦点网",
            "发布时间：2016年10月25日 16:38 \t稿件来源：中安在线 \t编辑：杨艳红",
            "美灵老师 2016-12-15 17:10:50"
    };

    @Test
    public void testSniffDate() {
        DateTimeDetector detector = new DateTimeDetector();
        for (String dateString : dateStrings) {
            OffsetDateTime parsedDate = detector.detectDate(dateString);
            assertNotNull(dateString, parsedDate);
        }

        for (String dateString : dateTimeStrings) {
            OffsetDateTime parsedDate = detector.detectDateTime(dateString);
            assertNotNull(dateString, parsedDate);
        }
    }

    @Test
    public void testSniffDateTime() {
        DateTimeDetector detector = new DateTimeDetector();
        for (String dateString : dateStrings) {
            OffsetDateTime parsedDate = detector.detectDateTimeLeniently(dateString);
            // TODO: date string not detected
            // assertNull(dateString, parsedDate);
        }

        for (String dateString : dateTimeStrings) {
            OffsetDateTime parsedDate = detector.detectDateTimeLeniently(dateString);
            System.out.println(parsedDate);
            assertNotNull(dateString, parsedDate);
        }
    }

    @Test
    public void testRegexExtract() {
        String[] texts = {
                "2016-01-28 08:40:52来源： 财新网 作者：岳跃责任编辑：李箐",
                "2016-06-26 13:41:14来源： 财新网 作者：王力为 王玲责任编辑：蒋飞",
                "2016-08-09 09:32:50来源： 财新网 作者：董兢责任编辑：蒋飞",
                "2016-05-19 19:38:57来源： 财新网 作者：陈嘉慧责任编辑：黄晨",
                "2016-06-01 15:41:19来源： 财新网 作者：陈嘉慧 王琼慧责任编辑：黄晨",
                "2016-06-23 17:30:43来源： 财新网 作者：张艾华责任编辑：黄晨",
                "2016-05-19 19:38:57来源： 财新网 作者陈嘉慧责任编辑：黄晨",
                "2016-06-01 15:41:19来源： 财新网 作者陈嘉慧 王琼慧责任编辑：黄晨",
                "2016-06-23 17:30:43来源： 财新网 作者张艾华责任编辑：黄晨",
                "一个姓氏不再列表中的案例2016-06-23 17:30:43来源： 财新网 作者艾华责任编辑：晨晨晨",
                "白银大赛千万实盘资金派送中 2013年07月13日 14:34   来源： 金融界网站   网友评论（ 人参与 ）",
                "白银大赛千万实盘资金派送中 2013年07月25日 09:33   来源： 中国证券报-中证网   网友评论（ 人参与 ）",
                "白银大赛千万实盘资金派送中 2013年08月22日 15:12   来源： 青岛新闻网   网友评论（ 人参与 ）",
                "白银大赛千万实盘资金派送中 2013年09月03日 11:16   来源： 中国山东网青岛频道   网友评论（ 人参与 ）",
                "2016/05/16 11:48 来源： 搜狐焦点网",
                "2016/06/20 15:11 来源： 搜狐焦点网",
                "2016/08/15 12:12 来源： 搜狐焦点网",
                "2016/10/24 12:39 来源： 搜狐焦点网",
                "2015/12/21 08:42 来源： 中国新闻网 作者：      张婵娟",
                "2016-10-20 15:21:38 来源：新浪创事记 评论：昆明信息港讯 记者张钊 3月5日下午，“中国梦·春舞大地”2016年春城文化节启动仪式在南屏街广场举行。现场载歌载舞，欢腾一片，在喜庆祥和的氛围中，春城文化节拉开序幕。",
                "2016-11-15 14:00 来源：新华社\n当前，各地大力推进精准扶贫...。但记者采访发现，仍有一些贫困村在落实扶贫政策时“撒芝麻盐”。"
        };

        for (String text : texts) {
            Map<String, String> results = ScentUtils.extract(text, Scent.REGEX_FIELD_RULES);
            String message = text + " - " + results.toString();
            System.out.println(message);
        }
    }

    @Test
    public void testUrlPattern() throws Exception {
        System.out.println("4".matches("^\\d$"));

        String[] urls = {
                "http://cd.focus.cn/loupan/20023141/tu-14/",
                "http://finance.qq.com/dc_2016/mcontent/tagsList.htm?tags=baidu,google",
                "//house.focus.cn/msglist/2500246/"
        };

        for (String url : urls) {
            url = url.toLowerCase();
            if (!INDEX_PAGE_URL_PATTERN.matcher(url).matches()) {
                System.out.println("Not Match : " + url);
            } else {
                System.out.println("Matches : " + url);
            }
        } // for
    } // main

    @Test
    public void testDateTimeFormat() {
        System.out.println(new Date());

        String now = Instant.now().toString();
        System.out.println(now);

        DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
        ZonedDateTime zdt = ZonedDateTime.parse(now, f);
        System.out.println(zdt);

        zdt = ZonedDateTime.parse("2016-11-29T15:25:00.200Z", f);
        System.out.println(zdt);

        zdt = ZonedDateTime.parse("2016-11-29T08:00:00Z", f);
        System.out.println(zdt);
    }
}
