/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.platon.pulsar.jobs;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.persist.WebPage;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.junit.Before;
import org.junit.Test;
import org.platon.pulsar.jobs.common.FetchEntry;
import org.platon.pulsar.jobs.common.SelectorEntry;
import org.platon.pulsar.jobs.common.URLPartitioner;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Tests {@link URLPartitioner}
 */
public class TestURLPartitioner {

    private Configuration conf = new MutableConfig().unbox();

    @Before
    public void setup() {
    }

    /**
     * tests one reducer, everything goes into one partition, using host
     * partitioner.
     */
    @Test
    public void testOneReducer() {
        URLPartitioner partitioner = new URLPartitioner();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);
        partitioner.setConf(conf);

        int numReduceTasks = 1;

        assertEquals(0, partitioner.getPartition("http://example.org", numReduceTasks));
        assertEquals(0, partitioner.getPartition("http://www.apache.org", numReduceTasks));
    }

    /**
     * tests partitioning by host
     */
    @Test
    public void testModeHost() {
        URLPartitioner partitioner = new URLPartitioner();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);
        partitioner.setConf(conf);

        int numReduceTasks = 100;

        int partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks);
        int partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks);
        assertNotSame("partitions should differ because of different host", partitionWithoutWWW, partitionWithWWW);

        int partitionSame1 = partitioner.getPartition("http://www.example.org/paris", numReduceTasks);
        int partitionSame2 = partitioner.getPartition("http://www.example.org/london", numReduceTasks);
        assertEquals("partitions should be same because of same host", partitionSame1, partitionSame2);
    }

    /**
     * tests partitioning by domain
     */
    @Test
    public void testModeDomain() {
        URLPartitioner partitioner = new URLPartitioner();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_DOMAIN);
        partitioner.setConf(conf);

        int numReduceTasks = 100;

        int partitionExample = partitioner.getPartition("http://www.example.org/", numReduceTasks);
        int partitionApache = partitioner.getPartition("http://www.apache.org/", numReduceTasks);
        assertNotSame("partitions should differ because of different domain", partitionExample, partitionApache);

        int partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks);
        int partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks);
        assertEquals("partitions should be same because of same domain", partitionWithoutWWW, partitionWithWWW);
    }

    /**
     * tests partitioning by IP
     */
    @Test
    public void testModeIP() {
        URLPartitioner partitioner = new URLPartitioner();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_IP);
        partitioner.setConf(conf);

        int numReduceTasks = 100;

        int partitionExample = partitioner.getPartition("http://www.example.org/", numReduceTasks);
        int partitionApache = partitioner.getPartition("http://www.apache.org/", numReduceTasks);
        assertNotSame("partitions should differ because of different ip", partitionExample, partitionApache);

        int partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks);
        int partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks);
        // the following has dependendy on example.org (that is has the same ip as
        // www.example.org)
        assertEquals("partitions should be same because of same ip", partitionWithoutWWW, partitionWithWWW);
    }

    /**
     * Test the seed functionality, using host partitioner.
     */
    @Test
    public void testSeed() {
        URLPartitioner partitioner = new URLPartitioner();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);
        partitioner.setConf(conf);

        int numReduceTasks = 100;
        int partitionNoSeed = partitioner.getPartition("http://example.org/", numReduceTasks);

        conf.setInt(URLPartitioner.PARTITION_URL_SEED, 1);
        partitioner.setConf(conf);

        int partitionWithSeed = partitioner.getPartition("http://example.org/", numReduceTasks);

        assertNotSame("partitions should differ because of different seed", partitionNoSeed, partitionWithSeed);
    }

    /**
     * Tests the {@link URLPartitioner.SelectorEntryPartitioner}.
     */
    @Test
    public void testSelectorEntryPartitioner() {
        // The reference partitioner
        URLPartitioner refPartitioner = new URLPartitioner();

        // The to be tested partitioner with specific text
        URLPartitioner.SelectorEntryPartitioner sigPartitioner = new URLPartitioner.SelectorEntryPartitioner();

        Configuration conf = new MutableConfig().unbox();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);

        refPartitioner.setConf(conf);
        sigPartitioner.setConf(conf);

        int numReduceTasks = 100;

        String url = "http://www.example.org/";
        int partitionFromRef = refPartitioner.getPartition(url, numReduceTasks);
        // init selector entry (score shouldn't matter)
        SelectorEntry selectorEntry = new SelectorEntry(url, 1337);
        WebPage page = WebPage.newWebPage(url);
        int partitionFromSig = sigPartitioner.getPartition(selectorEntry, page.unbox(), numReduceTasks);

        assertEquals("partitions should be same", partitionFromRef, partitionFromSig);
    }

    /**
     * Tests the {@link URLPartitioner.SelectorEntryPartitioner}.
     */
    @Test
    public void testSelectorEntryPartitioner2() {
        int numReduceTasks = 50;
        String urls[] = {
                "http://biz.eastmoney.com/news/1675,20160511622807454.html",
                "http://biz.eastmoney.com/news/1675,20160918674154131.html",
                "http://by.gansudaily.com.cn/system/2016/10/11/016442631.shtml",
                "http://cq.focus.cn/daogou/11165589.html",
                "http://cq.focus.cn/loupan/40074/tu380050202.html",
                "http://cq.focus.cn/msgview/43086/362946563.html",
                "http://data.eastmoney.com/notice_n/20161021/JTgr25dksTANPH5pwqQld5.html",
                "http://enterprise.eastmoney.com/news/1683,20161013672582291.html",
                "http://estate.caijing.com.cn/20161010/4184304.shtml",
                "http://forum.home.news.cn/space/userinfo.do?id=94937791",
                "http://global.eastmoney.com/news/1781,20160511623001266.html"
        };

        Configuration conf = new MutableConfig().unbox();
        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);
        URLPartitioner partitioner = new URLPartitioner(conf);
        for (String url : urls) {
            int partition = partitioner.getPartition(url, numReduceTasks);
            System.out.println(partition + " <- " + url);
        }

        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_DOMAIN);
        URLPartitioner partitioner2 = new URLPartitioner(conf);
        for (String url : urls) {
            int partition = partitioner2.getPartition(url, numReduceTasks);
            System.out.println(partition + " <- " + url);
        }
    }

    /**
     * Tests the {@link URLPartitioner.FetchEntryPartitioner}
     *
     * @throws MalformedURLException
     */
    @Test
    public void testFetchEntryPartitioner() throws MalformedURLException {
        // The reference partitioner
        URLPartitioner refPartitioner = new URLPartitioner();

        // The to be tested partitioner with specific text
        URLPartitioner.FetchEntryPartitioner sigPartitioner = new URLPartitioner.FetchEntryPartitioner();

        conf.setEnum(URLPartitioner.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);

        refPartitioner.setConf(conf);
        sigPartitioner.setConf(conf);

        int numReduceTasks = 100;

        String url = "http://www.example.org/";
        int partitionFromRef = refPartitioner.getPartition(url, numReduceTasks);
        IntWritable intWritable = new IntWritable(1337); // doesn't matter
        WebPage page = WebPage.newWebPage(url);
        String key = page.getKey();
        FetchEntry fetchEntry = new FetchEntry(conf, key, page);
        int partitionFromSig = sigPartitioner.getPartition(intWritable, fetchEntry, numReduceTasks);

        assertEquals("partitions should be same", partitionFromRef, partitionFromSig);
    }
}
