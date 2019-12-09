package ai.platon.pulsar.jobs.fetch.service

import ai.platon.pulsar.common.NetUtil
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.io.IOException

/**
 * FetchServer is responsible to schedule fetch tasks
 */
interface FetchServer {

    fun isRunning(): Boolean

    @Throws(IOException::class)
    fun setup(applicationContext: ApplicationContext)

    fun canStart(): Boolean

    fun start()

    /**
     * Starts the fetch server.
     */
    fun startAsDaemon()

    fun shutdown(): Boolean

    /**
     * Stop the fetch server.
     *
     * @return true if no server is running or if the shutdown was successful.
     * Return false if there are running jobs and the force switch has not
     * been activated.
     */
    fun shutdownNow(): Boolean

    fun registerServiceInstance()

    fun unregisterServiceInstance()

    companion object {

        val FETCH_SERVER = "FETCH_SERVER"
        val LOG = LoggerFactory.getLogger(FetchServer::class.java)
        val BASE_PORT = 21000
        val ROOT_PATH = "/api"

        /**
         * Determine whether a server is running.
         *
         * @return true if a server instance is running.
         */
        fun isRunning(port: Int): Boolean {
            return NetUtil.testNetwork("127.0.0.1", port)
        }
    }
}
