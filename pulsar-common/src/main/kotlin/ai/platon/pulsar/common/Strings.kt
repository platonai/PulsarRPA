package ai.platon.pulsar.common

fun readableClassName(obj: Any): String {
    val names = obj::class.java.name.split(".")
    val size = names.size
    return names.mapIndexed { i, n -> n.takeIf { i >= size - 2 }?:n.substring(0, 1) }.joinToString(".")
}

fun prependReadableClassName(obj: Any, name: String, separator: String = "."): String {
    return "${readableClassName(obj)}$separator$name"
}
