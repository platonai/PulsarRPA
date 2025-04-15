package ai.platon.pulsar.persist.mongo

import ai.platon.pulsar.common.NetUtil
import shaded.com.mongodb.ServerAddress

object MongoDBUtils {

    fun getServerAddresses(mongoServers: String): List<ServerAddress> {
        return mongoServers.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { host: String ->
                val parts = host.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val hostname = parts[0]
                val port = if (parts.size > 1) parts[1].toInt() else 27017
                ServerAddress(hostname, port)
            }
    }

    fun isMongoReachable(mongoServers: String): Boolean {
        return try {
            getServerAddresses(mongoServers).all { NetUtil.testNetwork(it.host, it.port) }
        } catch (e: Exception) {
            false
        }
    }
}
