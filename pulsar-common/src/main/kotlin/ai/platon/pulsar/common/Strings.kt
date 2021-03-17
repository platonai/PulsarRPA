package ai.platon.pulsar.common

import kotlin.reflect.KClass

/**
 * A human readable, simplified full name of a java class, the name can be used as a file name
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

fun parseSimpleOption(args: String?, optionName: String): String? {
    val s = args ?: return null
    return "$optionName\\s+([\\-_a-zA-Z0-9]+)\\s?".toRegex().find(s)?.groupValues?.get(1)
}
