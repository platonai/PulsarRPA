package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.Parameterized
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern
import kotlin.system.exitProcess

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class PulsarOptions : Parameterized {
    protected val log = LoggerFactory.getLogger(PulsarOptions::class.java)

    var expandAtSign = true
    var acceptUnknownOptions = true
    var allowParameterOverwriting = true
    // arguments
    val args: String
    // argument vector
    val argv: Array<String>
    protected val objects: MutableSet<Any> = HashSet()
    protected lateinit var jc: JCommander

    open var isHelp: Boolean = false

    constructor(): this("")

    constructor(args: String): this(split(args))

    constructor(argv: Array<String>) {
        this.argv = argv
        for (i in this.argv.indices) {
            // Since space can not appear in dynamic parameters in command line, we use % instead
            this.argv[i] = this.argv[i].replace("%".toRegex(), " ")
        }
        args = argv.joinToString(DEFAULT_DELIMETER)
    }

    constructor(argv: Map<String, String>)
            : this(argv.entries.joinToString(DEFAULT_DELIMETER) { it.key + DEFAULT_DELIMETER + it.value })

    fun setObjects(vararg objects: Any) {
        this.objects.clear()
        this.objects.addAll(objects.toList())
    }

    fun addObjects(vararg objects: Any) {
        this.objects.addAll(objects.toList())
    }

    open fun parse(): Boolean {
        try {
            doParse()
        } catch (e: Throwable) {
            log.warn(StringUtil.stringifyException(e))
            return false
        }

        return true
    }

    open fun parseOrExit() {
        parseOrExit(mutableSetOf())
    }

    protected open fun parseOrExit(objects: Set<Any>) {
        try {
            addObjects(objects)
            doParse()

            if (isHelp) {
                jc.usage()
                exitProcess(0)
            }
        } catch (e: ParameterException) {
            println(e.toString())
            exitProcess(0)
        }
    }

    private fun doParse() {
        objects.add(this)

        jc = JCommander.newBuilder()
                .acceptUnknownOptions(acceptUnknownOptions)
                .allowParameterOverwriting(allowParameterOverwriting)
                .expandAtSign(expandAtSign).build()
        objects.forEach { jc.addObject(it) }

        if (argv.isNotEmpty()) {
            jc.parse(*argv)
        }
    }

    open fun usage() {
        jc.usage()
    }

    open fun toCmdLine(): String {
        return params.withKVDelimiter(" ").formatAsLine()
                .replace("\\s+".toRegex(), " ")
    }

    open fun toArgsMap(): Map<String, String> {
        return params.asStringMap()
    }

    open fun toMutableArgsMap(): MutableMap<String, String> {
        return params.asStringMap()
    }

    open fun toArgv(): Array<String> {
        return params.withKVDelimiter(" ").formatAsLine()
                .split("\\s+".toRegex())
                .toTypedArray()
    }

    override fun hashCode(): Int {
        return args.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PulsarOptions && args == other.args
    }

    override fun toString(): String {
        return args
    }

    companion object {
        const val DEFAULT_DELIMETER = " "
        val CMD_SPLIT_PATTERN = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+")

        @JvmOverloads
        fun normalize(args: String, seps: String = ","): String {
            return StringUtils.replaceChars(args, seps, StringUtils.repeat(' ', seps.length))
        }

        /**
         * Split a command line into argument vector (argv)
         * @see {https://stackoverflow.com/questions/36292591/splitting-a-nested-string-keeping-quotation-marks/36292778}
         *
         */
        fun split(args: String): Array<String> {
            val matcher = CMD_SPLIT_PATTERN.matcher(normalize(args))
            matcher.reset()
            val result = ArrayList<String>()
            while (matcher.find()) {
                result.add(matcher.group(0))
            }
            return result.toTypedArray()
        }
    }
}
