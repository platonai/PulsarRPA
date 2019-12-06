package ai.platon.pulsar.jobs.fetch.service

import ai.platon.pulsar.crawl.fetch.FetchJobForwardingResponse
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.TaskSchedulers
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata
import com.google.common.collect.Lists
import com.sun.jersey.spi.resource.Singleton
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType


@Component
@Singleton
@Path(value = "/fetch")
@Produces(MediaType.APPLICATION_JSON)
class FetchResource {

    @Autowired
    private val taskSchedulers: TaskSchedulers? = null

    @GET
    @Path("/schedule/{count}")
    fun getFetchItems(@PathParam("count") count: Int): List<FetchTask.Key> {
        var count = count
        val keys = Lists.newArrayList<FetchTask.Key>()

        if (count < 0) {
            LOG.debug("Invalid count $count")
            return keys
        }

        if (count > MAX_TASKS_PER_SCHEDULE) {
            count = MAX_TASKS_PER_SCHEDULE
        }

        return taskSchedulers!!.randomFetchItems(count)
    }

    /**
     * Jersey1 may not support return a list of integer
     */
    @GET
    @Path("/scheduler/list")
    fun listScheduers(): List<Int> {
        return taskSchedulers!!.schedulerIds()
    }

    @GET
    @Path("/scheduler/listCommands")
    fun listCommands(): List<String> {
        return taskSchedulers!!.schedulerIds().map { "curl http://localhost:8182/fetch/stop/$it" }
    }

    @PUT
    @Path("/stop/{schedulerId}")
    fun stop(@PathParam("schedulerId") schedulerId: Int) {
        val taskScheduler = taskSchedulers!![schedulerId]
        if (taskScheduler != null) {
            // taskScheduler.stop();
        }
    }


    /**
     * Accept page content from satellite(crowdsourcing web fetcher),
     * the content should be put with media getType "text/html; charset='UTF-8'"
     *
     * TODO : does the framework make a translation between string and byte array?
     * which might affect the performance
     */
    @PUT
    @Path("/submit")
    @Consumes("text/html; charset='UTF-8'")
    @Produces("text/html; charset='UTF-8'")
    fun finishFetchItem(@javax.ws.rs.core.Context httpHeaders: HttpHeaders, content: ByteArray): String {
        val customHeaders = SpellCheckedMultiMetadata()
        for ((key, value1) in httpHeaders.requestHeaders) {
            var name = key.toLowerCase()

            // Q- means meta-data from satellite
            // F- means forwarded headers by satellite
            // ant other headers are information between satellite and this server
            if (name.startsWith("f-") || name.startsWith("q-")) {
                if (name.startsWith("f-")) {
                    name = name.substring("f-".length)
                }

                for (value in value1) {
                    customHeaders.put(name, value)
                }
            }
        }

        //    log.debug("headers-1 : {}", httpHeaders.getRequestHeaders().entrySet());
        //    log.debug("headers-2 : {}", customHeaders);

        val fetchResult = FetchJobForwardingResponse(customHeaders, content)
        val taskScheduler = taskSchedulers!![fetchResult.jobId]
        taskScheduler!!.produce(fetchResult)

        return "success"
    }

    companion object {

        val LOG = LoggerFactory.getLogger(FetchServer::class.java)

        val MAX_TASKS_PER_SCHEDULE = 100
    }
}// taskSchedulers =
