package ai.platon.pulsar.persist

import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.PulsarPaths.DATA_DIR
import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_EMBED_MONGO_SERVER
import de.flapdoodle.embed.mongo.Command
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodProcess
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder
import de.flapdoodle.embed.mongo.config.Storage
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.runtime.Network
import org.slf4j.LoggerFactory

/**
 * TODO: use spring boot instead
 * */
class EmbedMongoDB(val server: String = DEFAULT_EMBED_MONGO_SERVER) {

    private val log = LoggerFactory.getLogger(EmbedMongoDB::class.java)

    val version: Version.Main = Version.Main.DEVELOPMENT
    val dataDirectory = PulsarPaths.get(DATA_DIR, "mongo")
    val ip = server.substringBefore(':')
    val port = server.substringAfter(':').toInt()

    private var mongodExe: MongodExecutable? = null
    private var mongod: MongodProcess? = null

    /**
     * Initiate the MongoDB server on the default port
     */
    fun start() {
        val runtimeConfig = RuntimeConfigBuilder()
                .defaultsWithLogger(Command.MongoD, log)
                .daemonProcess(false) // should shutdown manually after all data flushed
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build()

        val runtime = MongodStarter.getInstance(runtimeConfig)

        val replication = Storage(dataDirectory.toString(), null, 0)
        val mongodConfig = MongodConfigBuilder()
                .version(version)
                .replication(replication)
                .net(Net(port, Network.localhostIsIPv6())).build()

        log.info("Starting embedded Mongodb server on port $port")
        try {
            mongodExe = runtime.prepare(mongodConfig)
            mongod = mongodExe?.start()
        } catch (e: Exception) {
            log.error("Error starting embedded Mongodb server... tearing down test driver.")
            stop()
            // re-throw the error
            throw e
        }
    }

    /**
     * Stop the server
     */
    fun stop() {
        log.info("Shutting down mongodb server...")
        mongod?.stop()
        mongodExe?.stop()
    }
}
