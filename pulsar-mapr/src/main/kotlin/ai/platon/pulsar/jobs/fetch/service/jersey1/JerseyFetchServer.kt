package ai.platon.pulsar.jobs.fetch.service.jersey1

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants.JOB_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.jobs.fetch.service.FetchResource
import ai.platon.pulsar.jobs.fetch.service.FetchServer
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory
import com.sun.jersey.api.core.ClassNamesResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import org.apache.hadoop.classification.InterfaceStability
import org.glassfish.grizzly.http.server.HttpServer
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * JerseyFetchServer is responsible to schedule fetch tasks
 */
@InterfaceStability.Unstable
class JerseyFetchServer(private val conf: ImmutableConfig) : FetchServer {
    lateinit var baseUri: URI
    private var resourceConfig: ResourceConfig? = null
    private var isActive = false
    private var server: HttpServer? = null
    private lateinit var serverInstance: ServerInstance
    private lateinit var masterReference: MasterReference

    @Throws(IOException::class)
    override fun setup(applicationContext: ApplicationContext) {
        this.masterReference = MasterReference(conf)
        if (!masterReference.test()) {
            FetchServer.LOG.warn("Failed to create fetch server : PMaster is not available")
            return
        }

        val port = masterReference.acquirePort(ServerInstance.Type.FetchService)
        if (port < FetchServer.BASE_PORT) {
            FetchServer.LOG.warn("Failed to create fetch server : can not acquire a valid port")
            return
        }

        this.baseUri = URI.create(String.format("http://%s:%d%s", "127.0.0.1", port, FetchServer.ROOT_PATH))
        this.resourceConfig = ClassNamesResourceConfig(FetchResource::class.java)
    }

    override fun canStart(): Boolean {
        if (isRunning()) {
            FetchServer.LOG.warn("Fetch server is already running")
            return false
        }

        return resourceConfig != null && server == null
    }

    override fun start() {
        if (!canStart()) {
            FetchServer.LOG.warn("FetchServer is not initialized properly, will not start")
            return
        }

        FetchServer.LOG.info("Starting fetch server on port: {}", baseUri.port)

        try {
            this.server = GrizzlyServerFactory.createHttpServer(baseUri, resourceConfig!!)
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { this.shutdown() }))
            registerServiceInstance()
            isActive = true
        } catch (e: IOException) {
            FetchServer.LOG.error(e.toString())
        }
    }

    /**
     * Starts the fetch server.
     */
    override fun startAsDaemon() {
        val t = Thread(Runnable { this.start() })
        t.isDaemon = true
        t.start()
    }

    override fun shutdown(): Boolean {
        return shutdownNow()
    }

    /**
     * Stop the fetch server.
     *
     * @return true if no server is running or if the shutdown was successful.
     * Return false if there are running jobs and the force switch has not
     * been activated.
     */
    override fun shutdownNow(): Boolean {
        if (!isActive) {
            return true
        }

        if (server == null) {
            FetchServer.LOG.warn("FetchServer is not initialized")
            return false
        }

        try {
            unregisterServiceInstance()
        } catch (e: Throwable) {
            FetchServer.LOG.error(StringUtil.stringifyException(e))
        } finally {
            server!!.stop()
            isActive = false
        }

        FetchServer.LOG.info("FetchServer is stopped. Port : {}", baseUri.port)
        return true
    }

    override fun isRunning(): Boolean {
        return server != null && server!!.isStarted && FetchServer.isRunning(baseUri.port)
    }

    override fun registerServiceInstance() {
        // We use an Internet ip rather than an Intranet ip
        serverInstance = ServerInstance("", baseUri.port, ServerInstance.Type.FetchService.name)
        serverInstance = masterReference.register(serverInstance)
        FetchServer.LOG.info("Registered ServerInstance $serverInstance")
    }

    override fun unregisterServiceInstance() {
        masterReference.recyclePort(ServerInstance.Type.FetchService, baseUri.port)
        serverInstance = masterReference.unregister(serverInstance.id)
        FetchServer.LOG.info("UnRegistered ServerInstance $serverInstance")
    }
}

fun main() {
    val context = ClassPathXmlApplicationContext(JOB_CONTEXT_CONFIG_LOCATION)
    val fetchServer = context.getBean(JerseyFetchServer::class.java)
    // JerseyFetchServer fetchServer1 = new JerseyFetchServer(context.getBean(ImmutableConfig.class));
    fetchServer.setup(context)

    if (!fetchServer.canStart()) {
        println("Can not start FetchServer")
    }

    fetchServer.startAsDaemon()
    println("Application started.\nTry out " + fetchServer.baseUri)
    while (true) {
        println("Hit X to exit : ")
        val cmd = Scanner(System.`in`).nextLine()
        if (cmd.equals("x", ignoreCase = true)) {
            fetchServer.shutdown()
            break
        }
    }
}
