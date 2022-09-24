package ai.platon.pulsar.crawl.fetch.batch.data

import java.net.URL

/**
 * Created by vincent on 17-3-8.
 */
class PoolId(val priority: Int, val protocol: String, val host: String) : Comparable<PoolId> {

    constructor(priority: Int, url: URL): this(priority, url.protocol, url.host)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is PoolId
                && priority == other.priority
                && protocol == other.protocol
                && host == other.host
    }

    override fun compareTo(other: PoolId): Int {
        var c = priority - other.priority
        if (c == 0) {
            c = protocol.compareTo(other.protocol)
            if (c == 0) {
                c = host.compareTo(other.host)
            }
        }

        return c
    }

    override fun hashCode(): Int {
        return priority * 31 xor 2 + protocol.hashCode() * 31 + host.hashCode()
    }

    override fun toString(): String {
        return "<$priority, $protocol://$host>"
    }
}
