package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.slf4j.LoggerFactory
import java.io.IOException

@UDFGroup(namespace = "STR")
object StringFunctions {

    private val log = LoggerFactory.getLogger(StringFunctions::class.java)

    @UDFunction(description = "Get the first integer in the given string")
    @JvmStatic
    fun getFirstInteger(str: String?, defaultValue: Int): Int {
        return StringUtil.getFirstInteger(str, defaultValue)
    }

    @UDFunction(description = "Get the first float number in the given string")
    @JvmStatic
    fun getFirstFloatNumber(str: String?, defaultValue: Float): Float {
        return StringUtil.getFirstFloatNumber(str, defaultValue)
    }

    @UDFunction(description = "Chinese tokenizer")
    @JvmStatic
    @JvmOverloads
    fun chineseTokenize(str: String?, sep: String = " "): String {
        if (str.isNullOrBlank()) {
            return ""
        }

        val analyzer = SmartChineseAnalyzer()

        val sb = StringBuilder()
        try {
            val tokenStream = analyzer.tokenStream("field", str)
            val term = tokenStream.addAttribute(CharTermAttribute::class.java)
            tokenStream.reset()
            while (tokenStream.incrementToken()) {
                sb.append(term.toString())
                sb.append(sep)
            }
            tokenStream.end()
            tokenStream.close()
        } catch (e: IOException) {
            log.warn(e.message)
        }

        return sb.toString()
    }
}
