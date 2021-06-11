package ai.platon.pulsar.ql.h2.udas

import ai.platon.pulsar.ql.annotation.UDAggregation
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.h2.api.Aggregate
import org.h2.api.AggregateFunction
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.*

@UDFGroup
@UDAggregation(name = "GROUP_FETCH")
class GroupFetch : Aggregate {

    private lateinit var conn: Connection
    private val urls = ArrayList<String>()

    override fun init(conn: Connection) {
        this.conn = conn
    }

    override fun getInternalType(ints: IntArray): Int {
        if (ints.size != 3) {
            // throw DbException.get(org.h2.api.ErrorCode.INVALID_USE_OF_AGGREGATE_FUNCTION_1);
            LOG.debug("types: ", ints.size)
        }
        return Value.ARRAY
    }

    override fun add(o: Any?) {
        if (o == null) return

        val url = if (o is Value) {
            o.string
        } else {
            o.toString()
        }

        if (url != null && url.length >= SHORTEST_URL_LENGTH) {
            urls.add(url)
        }
    }

    override fun getResult(): Any {
        val session = H2SessionFactory.getSession(conn)
        val h2session = session.sessionDelegate.implementation as org.h2.engine.Session
        session.loadAll(urls)
        val values = urls.map { url -> DataType.convertToValue(h2session, url, Value.STRING) }.toTypedArray()
        return ValueArray.get(values)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AggregateFunction::class.java)
        private val SHORTEST_URL_LENGTH = "ftp://t.tt/".length
    }
}
