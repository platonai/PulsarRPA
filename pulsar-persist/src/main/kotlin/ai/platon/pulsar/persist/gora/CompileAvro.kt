package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.measure.ByteUnit
import org.apache.gora.compiler.GoraCompiler
import org.apache.gora.compiler.utils.LicenseHeaders
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isDirectory

fun main() {
    val resource = ResourceLoader.getResource("avro/webpage.avsc")!!
    
    // Remove prefix to correct malformed path on Windows:
    // Illegal char <:> at index 2: /D:/workspace/PulsarRPA/pulsar-persist/target/classes/avro/webpage.avsc
    val path = resource.toURI().path.removePrefix("/")
    
    val inputPath = Paths.get(path)
    val workingDir = Paths.get("").toAbsolutePath()
    
    val outputPath = Files.walk(workingDir)
        .filter { it.isDirectory() }
        .filter { it.toString().contains("pulsar-persist[/\\\\]src[/\\\\]main[/\\\\]java".toRegex()) }
        .findFirst().get()
    println("Output dir: $outputPath")

    // Setting the default license header to ASLv2
    val licenseHeader = LicenseHeaders("ASLv2")
    GoraCompiler.compileSchema(arrayOf(inputPath.toFile()), outputPath.toFile(), licenseHeader)
}
