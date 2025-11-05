# AI 编程指南

实现以下工具：

```
browser.closeTab(tabId: String)

// domain: fs
fs.writeString(filename: String, content: String)
fs.readString(filename: String)
fs.replaceContent(filename: String, oldStr: String, newStr: String)

```

注意事项：

- 依照 `browser.switchTab(tabId: String): Int` 实现的整个流程实现。

提示：

- 主要改动在 `pulsar-core/pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/tools` 目录下。
