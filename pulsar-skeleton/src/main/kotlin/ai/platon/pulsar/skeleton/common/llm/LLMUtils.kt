package ai.platon.pulsar.skeleton.common.llm

import ai.platon.pulsar.common.code.ProjectUtils
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object LLMUtils {

    val webDriverMessageTemplate = """
以下是操作网页的 API 接口及其注释，你可以使用这些接口来操作网页，比如打开网页、点击按钮、输入文本等等。

{{webDriverSourceCode}}

WebDriver 代码结束。

如果用户要求你进行页面操作，你需要生成一个挂起函数实现用户的需求。
该函数接收一个 WebDriver 对象，你仅允许使用 WebDriver 接口中的函数。
你的返回结果仅包含该函数，不需要任何注释或者解释，返回格式如下：
```kotlin
suspend fun llmGeneratedFunction(driver: WebDriver) {
    // 你的代码
}
```


        """.trimIndent()

    fun copyWebDriverFile(dest: Path) {
        val file = ProjectUtils.findFile("WebDriver.kt")
        if (file != null) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        // If we can not find WebDriver.kt, copy it from github.com or gitee.com
        val webDriverURL =
            "https://raw.githubusercontent.com/platonai/PulsarRPA/refs/heads/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt"
        val webDriverURL2 = "https://gitee.com/platonai_galaxyeye/PulsarRPA/tree/master/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt"
        listOf(webDriverURL, webDriverURL2).forEach { url ->
            if (Files.size(dest) < 100) {
                Files.writeString(dest, URI(url).toURL().readText())
            }
        }
    }
}
