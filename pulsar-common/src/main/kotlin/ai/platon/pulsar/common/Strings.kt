package ai.platon.pulsar.common

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.reflect.KClass

/**
 * A human-readable, simplified full name of a java class, the name can be used as a file name
 * */
fun readableClassName(obj: Any, fullNameCount: Int = 1, partCount: Int = 3): String {
    val names = when (obj) {
        is Class<*> -> obj.name.split(".")
        is KClass<*> -> obj.java.name.split(".")
        else -> obj::class.java.name.split(".")
    }.takeLast(partCount)

    val size = names.size
    return names.mapIndexed { i, n -> n.takeIf { i >= size - fullNameCount } ?: n.substring(0, 1) }
        .joinToString(".") {
            it.replace("Companion", "C").replace("$", "_")
        }
}

fun prependReadableClassName(obj: Any, name: String, separator: String = "."): String {
    return "${readableClassName(obj)}$separator$name".replace("\\.+".toRegex(), separator)
}

fun prependReadableClassName(obj: Any, ident: String, name: String, separator: String): String {
    if (ident.isBlank()) {
        return prependReadableClassName(obj, name, separator)
    }

    val prefix = readableClassName(obj)
    return "$prefix$separator$ident$separator$name".replace("\\.+".toRegex(), separator)
}

/**
 *
 * Stringify an exception.
 *
 * @param e a Throwable.
 * @param prefix The message prefix.
 * @param postfix The message postfix.
 * @return The message of the throwable.
 */
fun stringifyException(e: Throwable, prefix: String = "", postfix: String = ""): String {
    Objects.requireNonNull(e)
    val stm = StringWriter()
    val wrt = PrintWriter(stm)
    wrt.print(prefix)
    e.printStackTrace(wrt)
    wrt.print(postfix)
    wrt.close()
    return stm.toString()
}

/**
 *
 * simplifyException.
 *
 * @param e a Throwable.
 * @param prefix The message prefix.
 * @param postfix The message postfix.
 * @return The message of the throwable.
 */
fun simplifyException(e: Throwable, prefix: String = "", postfix: String = ""): String {
    var message = e.message
    if (message == null) {
        message = e.toString()
    }

    val lines = message.split("\n").toTypedArray()
        .filter { it.isNotBlank() }
    val n = lines.size
    message = when (n) {
        0 -> ""
        1 -> lines[0]
        2 -> lines[0] + "\t" + lines[1]
        else -> lines[0] + "\t" + lines[1] + " ..."
    }

    return prefix + message + postfix
}
