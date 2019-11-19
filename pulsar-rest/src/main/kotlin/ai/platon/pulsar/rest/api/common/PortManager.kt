package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.config.Params
import com.google.common.collect.Lists
import com.google.common.collect.Queues
import com.google.common.collect.Sets
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.atomic.AtomicInteger

const val BASE_PORT = 21000
const val MAX_PORT = BASE_PORT + 1000

class PortManager(
        val type: String = "",
        private val basePort: Int = BASE_PORT,
        private val maxPort: Int = MAX_PORT
) {
    private val nextPort = AtomicInteger(basePort)
    private val activePorts = Sets.newHashSet<Int>()
    private val freePorts = Queues.newPriorityQueue<Int>()

    @Synchronized
    fun getActivePorts(): List<Int> {
        return activePorts.toList()
    }

    @Synchronized
    fun getFreePorts(): List<Int> {
        return freePorts.toList()
    }

    @Synchronized
    fun acquire(): Int? {
        return getNextPort()
    }

    @Synchronized
    fun recycle(port: Int) {
        if (port in basePort..maxPort) {
            activePorts.remove(port)
            freePorts.add(port)
        }
    }

    private fun getNextPort(): Int {
        var port: Int

        if (!freePorts.isEmpty()) {
            port = freePorts.poll()
        } else {
            port = nextPort.incrementAndGet()
        }

        if (port >= basePort && port <= maxPort) {
            activePorts.add(port)
        } else {
            port = -1
        }

        return port
    }

    @Synchronized
    override fun toString(): String {
        return Params.format(
                "Next port", nextPort,
                "Active ports", StringUtils.join(activePorts, ", "),
                "Free ports", StringUtils.join(freePorts, ", ")
        )
    }
}
