package ai.platon.pulsar.common

import kotlin.reflect.KClass

fun readableClassName(obj: Any, fullNameCount: Int = 1, partCount: Int = 3): String {
    val names = when (obj) {
        is Class<*> -> obj.name.split(".")
        is KClass<*> -> obj.java.name.split(".")
        else -> obj::class.java.name.split(".")
    }.takeLast(partCount)

    val size = names.size
    return names.mapIndexed { i, n -> n.takeIf { i >= size - fullNameCount }?:n.substring(0, 1) }
            .joinToString(".") { it.removeSuffix("\$Companion") }
}

fun prependReadableClassName(obj: Any, name: String, separator: String = "."): String {
    return "${readableClassName(obj)}$separator$name"
}

fun prependReadableClassName(obj: Any, ident: String, name: String, separator: String): String {
    if (ident.isBlank()) {
        return prependReadableClassName(obj, name, separator)
    }

    val prefix = readableClassName(obj)
    val ident0 = ident.removeSurrounding(separator)
    return "$prefix$separator$ident0$separator$name"
}
