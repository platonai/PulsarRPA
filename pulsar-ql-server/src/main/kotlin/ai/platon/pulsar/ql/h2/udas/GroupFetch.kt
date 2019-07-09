package ai.platon.pulsar.ql.h2.udas

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.ql.annotation.UDAggregation
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.h2.api.Aggregate
import org.h2.api.AggregateFunction
import org.h2.engine.Session
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.slf4j.LoggerFactory
import java.util.*

@UDFGroup
@UDAggregation(name = "GROUP_FETCH")
class GroupFetch : Aggregate {

    private lateinit var h2session: Session
    private val urls = ArrayList<String>()

    override fun init(h2session: Session) {
        this.h2session = h2session
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

        val url: String?
        if (o is Value) {
            url = o.string
        } else {
            url = o.toString()
        }

        if (url != null && url.length >= SHORTEST_URL_LENGTH) {
            urls.add(url)
        }
    }

    override fun getResult(): Any {
        val session = H2SessionFactory.getSession(h2session.serialId)
        val options = LoadOptions()
        session.parallelLoadAll(urls, options)
        val values = urls.map { url -> DataType.convertToValue(h2session, url, Value.STRING) }.toTypedArray()
        return ValueArray.get(values)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AggregateFunction::class.java)
        private val SHORTEST_URL_LENGTH = "ftp://t.tt/".length
    }
}
