# 🤖 Browser4

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/browser4?style=flat-square)](https://hub.docker.com/r/galaxyeye88/browser4)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/browser4/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/Browser4)

<!-- TOC -->
**目录**
- [🤖 Browser4](#-browser4)
    - [🌟 项目介绍](#-项目介绍)
        - [✨ 核心能力](#-核心能力)
    - [🎥 演示视频](#-演示视频)
    - [🚀 快速开始](#-快速开始)
    - [💡 使用示例](#-使用示例)
        - [浏览器智能体](#浏览器智能体)
        - [工作流自动化](#工作流自动化)
        - [LLM + X-SQL](#llm--x-sql)
        - [高速并行处理](#高速并行处理)
    - [📦 模块概览](#-模块概览)
    - [📜 文档](#-文档)
    - [🔧 代理配置 - 解锁网站访问](#-代理配置---解锁网站访问)
    - [✨ 功能特性](#-功能特性)
    - [🤝 支持与社区](#-支持与社区)
<!-- /TOC -->

## 🌟 项目介绍

💖 **Browser4: 为 AI 自动化打造的闪电般快速、协程安全的浏览器引擎** 💖

### ✨ 核心能力

* 👽 **浏览器智能体** — 能够在浏览器中推理、规划和行动的自主智能体。
* 🤖 **浏览器自动化** — 用于工作流、导航和数据提取的高性能自动化。
* ⚡ **极致性能** — 完全协程安全；支持每台机器每天访问 100k+ 页面。
* 🧠 **网页理解** — 深度理解动态、脚本驱动和交互式网页。
* 📊 **数据提取 API** — 强大的 API，轻松提取结构化数据。

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 哔哩哔哩:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

## 🚀 快速开始

**前置要求**：Java 17+ 和 Maven 3.6+

1. **克隆仓库**
   ```shell
   git clone https://github.com/platonai/browser4.git
   cd browser4
   ```

2. **配置 LLM API 密钥**
   编辑 [application.properties](application.properties) 并添加你的 API 密钥。

3. **构建项目**（Windows）
   ```cmd
   mvnw.cmd -q -DskipTests
   ```
   或者在 Linux/macOS 上：
   ```bash
   ./mvnw -q -DskipTests
   ```

4. **运行示例**
   浏览并运行 `pulsar-examples` 模块中的示例，体验 Browser4 的实际效果。

Docker 部署请查看我们的 [Docker Hub 仓库](https://hub.docker.com/r/galaxyeye88/browser4)。

---

## 💡 使用示例

### 浏览器智能体

能够理解自然语言指令并执行复杂浏览器工作流的自主智能体。

```kotlin
val agent = AgenticContexts.getOrCreateAgent()

val problem = """
    1. go to amazon.com
    2. search for pens to draw on whiteboards
    3. compare the first 4 ones
    4. write the result to a markdown file
    """

agent.resolve(problem)
```

### 工作流自动化

底层浏览器自动化和数据提取，提供细粒度控制。

**功能特性:**
- 直接和完整的 Chrome DevTools Protocol (CDP) 控制
- 精确的元素交互（点击、滚动、输入）
- 使用 CSS 选择器/XPath 快速提取数据

```kotlin
val session = AgenticContexts.getOrCreateSession()
val agent = session.companionAgent
val driver = session.getOrCreateBoundDriver()

// 打开并解析页面
var page = session.open(url)
var document = session.parse(page)
var fields = session.extract(document, mapOf("title" to "#title"))

// 与页面交互
var result = agent.act("scroll to the comment section")
var content = driver.selectFirstTextOrNull("body")

// 复杂的智能体任务
result = agent.resolve("Search for 'smart phone', read the first four products, and give me a comparison.")

// 捕获并提取当前状态
page = session.capture(driver)
document = session.parse(page)
fields = session.extract(document, mapOf("ratings" to "#ratings"))
```

### LLM + X-SQL

适用于具有多个实体和每个实体数百个字段的高复杂度数据提取管道。

**优势:**
- 与传统方法相比，提取 10 倍的实体数和 100 倍的字段数
- 结合 LLM 智能与精确的 CSS 选择器/XPath
- 类 SQL 语法，易于理解和使用

```kotlin
val context = AgenticContexts.create()
val sql = """
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_text(dom, '#bylineInfo') as brand,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
  dom_first_text(dom, '#acrCustomerReviewText') as ratings,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB -i 1s -njr 3', 'body');
"""
val rs = context.executeQuery(sql)
println(ResultSetFormatter(rs, withHeader = true))
```

### 高速并行处理

通过并行浏览器控制和智能资源优化实现极致吞吐量。

**性能:**
- 每台机器每天访问 100,000+ 页面
- 并发会话管理
- 资源阻断以加快页面加载速度

```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val blockingUrls = listOf("*.png", "*.jpg")
val links = LinkExtractors.fromResource("urls.txt")
    .map { ListenableHyperlink(it, "", args = args) }
    .onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }

session.submitAll(links)
```
---

## 📦 模块概览

| 模块 | 说明 |
|--------|-------------|
| `pulsar-core` | 核心引擎：会话、调度、DOM、浏览器控制 |
| `pulsar-rest` | Spring Boot REST 层和命令端点 |
| `pulsar-client` | 客户端 SDK / CLI 工具 |
| `browser4-spa` | 用于浏览器智能体的单页应用 |
| `browser4-agents` | 智能体和爬虫编排及产品打包 |
| `pulsar-tests` | 重型集成和场景测试 |
| `pulsar-tests-common` | 共享测试工具和固件 |

---

## 📜 文档

* 🛠️ [配置指南](docs/config.md)
* 📚 [从源码构建](docs/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理配置 - 解锁网站访问

<details>

将环境变量 PROXY_ROTATION_URL 设置为代理服务提供的 URL：

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问轮换 URL 时，它应返回包含一个或多个新代理 IP 的响应。
请向您的代理提供商询问此类 URL。

</details>

---

## ✨ 功能特性

### AI 与智能体
- 解决问题的自主浏览器智能体
- 并行智能体会话
- LLM 辅助的页面理解和提取

### 浏览器自动化与 RPA
- 基于工作流的浏览器操作
- 精准的协程安全控制（滚动、点击、提取）
- 灵活的事件处理器和生命周期管理

### 数据提取与查询
- 单行数据提取命令
- 用于 DOM/内容的 X-SQL 扩展查询语言
- 结构化+非结构化混合提取（LLM + 选择器）

### 性能与可扩展性
- 高效并行页面渲染
- 抗封锁设计和智能重试
- 在普通硬件上每天处理 100,000+ 页面（指示性）

### 隐身与可靠性
- 高级反机器人技术
- IP 和配置文件轮换
- 弹性调度和质量保证

### 开发者体验
- 简单的 API 集成（REST、原生、文本命令）
- 丰富的配置分层
- 清晰的结构化日志和指标

### 存储与监控
- 本地文件系统和 MongoDB 支持（可扩展）
- 全面的日志和透明度
- 详细的指标和生命周期可见性

---

## 🤝 支持与社区

- 💬 微信：galaxyeye
- 🌐 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter：galaxyeye8
- 🌍 网站：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="微信二维码" />
</div>

---

> 英文文档请参阅 [English README](README.md)。

