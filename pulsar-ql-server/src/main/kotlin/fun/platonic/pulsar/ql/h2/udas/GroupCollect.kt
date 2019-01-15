package `fun`.platonic.pulsar.ql.h2.udas

import `fun`.platonic.pulsar.ql.annotation.UDAggregation
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.h2.api.Aggregate
import org.h2.engine.Session
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueNull
import org.jsoup.nodes.Element
import java.util.*

@UDFGroup
@UDAggregation(name = "GROUP_COLLECT")
class GroupCollect : Aggregate {

    private var h2session: Session? = null
    private val values = ArrayList<Value>()

    override fun init(h2session: Session?) {
        this.h2session = h2session
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
            values.add(DataType.convertToValue(h2session, o, Value.UNKNOWN))
        }
    }

    override fun getResult(): Any {
        return ValueArray.get(values.toTypedArray())
    }
}
