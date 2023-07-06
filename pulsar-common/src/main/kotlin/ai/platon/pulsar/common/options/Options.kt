package ai.platon.pulsar.common.options

import java.util.*
import kotlin.collections.ArrayList


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

    /**
     * [code borrowed from ant.jar]
     * Crack a command line.
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * An empty or null toProcess parameter results in a zero sized array.
     */
    fun translateCommandline(toProcess: String): List<String> {
        if (toProcess.isBlank()) {
            //no command? no string
            return listOf()
        }

        // parse with a simple finite state machine
        val normal = 0
        val inQuote = 1
        val inDoubleQuote = 2
        var state = normal
        val tok = StringTokenizer(toProcess, "\"\' ", true)
        val result = ArrayList<String>()
        val current = StringBuilder()
        var lastTokenHasBeenQuoted = false

        while (tok.hasMoreTokens()) {
            val nextTok: String = tok.nextToken()
            when (state) {
                inQuote -> if ("\'" == nextTok) {
                    lastTokenHasBeenQuoted = true
                    state = normal
                } else {
                    current.append(nextTok)
                }

                inDoubleQuote -> if ("\"" == nextTok) {
                    lastTokenHasBeenQuoted = true
                    state = normal
                } else {
                    current.append(nextTok)
                }

                else -> {
                    if ("\'" == nextTok) {
                        state = inQuote
                    } else if ("\"" == nextTok) {
                        state = inDoubleQuote
                    } else if (" " == nextTok) {
                        if (lastTokenHasBeenQuoted || current.length != 0) {
                            result.add(current.toString())
                            current.setLength(0)
                        }
                    } else {
                        current.append(nextTok)
                    }
                    lastTokenHasBeenQuoted = false
                }
            }
        }

        if (lastTokenHasBeenQuoted || current.isNotEmpty()) {
            result.add(current.toString())
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw RuntimeException("unbalanced quotes in $toProcess")
        }

        return result
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
