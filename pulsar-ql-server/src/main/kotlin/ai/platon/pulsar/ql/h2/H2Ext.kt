package ai.platon.pulsar.ql.h2

import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value

fun SimpleResultSet.addColumn(name: String) {
    addColumn(name, DataType.convertTypeToSQLType(Value.STRING), 0, 0)
}
