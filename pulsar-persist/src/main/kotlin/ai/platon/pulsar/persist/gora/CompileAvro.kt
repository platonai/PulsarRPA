package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.ResourceLoader
import org.apache.gora.compiler.GoraCompiler
import org.apache.gora.compiler.utils.LicenseHeaders
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val resource = ResourceLoader.getResource("avro/webpage.avsc")!!
    val inputPath = Paths.get(resource.toURI().path)
    val workingDir = Paths.get("").toAbsolutePath()
    val outputPath = Files.walk(workingDir)
        .filter { it.toString().endsWith("pulsar-persist/src/main/java") }
        .findFirst().get()
    println(outputPath)

    // Setting the default license header to ASLv2
    val licenseHeader = LicenseHeaders("ASLv2")
    GoraCompiler.compileSchema(arrayOf(inputPath.toFile()), outputPath.toFile(), licenseHeader)
}
