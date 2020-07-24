package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.h2.engine.Session
import ai.platon.pulsar.ql.annotation.H2Context
import ai.platon.pulsar.ql.h2.addColumn
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

@UDFGroup
object CommonFunctionTables {

    @UDFunction(deterministic = true, description = "Show all load options, " +
            "almost every user defined functions who have an url parameter " +
            "can be configured by adding load options to the url parameter")
    @JvmStatic
    fun loadOptions(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        rs.addColumn("OPTION")
        rs.addColumn("TYPE")
        rs.addColumn("DEFAULT")
        rs.addColumn("DESCRIPTION")

        LoadOptions.helpList.forEach { rs.addRow(*it.toTypedArray()) }

        return rs
    }

    @UDFunction(deterministic = true, description = "Show all X-SQL functions")
    @JvmStatic
    fun xsqlHelp(@H2Context conn: Connection): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        rs.addColumn("NAMESPACE")
        rs.addColumn("XSQL FUNCTION")
        rs.addColumn("NATIVE FUNCTION")
        rs.addColumn("DESCRIPTION")

        val session = H2SessionFactory.getSession(conn)
        session.registeredUdfClasses
                .flatMap { getMetadataOfUdfs(it.kotlin) }
                .sortedBy { it[0] }
                .forEach { rs.addRow(*it) }

        return rs
    }

    @UDFunction(deterministic = true,
            description = "Create a ResultSet from a list of values, the values should have format kvkv ... kv")
    @JvmStatic
    fun map(vararg kvs: Value): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        rs.addColumn("KEY")
        rs.addColumn("VALUE")

        if (kvs.isEmpty()) {
            return rs
        }

        var i = 0
        while (i < kvs.size / 2) {
            rs.addRow(kvs[i], kvs[i + 1])
            i += 2
        }

        return rs
    }

    @UDFunction(deterministic = true, description = "Create an empty ResultSet")
    @JvmStatic
    fun explode(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        return rs
    }

    @UDFunction(deterministic = true, description = "Create a ResultSet from an array")
    @JvmStatic
    @JvmOverloads
    fun explode(@H2Context conn: Connection, values: ValueArray, col: String = "COL"): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        if (values.list.isEmpty()) {
            return rs
        }

        val template = values.list[0]
        val typeInfo = template.type
        val dt = DataType.getDataType(typeInfo.valueType)
        rs.addColumn(col, dt.sqlType, typeInfo.precision.toInt(), typeInfo.scale)

        for (i in 0 until values.list.size) {
            val value = values.list[i]
            rs.addRow(value)
        }

        return rs
    }

    @UDFunction(deterministic = true, description = "Create an empty ResultSet")
    @JvmStatic
    fun posexplode(): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        return rs
    }

    @UDFunction(deterministic = true, description = "Create a ResultSet from an array with the position in the array")
    @JvmStatic
    @JvmOverloads
    fun posexplode(values: ValueArray, col: String = "COL"): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        if (values.list.isEmpty()) {
            return rs
        }

        rs.addColumn("POS", Types.INTEGER, 10, 0)
        val template = values.list[0]
        val typeInfo = template.type
        val dt = DataType.getDataType(typeInfo.valueType)
        rs.addColumn(col, dt.sqlType, typeInfo.precision.toInt(), typeInfo.scale)

        for (i in 0 until values.list.size) {
            rs.addRow(i + 1, values.list[i])
        }
        return rs
    }

    /**
     * Get the metadata of UDFs in an UDF class
     * */
    private fun getMetadataOfUdfs(udfClass: KClass<out Any>): List<Array<String>> {
        val className = udfClass.simpleName
        val group = udfClass.annotations.first { it is UDFGroup } as UDFGroup
        val namespace = group.namespace

        return udfClass.members.mapNotNull { (it.annotations.firstOrNull { it is UDFunction } as? UDFunction)?.to(it) }
                .map { (annotation, method) ->
                    val hasShortcut = annotation.hasShortcut
                    val parameters = getParameters(method).joinToString { it }
                    arrayOf(
                            if (hasShortcut) "$namespace(Ignorable)" else namespace,
                            getAlias(udfClass, method.name, namespace) + "(" + parameters.toUpperCase() + ")",
                            className + "." + method.name + "(" + parameters + ")",
                            getDescription(method)
                    )
                }
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

    private fun getParameters(udf: KCallable<*>): List<String> {
        return udf.parameters
                .filter { it.kind == KParameter.Kind.VALUE }
                .filter { !it.annotations.any { it is H2Context } }
                .map { "${it.name}: ${it.type.javaType.typeName.substringAfterLast(".")}" }
    }

    private fun getDescription(udf: KCallable<*>): String {
        return udf.annotations.mapNotNull { it as? UDFunction }.firstOrNull()?.description?:""
    }
}
