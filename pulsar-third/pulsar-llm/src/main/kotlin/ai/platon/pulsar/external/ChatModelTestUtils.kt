package ai.platon.pulsar.external

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object ChatModelTestUtils {
    private val enabledConfigs = mutableSetOf<Path>()

    fun initConfig() {
        AppPaths.CONFIG_ENABLED_DIR.listDirectoryEntries("*.xml").toCollection(enabledConfigs)
        if (enabledConfigs.isNotEmpty()) {
            return
        }

        // The two engine are tested recently, so we enable them first
        listOf("a2-pulsar-volcengine-deepseek-v3.xml", "b1-pulsar-deepseek.xml")
            .sorted()
            .firstOrNull { AppPaths.CONFIG_ENABLED_DIR.resolve(it).exists() }
            ?.let { AppFiles.enableConfig(it) }
    }

    fun resetConfig() {
        AppPaths.CONFIG_ENABLED_DIR.listDirectoryEntries("*.xml").forEach { it.deleteIfExists() }
        enabledConfigs.forEach { AppFiles.enableConfig(it.fileName.toString()) }
    }
}
