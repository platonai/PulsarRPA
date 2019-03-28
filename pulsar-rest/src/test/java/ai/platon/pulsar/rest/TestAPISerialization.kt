package ai.platon.pulsar.rest

import ai.platon.pulsar.common.PulsarParams.ARG_SEED_PATH
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.rest.model.request.JobConfig
import ai.platon.pulsar.rest.service.JobManager
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class JTestAPISerialization {

    @Test
    fun testJobConfig() {
        val jobConfig = JobConfig("test_resource", "test_resource", JobManager.JobType.INJECT)
        jobConfig.args.put(ARG_SEED_PATH, "/tmp/1484315118872-0/")
        val gson = GsonBuilder().create()
        assertEquals("{\"crawlId\":\"test_resource\",\"confId\":\"test_resource\",\"type\":\"INJECT\",\"args\":{\"seedDir\":\"/tmp/1484315118872-0/\"}}", gson.toJson(jobConfig))
    }

    @Test
    fun testDbFilter() {
        val dbQuery = DbQuery("http://www.warpspeed.cn/0", "http://www.warpspeed.cn/3")
        val gson = GsonBuilder().create()
        assertEquals("{\"crawlId\":\"\",\"batchId\":\"all\",\"startUrl\":\"http://www.warpspeed.cn/0\",\"endUrl\":\"http://www.warpspeed.cn/3\",\"urlFilter\":\"+.\",\"start\":0,\"limit\":100,\"fields\":[]}", gson.toJson(dbQuery))

        val dbQuery2 = gson.fromJson("{\"startUrl\":\"http://www.warpspeed.cn/0\",\"endUrl\":\"http://www.warpspeed.cn/3\"}", DbQuery::class.java)
        assertEquals("", dbQuery2.crawlId)
        assertEquals("-all", dbQuery2.batchId)
        assertEquals("http://www.warpspeed.cn/0", dbQuery2.startUrl)
        assertEquals("http://www.warpspeed.cn/3", dbQuery2.endUrl)
    }
}
