package ai.platon.pulsar.ql.h2.udas

import ai.platon.pulsar.ql.common.annotation.UDAggregation
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.types.ValueDom
import org.h2.api.Aggregate
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueNull
import org.jsoup.nodes.Element
import java.sql.Connection
import java.util.*

@UDFGroup
@UDAggregation(name = "GROUP_COLLECT")
class GroupCollect : Aggregate {

    private var conn: Connection? = null
    private val values = ArrayList<Value>()

    override fun init(conn: Connection) {
        this.conn = conn
    }

    override fun getInternalType(types: IntArray): Int {
        return Value.ARRAY
    }

    override fun add(o: Any?) {
        if (o == null) {
            values.add(ValueNull.INSTANCE)
        } else if (o is ValueArray) {
            values.addAll(o.list.toList())
        } else if (o is Value) {
            values.add(o)
        } else if (o is Element) {
            // TODO: ValueDom is converted to Element by h2, how to avoid the conversion?
            values.add(ValueDom.get(o))
        } else {
            // values.add(DataType.convertToValue(conn, o, Value.UNKNOWN))
        }
    }

    override fun getResult(): Any {
        return ValueArray.get(values.toTypedArray())
    }
}
