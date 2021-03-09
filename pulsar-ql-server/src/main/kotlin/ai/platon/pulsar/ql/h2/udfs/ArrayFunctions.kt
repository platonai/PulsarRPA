package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import org.h2.value.ValueArray

@UDFGroup(namespace = "ARRAY")
object ArrayFunctions {

    @UDFunction
    @JvmStatic
    fun joinToString(values: ValueArray, separator: String): String {
        return values.list.joinToString(separator) { it.string }
    }
}
