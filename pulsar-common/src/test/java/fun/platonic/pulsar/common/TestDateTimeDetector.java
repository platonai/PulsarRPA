package fun.platonic.pulsar.common;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class TestDateTimeDetector {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testParseDateTime() {
        String t = "2017-02-06T02:15:11.174Z";
        Instant dateTime = DateTimeUtil.parseInstant(t, Instant.EPOCH);
        assertEquals(t, DateTimeFormatter.ISO_INSTANT.format(dateTime));
//    System.out.println(dateTime);
//    System.out.println(Instant.parse(t));
    }

    @Test
    public void testParseDateStrictly() throws ParseException {
        Instant dateTime = LocalDateTime.parse("2015-12-30T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015.12.30", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015/12/30", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("20151230", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015年12月30日", "yy年MM月dd日").toInstant());

        dateTime = LocalDateTime.parse("2015-02-04T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015.02.04", "yy.M.d").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015.2.4", "yy.M.d").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015.2.4", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015.2.4", "yyyy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015/02/04", "yy/M/d").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015/02/04", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("20150204", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDateStrictly("2015年2月4日", "yy年M月d日").toInstant());
    }

    @Test
    public void testParseDate() throws ParseException {
        Instant dateTime = LocalDateTime.parse("2015-12-30T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDate("2015.12.30", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015/12/30", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("20151230", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015年12月30日", "yy年MM月dd日").toInstant());

        dateTime = LocalDateTime.parse("2015-02-04T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDate("2015.02.04", "yy.M.d").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015.2.4", "yy.M.d").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015.2.4", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015/02/04", "yy/M/d").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015/02/04", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("20150204", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015年2月4日", "yy年M月d日").toInstant());
    }

    @Test
    public void testParseYearMonth() throws ParseException {
        YearMonth yearMonth = YearMonth.parse("2015-12");
        // System.out.println(yearMonth.atDay(1).atStartOfDay());
        assertEquals("2015-12-01T00:00", yearMonth.atDay(1).atStartOfDay().toString());
    }

    @Test
    public void testParseMalformedDate() throws ParseException {
        exception.expect(ParseException.class);
        DateUtils.parseDate("2015.2.4HELLO", "yy.MM.dd");
    }

    @Test
    public void testParseMalformedDate2() throws ParseException {
        exception.expect(IllegalArgumentException.class);
        DateUtils.parseDate("2015.2.4", "yy.MM.ddHELLO");
    }

    @Test
    public void testParseDateWithLeniency() throws ParseException {
        Instant dateTime = LocalDateTime.parse("2015-12-02T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDate("2015.11.32", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015/11/32", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("20151132", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015年11月32日", "yy年MM月dd日").toInstant());

        dateTime = LocalDateTime.parse("2016-01-02T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(dateTime, DateUtils.parseDate("2015.13.02", "yy.MM.dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015/13/02", "yy/MM/dd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("20151302", "yyyyMMdd").toInstant());
        assertEquals(dateTime, DateUtils.parseDate("2015年13月02日", "yy年MM月dd日").toInstant());
    }

    @Test
    public void testDateTimeDetector() {
        DateTimeDetector detector = new DateTimeDetector();
        ZoneId zoneId = ZoneId.systemDefault();

        String oldTexts[] = {
                "http://www.bjnews.com.cn/finance/20151230/432945.html",
                "http://www.bjnews.com.cn/finance/2015/12/30/432945.html",
                "http://www.bjnews.com.cn/finance/2015-12-30/432945.html",
                "http://www.bjnews.com.cn/finance/15-12-30/432945.html",
                "http://www.bjnews.com.cn/finance/15/12/30/432945.html",

                "http://www.bjnews.com.cn/finance/2015/2/3/432945.html",
                "http://www.bjnews.com.cn/finance/2015-2-3/432945.html",
                "http://www.bjnews.com.cn/finance/15-2-3/432945.html",
                "http://www.bjnews.com.cn/finance/15/2/3/432945.html",

                "http://news.17173.com/content/2012-09-20/20120920110949379_1.shtml"
        };
        for (String text : oldTexts) {
            OffsetDateTime dateTime = detector.detectDate(text);
            assertNotNull(text, dateTime);
            assertTrue(text + ", " + dateTime.toInstant(), detector.containsOldDate(text, 1, zoneId));
        }

        String invalideTexts[] = {
                "http://www.bjnews.com.cn/finance/20151260/432945.html",
                "http://www.bjnews.com.cn/finance/2015/12/60/432945.html",
                "http://www.bjnews.com.cn/finance/2015-12-60/432945.html",
                "http://www.bjnews.com.cn/finance/15-12-60/432945.html",
                "http://www.bjnews.com.cn/finance/15/12/60/432945.html"
        };
        for (String text : invalideTexts) {
            assertNull(detector.detectDate(text));
        }
    }

    @Test
    public void testDetectDateTime() {
        DateTimeDetector detector = new DateTimeDetector();
        ZoneId zoneId = ZoneId.systemDefault();

        ArrayList<String> texts = Lists.newArrayList(
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
                "2017/03/24 12:39 来源： 搜狐焦点网",
                "2015/12/21 08:42 来源： 中国新闻网 作者：      张婵娟",
                "2016-10-20 15:21:38 来源：新浪创事记 评论：昆明信息港讯 记者张钊 3月5日下午，“中国梦·春舞大地”2016年春城文化节启动仪式在南屏街广场举行。现场载歌载舞，欢腾一片，在喜庆祥和的氛围中，春城文化节拉开序幕。",
                "2016-11-15 14:00 来源：新华社\n当前，各地大力推进精准扶贫...。\t但记者采访发现，仍有一些贫困村在落实扶贫政策时“撒芝麻盐”。"
        );

//    texts.clear();
//    texts.add("2016-11-15 14:00 来源：新华社\n当前，各地大力推进精准扶贫...。但记者采访发现，仍有一些贫困村在落实扶贫政策时“撒芝麻盐”。");

        for (String text : texts) {
            OffsetDateTime dateTime = detector.detectDateTime(text);
            // System.out.println(dateTime);
            assertNotNull(text, dateTime);
            assertTrue(text + ", " + dateTime, detector.containsOldDate(text, 1, zoneId));
        }
    }

    @Test
    public void testOldDateTimeString() {
        DateTimeDetector detector = new DateTimeDetector();
        ZoneId zoneId = ZoneId.systemDefault();

        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        String validUrls[] = {
                "http://www.bjnews.com.cn/finance/" + yearMonth + "/01/432945.html",
                "http://www.bjnews.com.cn/finance/" + yearMonth + "/26/432945.html"
        };

        for (String url : validUrls) {
            OffsetDateTime dateTime = detector.detectDate(url);
            assertNotNull(url, dateTime);
            assertFalse(url + ", " + dateTime, detector.containsOldDate(url, 32, zoneId));
        }

        String oldUrls[] = {
                "http://www.bjnews.com.cn/finance/2013-12-10/432945.html",
                "http://www.bjnews.com.cn/finance/2016/12/10/432945.html",
                "http://www.bjnews.com.cn/finance/20151230/432945.html",
                "http://www.bjnews.com.cn/finance/2016/12/30/432945.html",
        };

        for (String url : oldUrls) {
            OffsetDateTime dateTime = detector.detectDate(url);
            assertNotNull(url, dateTime);
            assertTrue(url + ", " + dateTime, detector.containsOldDate(url, 15, zoneId));
        }
    }
}
