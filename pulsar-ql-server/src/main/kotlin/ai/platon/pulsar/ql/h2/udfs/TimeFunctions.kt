package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction

@UDFGroup(namespace = "TIME")
object TimeFunctions {
    /**
     * TODO: detect local date time from string
     * */
    @UDFunction
    @JvmOverloads
    @JvmStatic fun firstMysqlDateTime(str: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (str.isNullOrBlank()) {
            return "1970-01-01 00:00:00"
        }
        // DateTimeFormatter.ofPattern(pattern).parse(str)
        return str
    }
}
