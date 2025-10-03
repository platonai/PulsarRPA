package ai.platon.pulsar.common

/**
 * In-process Snowflake-like ID generator.
 *
 * Bit layout (from high -> low, using a signed 64-bit long, the sign bit is unused for data):
 * [ sign(1) | timestamp (variable) | node (nodeBits) | sequence (sequenceBits) ]
 *
 * Timestamp bits = 63 - (nodeBits + sequenceBits).
 * Maximum representable timestamp offset = (1L << timestampBits) - 1 milliseconds since the custom epoch.
 *
 * Capacity examples (with defaults: nodeBits=10, sequenceBits=12):
 * - timestampBits = 41 => ~69 years of millisecond timestamps from the custom epoch.
 * - Up to 2^10 (=1024) distinct node ids per process space.
 * - Up to 2^12 (=4096) IDs per millisecond per node before rolling to the next millisecond.
 *
 * Characteristics:
 * - Thread-safe via method synchronization (simple & reliable for modest throughput; could be replaced with CAS if needed).
 * - Monotonic within the same process (blocks briefly or throws if the system clock moves backwards significantly).
 * - Sequence resets to 0 for a new millisecond.
 * - When the sequence overflows within the same millisecond it spins until the next millisecond.
 *
 * Clock rollback handling:
 * - Small rollback (<= 5 ms) => bounded spin-wait until the clock catches up (prevents duplicate IDs without failing fast).
 * - Large rollback (> 5 ms) => throws IllegalStateException to surface a potential system clock anomaly.
 *
 * Review notes / potential enhancements:
 * - Consider injecting a time source (e.g. () -> Long) for improved testability (enables deterministic rollback & overflow tests).
 * - Consider adding a non-blocking overflow strategy (e.g. random backoff) if ultra-high contention is expected.
 * - Consider exposing decoded components (timestamp, nodeId, sequence) via a helper for observability/debugging.
 * - Synchronization may become a bottleneck if many threads request IDs concurrently; if that becomes an issue, a striped or atomics-based design could be introduced.
 *
 * Usage example:
 * val gen = InProcessIdGenerator(nodeId = 1)
 * val id = gen.nextId()
 *
 * Reference:
 * - [Unique ID Generators](https://medium.com/prepster/unique-id-generators-4e3f898d0999)
 */
class InProcessIdGenerator(
    private val nodeId: Long = 0,
    private val nodeBits: Int = 10,
    private val sequenceBits: Int = 12,
    epochMillis: Long = 1672531200000L // Default epoch: 2023-01-01T00:00:00Z (customizable)
) {
    init {
        require(nodeBits in 0..20) { "nodeBits should be between 0 and 20" }
        require(sequenceBits in 0..20) { "sequenceBits should be between 0 and 20" }
        require(nodeBits + sequenceBits <= 63) { "nodeBits + sequenceBits must be <= 63" }
    }

    private val epoch = epochMillis

    private val maxNodeId = (1L shl nodeBits) - 1
    private val maxSequence = (1L shl sequenceBits) - 1

    private val sequenceMask = maxSequence

    // Bit shifts: timestamp occupies the remaining high bits.
    private val timestampShift = nodeBits + sequenceBits
    private val nodeShift = sequenceBits

    @Volatile
    private var lastTimestamp = -1L

    private var sequence = 0L

    init {
        require(nodeId in 0..maxNodeId) { "nodeId must be between 0 and $maxNodeId" }
    }

    /**
     * Generate the next ID (thread-safe).
     */
    @Synchronized
    fun nextId(): Long {
        var timestamp = timeGen()
        if (timestamp < lastTimestamp) {
            // Handle clock rollback.
            val diff = lastTimestamp - timestamp
            // For small rollback, spin-wait; otherwise fail fast to avoid duplicates.
            if (diff <= 5) {
                timestamp = waitUntil(lastTimestamp)
            } else {
                throw IllegalStateException("Clock moved backwards. Refusing to generate id for $diff ms")
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and sequenceMask
            if (sequence == 0L) {
                // Sequence overflow -> wait for next millisecond.
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            // New millisecond -> reset sequence. (Could randomize to reduce contention hot spots if desired.)
            sequence = 0L
        }

        lastTimestamp = timestamp

        val diff = timestamp - epoch
        // Compose the ID by shifting & OR-ing the components.
        return (diff shl timestampShift) or (nodeId shl nodeShift) or sequence
    }

    /**
     * Return the next ID in string form using the given radix (base 2..62).
     */
    fun nextIdString(radix: Int = 36): String {
        require(radix in 2..62) { "radix must be between 2 and 62" }
        val id = nextId()
        return toUnsignedString(id, radix)
    }

    private fun tilNextMillis(lastTs: Long): Long {
        var ts = timeGen()
        while (ts <= lastTs) {
            ts = timeGen()
        }
        return ts
    }

    private fun waitUntil(target: Long, maxSpinMillis: Long = 5): Long {
        var ts = timeGen()
        val start = System.currentTimeMillis()
        while (ts < target && (System.currentTimeMillis() - start) < maxSpinMillis) {
            ts = timeGen()
        }
        return ts
    }

    private fun timeGen(): Long = System.currentTimeMillis()

    // 0-9 a-z A-Z (base62 alphabet)
    private val digits = charArrayOf(
        '0','1','2','3','4','5','6','7','8','9',
        'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
    )

    // Convert to an unsigned string representation supporting bases 2..62.
    private fun toUnsignedString(value: Long, radix: Int): String {
        require(radix in 2..62) { "radix must be between 2 and 62" }
        if (value == 0L) return "0"
        if (radix <= 36 && value >= 0) {
            // Fast path: delegate to JDK for common radices
            return value.toString(radix)
        }
        // Manual conversion (treat value as unsigned within positive domain; IDs are non-negative by construction)
        var v = value
        val buf = StringBuilder()
        while (v > 0) {
            val rem = (v % radix).toInt()
            buf.append(digits[rem])
            v /= radix
        }
        return buf.reverse().toString()
    }
}