package ai.platon.pulsar.common

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.reflect.KClass

/**
 * Generates a human-readable, simplified full name of a Java class.
 * The name can be used as a file name.
 *
 * @param obj The object whose class name is to be simplified.
 * @param fullNameCount The number of parts of the class name to keep as full names (default is 1).
 * @param partCount The total number of parts of the class name to include (default is 3).
 * @return A simplified, human-readable class name.
 */
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

/**
 * Prepends a human-readable class name to a given name, separated by a specified separator.
 *
 * @param obj The object whose class name is to be prepended.
 * @param name The name to which the class name is prepended.
 * @param separator The separator used between the class name and the name (default is ".").
 * @return The combined string with the class name and the name.
 */
fun prependReadableClassName(obj: Any, name: String, separator: String = "."): String {
    return "${readableClassName(obj)}$separator$name".replace("\\.+".toRegex(), separator)
}

/**
 * Prepends a human-readable class name and an identifier to a given name, separated by a specified separator.
 *
 * @param obj The object whose class name is to be prepended.
 * @param ident The identifier to be included between the class name and the name.
 * @param name The name to which the class name and identifier are prepended.
 * @param separator The separator used between the class name, identifier, and the name.
 * @return The combined string with the class name, identifier, and the name.
 */
fun prependReadableClassName(obj: Any, ident: String, name: String, separator: String): String {
    if (ident.isBlank()) {
        return prependReadableClassName(obj, name, separator)
    }

    val prefix = readableClassName(obj)
    return "$prefix$separator$ident$separator$name".replace("\\.+".toRegex(), separator)
}

/**
 * Converts an exception to a string representation, including a stack trace.
 *
 * @param e The throwable exception to be stringified.
 * @param prefix The message prefix to be added before the exception details (default is "").
 * @param postfix The message postfix to be added after the exception details (default is "").
 * @return The string representation of the exception.
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
 * Simplifies an exception message by extracting the most relevant lines.
 *
 * @param e The throwable exception to be simplified.
 * @param prefix The message prefix to be added before the simplified exception details (default is "").
 * @param postfix The message postfix to be added after the simplified exception details (default is "").
 * @return The simplified string representation of the exception.
 */
fun simplifyException(e: Throwable, prefix: String = "", postfix: String = ""): String {
    var message = e.message ?: stringifyException(e)
    val lines = message.split("\n").filter { it.isNotBlank() }

    message = when (lines.size) {
        0 -> ""
        1 -> lines[0]
        2 -> lines[0] + "\t" + lines[1]
        else -> lines[0] + "\t" + lines[1] + " ..."
    }

    return "$prefix$message$postfix"
}

