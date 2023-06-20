package ai.platon.pulsar.common.options

object OptionUtils {

    // typical options: best-seller, com.br
    val OPTION_REGEX = "\\s+([\\\\.\\-_a-zA-Z0-9]+)\\s?"

    fun findOption(args: String?, optionName: String): String? {
        val s = args ?: return null
        return "$optionName$OPTION_REGEX".toRegex().find(s)?.groupValues?.get(1)
    }

    fun findOption(args: String?, optionNames: Iterable<String>): String? {
        args ?: return null

        optionNames.forEach {
            val option = findOption(args, it)
            if (option != null) {
                return option
            }
        }

        return null
    }

    fun arity0ToArity1(args: String, search: String): String {
        var args0 = args
        var pos: Int = args0.indexOf(search)
        if (pos != -1) {
            pos += search.length
            if (pos == args0.length) {
                args0 = args0.replace(search, "$search true")
            } else {
                val s: String = args0.substring(pos).trim { it <= ' ' }
                if (s[0] == '-') {
                    args0 = args0.replace(search, "$search true")
                }
            }
        }
        return args0
    }
}
