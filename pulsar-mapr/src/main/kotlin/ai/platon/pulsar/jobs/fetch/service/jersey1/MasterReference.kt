package ai.platon.pulsar.jobs.fetch.service.jersey1

import ai.platon.pulsar.common.NetUtil.testHttpNetwork
import ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_HOST
import ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_PORT
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_HOST
import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_PORT
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.api.client.filter.ClientFilter
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URI
import java.util.*
import javax.ws.rs.core.MediaType

/**
 * Check PMaster is available.
 * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
 */
class MasterReference(host: String, port: Int) {

    private val baseUri: URI
    private val client = Client.create()
    private val target: WebResource

    constructor(conf: ImmutableConfig) : this(conf.get(PULSAR_MASTER_HOST, DEFAULT_PULSAR_MASTER_HOST),
            conf.getInt(PULSAR_MASTER_PORT, DEFAULT_PULSAR_MASTER_PORT)) {
    }

    init {
        this.baseUri = URI.create(String.format("http://%s:%d/%s", host, port, ROOT_PATH))
        target = client.resource(baseUri)
        LOG.info("MasterReference created, baseUri : $baseUri")
    }

    fun addFilter(filter: ClientFilter) {
        client.addFilter(filter)
    }

    fun test(): Boolean {
        try {
            return testHttpNetwork(baseUri.toURL())
        } catch (ignored: MalformedURLException) {
        }

        return false
    }

    fun echo(serverInstance: ServerInstance): ServerInstance {
        return target("service").path("echo")
                .type(MediaType.APPLICATION_JSON)
                //        .accept(MediaType.APPLICATION_JSON)
                .post(ServerInstance::class.java, serverInstance)
    }

    /**
     * Register this fetch server instance
     */
    fun register(serverInstance: ServerInstance): ServerInstance {
        return target("service").path("register")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(ServerInstance::class.java, serverInstance)
    }

    /**
     * Unregister this fetch server instance
     */
    fun unregister(serverId: Long): ServerInstance {
        return target("service").path("unregister").path(serverId.toString())
                .type(MediaType.APPLICATION_JSON)
                .delete(ServerInstance::class.java)
    }

    /**
     * Acquire a available fetch server port
     */
    fun acquirePort(type: ServerInstance.Type): Int {
        val response = target("port").path("legacy").path("acquire")
                .queryParam("type", type.name)
                .get(String::class.java)
        return Integer.parseInt(response)
    }

    /**
     * Recycle a available fetch server port
     */
    fun recyclePort(type: ServerInstance.Type, port: Int) {
        target("port")
                .path("recycle")
                .queryParam("type", type.name)
                .queryParam("port", port.toString())
                .put()
    }

    /**
     * Get all active ports
     */
    fun getFreePorts(type: ServerInstance.Type): List<Int>? {
        val response = target("port")
                .path("legacy")
                .path("free")
                .queryParam("type", type.name)
                .get(String::class.java)
        val listType = object : TypeToken<ArrayList<Int>>() {

        }.type
        return GsonBuilder().create().fromJson<List<Int>>(response, listType)
    }

    private fun target(path: String): WebResource {
        return target.path(path)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(MasterReference::class.java)
        val ROOT_PATH = "api"
    }
}
