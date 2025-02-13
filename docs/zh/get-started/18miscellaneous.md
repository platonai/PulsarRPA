杂项
=

浏览器设置
==

`BrowserSettings` 定义了一个简单的接口来管理浏览器行为。它提供了一系列静态方法来定制浏览器配置。
例如，要在无头模式下启动几个临时的浏览器——这在网页抓取操作中很常见——请使用以下代码片段：

```kotlin
BrowserSettings
  .headless()
  .maxBrowsers(4)
  .maxOpenTabs(12)
  .enableUrlBlocking()
```

这段代码实现了以下功能：

1. 以无头模式启动浏览器。
2. 配置四个独立的隐私环境。
3. 将每个浏览器环境限制在最多12个打开的标签页。
4. 激活URL阻止。

要使用具有图形用户界面（GUI）的系统默认浏览器并与网页进行交互，请使用以下配置：

```kotlin
BrowserSettings.withSystemDefaultBrowser().withGUI().withSPA()
```

上述配置执行以下操作：
1. 使用系统默认浏览器。
2. 以GUI模式操作浏览器。
3. 配置系统以处理单页应用程序（SPA）。

隐私代理
==

隐私代理确保网站访问具有不同的身份。当使用不同的隐私代理时，无论它们是否来自同一主机，页面访问不应被识别为同一个人。

Concepts
==

要释放 PulsarRPA 的全部潜能并应对最复杂的数据抓取任务，对其核心概念的扎实理解至关重要。通过掌握这些基本原理，您将能够使用 PulsarRPA 作为从网络中提取有价值信息的强大工具。

[PulsarRPA 概念](../concepts.md)

[上一篇](16console.md) [主页](1home.md)
