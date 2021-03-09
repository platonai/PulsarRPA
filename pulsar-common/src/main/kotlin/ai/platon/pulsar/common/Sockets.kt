package ai.platon.pulsar.common

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.util.*
import javax.net.ServerSocketFactory

/**
 * Simple utility methods for working with network sockets  for example,
 * for finding available ports on `localhost`.
 *
 *
 * Within this class, a TCP port refers to a port for a [ServerSocket];
 * whereas, a UDP port refers to a port for a [DatagramSocket].
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 4.0
 */
object Sockets {
    /**
     * The default minimum value for port ranges used when finding an available
     * socket port.
     */
    const val PORT_RANGE_MIN = 1024
    /**
     * The default maximum value for port ranges used when finding an available
     * socket port.
     */
    const val PORT_RANGE_MAX = 65535
    private val random = Random(System.currentTimeMillis())

    /**
     * Find an available TCP port randomly selected from the range
     * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    @JvmOverloads
    fun findAvailableTcpPort(minPort: Int = PORT_RANGE_MIN, maxPort: Int = PORT_RANGE_MAX): Int {
        return SocketType.TCP.findAvailablePort(minPort, maxPort)
    }
    /**
     * Find the requested number of available TCP ports, each randomly selected
     * from the range [`minPort`, `maxPort`].
     * @param numRequested the number of available ports to find
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return a sorted set of available TCP port numbers
     * @throws IllegalStateException if the requested number of available ports could not be found
     */
    /**
     * Find the requested number of available TCP ports, each randomly selected
     * from the range [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     * @param numRequested the number of available ports to find
     * @return a sorted set of available TCP port numbers
     * @throws IllegalStateException if the requested number of available ports could not be found
     */
    @JvmOverloads
    fun findAvailableTcpPorts(numRequested: Int, minPort: Int = PORT_RANGE_MIN, maxPort: Int = PORT_RANGE_MAX): SortedSet<Int> {
        return SocketType.TCP.findAvailablePorts(numRequested, minPort, maxPort)
    }
    /**
     * Find an available UDP port randomly selected from the range
     * [`minPort`, `maxPort`].
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available UDP port number
     * @throws IllegalStateException if no available port could be found
     */
    /**
     * Find an available UDP port randomly selected from the range
     * [`minPort`, {@value #PORT_RANGE_MAX}].
     * @param minPort the minimum port number
     * @return an available UDP port number
     * @throws IllegalStateException if no available port could be found
     */
    /**
     * Find an available UDP port randomly selected from the range
     * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     * @return an available UDP port number
     * @throws IllegalStateException if no available port could be found
     */
    @JvmOverloads
    fun findAvailableUdpPort(minPort: Int = PORT_RANGE_MIN, maxPort: Int = PORT_RANGE_MAX): Int {
        return SocketType.UDP.findAvailablePort(minPort, maxPort)
    }
    /**
     * Find the requested number of available UDP ports, each randomly selected
     * from the range [`minPort`, `maxPort`].
     * @param numRequested the number of available ports to find
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return a sorted set of available UDP port numbers
     * @throws IllegalStateException if the requested number of available ports could not be found
     */
    /**
     * Find the requested number of available UDP ports, each randomly selected
     * from the range [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     * @param numRequested the number of available ports to find
     * @return a sorted set of available UDP port numbers
     * @throws IllegalStateException if the requested number of available ports could not be found
     */
    @JvmOverloads
    fun findAvailableUdpPorts(numRequested: Int, minPort: Int = PORT_RANGE_MIN, maxPort: Int = PORT_RANGE_MAX): SortedSet<Int> {
        return SocketType.UDP.findAvailablePorts(numRequested, minPort, maxPort)
    }

    private enum class SocketType {
        TCP {
            override fun isPortAvailable(port: Int): Boolean {
                return try {
                    val serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                            port, 1, InetAddress.getByName("localhost"))
                    serverSocket.close()
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        },
        UDP {
            override fun isPortAvailable(port: Int): Boolean {
                return try {
                    val socket = DatagramSocket(port, InetAddress.getByName("localhost"))
                    socket.close()
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        };

        /**
         * Determine if the specified port for this `SocketType` is
         * currently available on `localhost`.
         */
        protected abstract fun isPortAvailable(port: Int): Boolean

        /**
         * Find a pseudo-random port number within the range
         * [`minPort`, `maxPort`].
         * @param minPort the minimum port number
         * @param maxPort the maximum port number
         * @return a random port number within the specified range
         */
        private fun findRandomPort(minPort: Int, maxPort: Int): Int {
            val portRange = maxPort - minPort
            return minPort + random.nextInt(portRange + 1)
        }

        /**
         * Find an available port for this `SocketType`, randomly selected
         * from the range [`minPort`, `maxPort`].
         * @param minPort the minimum port number
         * @param maxPort the maximum port number
         * @return an available port number for this socket type
         * @throws IllegalStateException if no available port could be found
         */
        fun findAvailablePort(minPort: Int, maxPort: Int): Int {
            require(minPort > 0) { "'minPort' must be greater than 0" }
            require(maxPort >= minPort) { "'maxPort' must be greater than or equal to 'minPort'" }
            require(maxPort <= PORT_RANGE_MAX) { "'maxPort' must be less than or equal to $PORT_RANGE_MAX" }
            val portRange = maxPort - minPort
            var candidatePort: Int
            var searchCounter = 0
            do {
                check(searchCounter <= portRange) {
                    String.format(
                            "Could not find an available %s port in the range [%d, %d] after %d attempts",
                            name, minPort, maxPort, searchCounter)
                }
                candidatePort = findRandomPort(minPort, maxPort)
                searchCounter++
            } while (!isPortAvailable(candidatePort))
            return candidatePort
        }

        /**
         * Find the requested number of available ports for this `SocketType`,
         * each randomly selected from the range [`minPort`, `maxPort`].
         * @param numRequested the number of available ports to find
         * @param minPort the minimum port number
         * @param maxPort the maximum port number
         * @return a sorted set of available port numbers for this socket type
         * @throws IllegalStateException if the requested number of available ports could not be found
         */
        fun findAvailablePorts(numRequested: Int, minPort: Int, maxPort: Int): SortedSet<Int> {
            require(minPort > 0) { "'minPort' must be greater than 0"}
            require(maxPort > minPort) { "'maxPort' must be greater than 'minPort'"}
            require(maxPort <= PORT_RANGE_MAX) { "'maxPort' must be less than or equal to $PORT_RANGE_MAX"}
            require(numRequested > 0) { "'numRequested' must be greater than 0"}
            require(maxPort - minPort >= numRequested) { "'numRequested' must not be greater than 'maxPort' - 'minPort'"}
            val availablePorts: SortedSet<Int> = TreeSet()
            var attemptCount = 0
            while (++attemptCount <= numRequested + 100 && availablePorts.size < numRequested) {
                availablePorts.add(findAvailablePort(minPort, maxPort))
            }
            check(availablePorts.size == numRequested) {
                String.format(
                        "Could not find %d available %s ports in the range [%d, %d]",
                        numRequested, name, minPort, maxPort)
            }
            return availablePorts
        }
    }
}
