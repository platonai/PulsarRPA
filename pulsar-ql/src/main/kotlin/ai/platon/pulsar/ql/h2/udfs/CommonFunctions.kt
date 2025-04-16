package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.common.ResultSets
import ai.platon.pulsar.ql.common.annotation.H2Context
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.h2.addColumn
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil
import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.h2.tools.SimpleResultSet
import org.h2.value.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Long.MAX_VALUE
import java.sql.Connection
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

@UDFGroup
object CommonFunctions {

    private val sqlContext get() = SQLContexts.create()

    @UDFunction(description = "Test if the given string is a number")
    @JvmStatic
    fun isNumeric(str: String): Boolean {
        return StringUtils.isNumeric(str)
    }

    @UDFunction(description = "Get the domain of a url")
    @JvmStatic
    fun getDomain(url: String): String {
        return UrlUtils.getTopPrivateDomain(url)
    }

    @UDFunction(description = "Extract the first group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String): String {
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction(description = "Extract the nth group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String, group: Int): String {
        return RegexExtractor().re1(text, regex, group)
    }

    @UDFunction(description = "Extract two groups of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String): ValueArray {
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction(description = "Extract two groups(key and value) of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun makeArray(vararg values: Value): ValueArray {
        return ValueArray.get(values)
    }

    @UDFunction
    @JvmStatic
    fun makeArrayN(value: Value, n: Int): ValueArray {
        val values = Array(n) { value }
        return ValueArray.get(values)
    }

    /**
     * The first column is treated as the key while the second one is treated as the value
     * */
    @UDFunction
    @JvmStatic
    fun toJson(rs: ResultSet): String {
        if (rs.metaData.columnCount < 2) {
            return "{}"
        }

        val map = mutableMapOf<String, String>()
        rs.beforeFirst()
        while (rs.next()) {
            // TODO: this is a temporary solution, find out why there is a ' surrounding
            val k = rs.getString(1).removeSurrounding("'")
            val v = rs.getString(2).removeSurrounding("'")
            map[k] = v
        }

        return Gson().toJson(map)
    }

    /**
     * For all ValueInts in the values, find out the minimal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().minByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueInts in the values, find out the maximal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().maxByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the minimal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().minByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the maximal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().maxByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    @UDFunction
    @JvmStatic
    fun getString(value: Value): String {
        return value.string
    }

    @UDFunction
    @JvmStatic
    fun isEmpty(array: ValueArray): Boolean {
        return array.list.isEmpty()
    }

    @UDFunction
    @JvmStatic
    fun isNotEmpty(array: ValueArray): Boolean {
        return array.list.isNotEmpty()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun formatTimestamp(timestamp: String, fmt: String = "yyyy-MM-dd HH:mm:ss"): String {
        val time = timestamp.toLongOrNull() ?: 0
        return formatTimestamp(time, fmt)
    }

    private fun formatTimestamp(timestamp: Long, fmt: String): String {
        return SimpleDateFormat(fmt).format(Date(timestamp))
    }
}
