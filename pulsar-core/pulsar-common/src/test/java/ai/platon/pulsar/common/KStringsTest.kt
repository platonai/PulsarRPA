package ai.platon.pulsar.common

import kotlin.test.Test
import kotlin.test.assertEquals

class KStringsTest {
    val prompt = """
## 支持的工具列表：

```kotlin
driver.click(selector: String)
```

- domain: 方法的调用方，如 driver, browser 等
- 将 `locator` 视为 `selector`

## 无障碍树（Accessibility Tree）说明：

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

        """.trimIndent()

    @Test
    fun testReplaceContentInSections() {
        val brief = compactPrompt(prompt)
        printlnPro(brief)
    }

    fun compactPrompt(prompt: String, maxWidth: Int = 20000): String {
        val boundaries = """
你正在通过根据用户希望观察的页面内容来查找元素
否则返回空数组。

## 支持的工具列表
##

## 无障碍树(Accessibility Tree)
##
            """.trimIndent()

        val boundaryPairs = boundaries.split("\n").filter { it.isNotBlank() }.chunked(2).map { it[0] to it[1] }
        assertEquals("## 支持的工具列表", boundaryPairs[1].first)
        assertEquals("##", boundaryPairs[1].second)

        val compacted = KStrings.replaceContentInSections(prompt, boundaryPairs, "\n...\n\n")

        return compacted
    }
}
