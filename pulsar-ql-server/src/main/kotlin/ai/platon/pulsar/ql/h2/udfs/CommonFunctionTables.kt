package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray

import java.sql.ResultSet
import java.sql.Types

@UDFGroup
object CommonFunctionTables {

    @UDFunction
    @JvmStatic
    fun map(vararg kvs: Value): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        rs.addColumn("KEY")
        rs.addColumn("VALUE")

        if (kvs.size == 0) {
            return rs
        }

        var i = 0
        while (i < kvs.size / 2) {
            rs.addRow(kvs[i], kvs[i + 1])
            i += 2
        }

        return rs
    }

    @UDFunction
    @JvmStatic
    fun explode(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        return rs
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun explode(@H2Context h2session: Session, values: ValueArray, col: String = "COL"): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        if (values.list.size == 0) {
            return rs
        }

        val template = values.list[0]

        val dt = DataType.getDataType(template.type)
        rs.addColumn(col, dt.sqlType, template.precision.toInt(), template.scale)

        for (i in 0 until values.list.size) {
            val value = values.list[i]
            rs.addRow(value)
        }

        return rs
    }

    @UDFunction
    @JvmStatic
    fun posexplode(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        return rs
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun posexplode(values: ValueArray, col: String = "COL"): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        if (values.list.size == 0) {
            return rs
        }

        rs.addColumn("POS", Types.INTEGER, 10, 0)
        val template = values.list[0]
        val dt = DataType.getDataType(template.type)
        rs.addColumn(col, dt.sqlType, template.precision.toInt(), template.scale)

        for (i in 0 until values.list.size) {
            rs.addRow(i + 1, values.list[i])
        }
        return rs
    }
}
