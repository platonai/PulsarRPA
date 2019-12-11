/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jobs

import ai.platon.pulsar.common.URLUtil.GroupMode
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.jobs.common.FetchEntryWritable
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.common.URLPartitioner
import ai.platon.pulsar.jobs.common.URLPartitioner.FetchEntryPartitioner
import ai.platon.pulsar.jobs.common.URLPartitioner.SelectorEntryPartitioner
import ai.platon.pulsar.persist.WebPage
import org.apache.hadoop.io.IntWritable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.MalformedURLException

/**
 * Tests [URLPartitioner]
 */
class TestURLPartitioner {
    private val conf = MutableConfig().unbox()
    @Before
    fun setup() {
    }

    /**
     * tests one reducer, everything goes into one partition, using host
     * partitioner.
     */
    @Test
    fun testOneReducer() {
        val partitioner = URLPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        partitioner.conf = conf
        val numReduceTasks = 1
        Assert.assertEquals(0, partitioner.getPartition("http://example.org", numReduceTasks).toLong())
        Assert.assertEquals(0, partitioner.getPartition("http://www.apache.org", numReduceTasks).toLong())
    }

    /**
     * tests partitioning by host
     */
    @Test
    fun testModeHost() {
        val partitioner = URLPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        partitioner.conf = conf
        val numReduceTasks = 100
        val partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks)
        val partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks)
        Assert.assertNotSame("partitions should differ because of different host", partitionWithoutWWW, partitionWithWWW)
        val partitionSame1 = partitioner.getPartition("http://www.example.org/paris", numReduceTasks)
        val partitionSame2 = partitioner.getPartition("http://www.example.org/london", numReduceTasks)
        Assert.assertEquals("partitions should be same because of same host", partitionSame1.toLong(), partitionSame2.toLong())
    }

    /**
     * tests partitioning by domain
     */
    @Test
    fun testModeDomain() {
        val partitioner = URLPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_DOMAIN)
        partitioner.conf = conf
        val numReduceTasks = 100
        val partitionExample = partitioner.getPartition("http://www.example.org/", numReduceTasks)
        val partitionApache = partitioner.getPartition("http://www.apache.org/", numReduceTasks)
        Assert.assertNotSame("partitions should differ because of different domain", partitionExample, partitionApache)
        val partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks)
        val partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks)
        Assert.assertEquals("partitions should be same because of same domain", partitionWithoutWWW.toLong(), partitionWithWWW.toLong())
    }

    /**
     * tests partitioning by IP
     */
    @Test
    fun testModeIP() {
        val partitioner = URLPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_IP)
        partitioner.conf = conf
        val numReduceTasks = 100
        val partitionExample = partitioner.getPartition("http://www.example.org/", numReduceTasks)
        val partitionApache = partitioner.getPartition("http://www.apache.org/", numReduceTasks)
        Assert.assertNotSame("partitions should differ because of different ip", partitionExample, partitionApache)
        val partitionWithoutWWW = partitioner.getPartition("http://example.org/", numReduceTasks)
        val partitionWithWWW = partitioner.getPartition("http://www.example.org/", numReduceTasks)
        // the following has dependendy on example.org (that is has the same ip as
// www.example.org)
        Assert.assertEquals("partitions should be same because of same ip", partitionWithoutWWW.toLong(), partitionWithWWW.toLong())
    }

    /**
     * Test the seed functionality, using host partitioner.
     */
    @Test
    fun testSeed() {
        val partitioner = URLPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        partitioner.conf = conf
        val numReduceTasks = 100
        val partitionNoSeed = partitioner.getPartition("http://example.org/", numReduceTasks)
        conf.setInt(CapabilityTypes.PARTITION_URL_SEED, 1)
        partitioner.conf = conf
        val partitionWithSeed = partitioner.getPartition("http://example.org/", numReduceTasks)
        Assert.assertNotSame("partitions should differ because of different seed", partitionNoSeed, partitionWithSeed)
    }

    /**
     * Tests the [URLPartitioner.SelectorEntryPartitioner].
     */
    @Test
    fun testSelectorEntryPartitioner() { // The reference partitioner
        val refPartitioner = URLPartitioner()
        // The to be tested partitioner with specific text
        val sigPartitioner = SelectorEntryPartitioner()
        val conf = MutableConfig().unbox()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        refPartitioner.conf = conf
        sigPartitioner.conf = conf
        val numReduceTasks = 100
        val url = "http://www.example.org/"
        val partitionFromRef = refPartitioner.getPartition(url, numReduceTasks)
        // init selector entry (score shouldn't matter)
        val selectorEntry = SelectorEntry(url, 1337)
        val page = WebPage.newWebPage(url)
        val partitionFromSig = sigPartitioner.getPartition(selectorEntry, page.unbox(), numReduceTasks)
        Assert.assertEquals("partitions should be same", partitionFromRef.toLong(), partitionFromSig.toLong())
    }

    /**
     * Tests the [URLPartitioner.SelectorEntryPartitioner].
     */
    @Test
    fun testSelectorEntryPartitioner2() {
        val numReduceTasks = 50
        val urls = arrayOf(
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
        )
        val conf = MutableConfig().unbox()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        val partitioner = URLPartitioner(conf)
        for (url in urls) {
            val partition = partitioner.getPartition(url, numReduceTasks)
            println("$partition <- $url")
        }
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_DOMAIN)
        val partitioner2 = URLPartitioner(conf)
        for (url in urls) {
            val partition = partitioner2.getPartition(url, numReduceTasks)
            println("$partition <- $url")
        }
    }

    /**
     * Tests the [URLPartitioner.FetchEntryPartitioner]
     *
     * @throws MalformedURLException
     */
    @Test
    @Throws(MalformedURLException::class)
    fun testFetchEntryPartitioner() { // The reference partitioner
        val refPartitioner = URLPartitioner()
        // The to be tested partitioner with specific text
        val sigPartitioner = FetchEntryPartitioner()
        conf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, GroupMode.BY_HOST)
        refPartitioner.conf = conf
        sigPartitioner.conf = conf
        val numReduceTasks = 100
        val url = "http://www.example.org/"
        val partitionFromRef = refPartitioner.getPartition(url, numReduceTasks)
        val intWritable = IntWritable(1337) // doesn't matter
        val page = WebPage.newWebPage(url)
        val key = page.key
        val fetchEntry = FetchEntryWritable(conf, key, page)
        val partitionFromSig = sigPartitioner.getPartition(intWritable, fetchEntry, numReduceTasks)
        Assert.assertEquals("partitions should be same", partitionFromRef.toLong(), partitionFromSig.toLong())
    }
}