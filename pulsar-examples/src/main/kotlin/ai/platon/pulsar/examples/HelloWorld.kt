package ai.platon.pulsar.examples

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.RequiredDirectory
import ai.platon.pulsar.common.isBlankBody
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

fun main() {
    AppPaths::class.java.declaredFields.filter { it.annotations.any { it is RequiredDirectory } }.mapNotNull { it.get(AppPaths) as? Path }
            .forEach { println(it) }
}
