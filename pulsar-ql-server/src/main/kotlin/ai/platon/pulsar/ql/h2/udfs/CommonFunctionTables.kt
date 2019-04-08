package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import com.google.common.reflect.ClassPath
import org.apache.commons.lang3.StringUtils
import org.h2.engine.Session
import org.h2.engine.SessionInterface
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import java.awt.SystemColor.text

import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

@UDFGroup
object CommonFunctionTables {

    @UDFunction(deterministic = true, description = "Show all X-SQL functions")
    @JvmStatic
    fun xsqlHelp(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        rs.addColumn("XSQL FUNCTION")
        rs.addColumn("NATIVE FUNCTION")
        rs.addColumn("DESCRIPTION")

        ClassPath.from(CommonFunctions.javaClass.classLoader)
                .getTopLevelClasses(CommonFunctions.javaClass.`package`.name)
                .map { it.load() }
                .filter { it.annotations.any { it is UDFGroup } }
                .flatMap { findUdfs(it.kotlin) }
                .forEach { rs.addRow(*it) }

        return rs
    }

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

    /**
     * Register a kotlin UDF class
     * */
    private fun findUdfs(udfClass: KClass<out Any>): List<Array<String>> {
        val className = udfClass.simpleName
        val group = udfClass.annotations.first { it is UDFGroup } as UDFGroup
        val namespace = group.namespace

        return udfClass.members
                .filter { it.annotations.any { it is UDFunction } }
                .map { arrayOf(getAlias(udfClass, it.name, namespace), className + "." + it.name, getDescription(it)) }
    }

    /**
     * Register user defined functions
     * We support arbitrary "_" in a UDF name, for example, the following functions are the same:
     * some_fun_(), _____some_fun_(), some______fun()
     */
    private fun getAlias(udfClass: KClass<out Any>, method: String, namespace: String = ""): String {
        var alias = method.split("(?=\\p{Upper})".toRegex()).joinToString("_") { it }
        alias = if (namespace.isEmpty()) alias else namespace + "_" + alias
        return alias.toUpperCase()
    }

    private fun getDescription(udf: KCallable<*>): String {
        return udf.annotations.mapNotNull { it as? UDFunction }.firstOrNull()?.description?:""
    }
}
