package ai.platon.pulsar.skeleton.common.llm

import ai.platon.pulsar.common.code.ProjectUtils
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object LLMUtils {

    fun copyWebDriverFile(dest: Path) {
        val file = ProjectUtils.findFile("WebDriver.kt")
        if (file != null) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        // If we can not find the file, copy it from github.com or gitee.com
        val webDriverURL =
            "https://raw.githubusercontent.com/platonai/PulsarRPA/refs/heads/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt"
        val webDriverURL2 = "https://gitee.com/platonai_galaxyeye/PulsarRPA/tree/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt"
        listOf(webDriverURL, webDriverURL2).forEach { url ->
            if (Files.size(dest) < 100) {
                Files.writeString(dest, URI(url).toURL().readText())
            }
        }
    }

    fun copyPulsarSessionFile(dest: Path) {
        val file = ProjectUtils.findFile("PulsarSession.kt")
        if (file != null) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        // If we can not find the file, copy it from github.com or gitee.com
        val webDriverURL =
            "https://raw.githubusercontent.com/platonai/PulsarRPA/refs/heads/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/session/PulsarSession.kt"
        val webDriverURL2 = "https://gitee.com/platonai_galaxyeye/PulsarRPA/tree/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/session/PulsarSession.kt"
        listOf(webDriverURL, webDriverURL2).forEach { url ->
            if (Files.size(dest) < 100) {
                Files.writeString(dest, URI(url).toURL().readText())
            }
        }
    }
}
