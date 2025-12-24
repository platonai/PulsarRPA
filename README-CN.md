# 🤖 Browser4

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/browser4?style=flat-square)](https://hub.docker.com/r/galaxyeye88/browser4)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/browser4/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

English | **简体中文** | [中国镜像](https://gitee.com/platonai_galaxyeye/Browser4)

<!-- TOC -->
**目录**
- [🤖 Browser4](#-browser4)
    - [🌟 项目介绍](#-项目介绍)
        - [✨ 核心能力](#-核心能力)
    - [🎥 演示视频](#-演示视频)
    - [🚀 快速开始](#-快速开始)
    - [💡 使用示例](#-使用示例)
        - [浏览器智能体](#浏览器智能体)
        - [流程自动化](#流程自动化)
        - [LLM + X-SQL](#llm--x-sql)
        - [高速并行处理](#高速并行处理)
        - [自动抽取](#自动抽取)
    - [📦 模块总览](#-模块总览)
    - [📜 文档](#-文档)
    - [🔧 代理与解锁](#-代理与解锁)
    - [✨ 特性](#-特性)
    - [🤝 支持与社区](#-支持与社区)
<!-- /TOC -->


## 🤖 Browser4：为你的 AI 智能体打造高性能「身体」

> **带上你的大脑（LLM），我们提供身体。**
> Browser4 是让人工智能理解、交互并在万维网上稳定运行的基础设施层。

### ✨ 核心能力

* 👽 **浏览器智能体** — 能在浏览器中自主推理、规划与执行任务。
* 🤖 **浏览器自动化** — 高性能工作流、导航与数据抽取自动化。
* ⚙️ **机器学习智能体** — 无需消耗 Token，自动学习复杂页面的字段结构。
* ⚡  **极致性能** — 完整协程安全，每台机器每日可支撑 10 万 ~ 20 万次的页面访问。

## ⚡ 快速示例：智能体工作流

```kotlin
// 给智能体一个任务，而不仅是脚本。
val agent = AgenticContexts.getOrCreateAgent()

// 智能体利用 Browser4 作为眼手进行规划、导航与执行。
val result = agent.run("""
    1. Go to amazon.com
    2. Search for '4k monitors'
    3. Analyze the top 5 results for price/performance ratio
    4. Return the best option as JSON
""")
```

---

## 🎥 演示视频

🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/rJzXNXH3Gwk/0.jpg)](https://youtu.be/rJzXNXH3Gwk)

📺 Bilibili:
[https://www.bilibili.com/video/BV1fXUzBFE4L](https://www.bilibili.com/video/BV1fXUzBFE4L)

---

## 🚀 快速开始

**前置条件**：Java 17+ 与 Maven 3.6+

1. **克隆仓库**
   ```shell
   git clone https://github.com/platonai/browser4.git
   cd browser4
   ```

2. **配置 LLM API Key**

   > 编辑 [application.properties](application.properties) 并写入你的 API Key。

3. **构建项目**
   ```shell
   ./mvnw -q -DskipTests
   ```

4. **运行示例**
   ```shell
   ./mvnw -pl pulsar-examples exec:java -D"exec.mainClass=ai.platon.pulsar.examples.agent.Browser4AgentKt"
   ```
   Windows 如遇到编码问题可使用：
   ```shell
   ./bin/run-examples.ps1
   ```

   欢迎在 `pulsar-examples` 模块中探索更多示例。

若需 Docker 部署，请访问我们的 [Docker Hub 仓库](https://hub.docker.com/r/galaxyeye88/browser4)。

---

## 💡 使用示例

### 浏览器智能体

智能体能够理解自然语言，并自动执行复杂的浏览器任务。

```kotlin
val agent = AgenticContexts.getOrCreateAgent()

val task = """
    1. go to amazon.com
    2. search for pens to draw on whiteboards
    3. compare the first 4 ones
    4. write the result to a markdown file
    """

agent.run(task)
```

### 流程自动化

基于底层控制的浏览器自动化与数据抽取，提供更细粒度的能力。

**特性：**
- 直接且完整地控制 Chrome DevTools Protocol (CDP)，并保持协程安全
- 精准的元素操作（点击、滚动、输入）
- 借助 CSS 选择器 / XPath 快速抽取数据

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
var content = driver.selectFirstTextOrNull("#comments")

// 复杂智能体任务
var history = agent.run("Search for 'smart phone', read the first four products, and give me a comparison.")

// 捕获当前状态并抽取
page = session.capture(driver)
document = session.parse(page)
fields = session.extract(document, mapOf("ratings" to "#ratings"))
```

### LLM + X-SQL

适用于包含数十实体、数百字段的复杂抽取场景。

**优势：**
- 相比传统方式可额外抽取 10 倍实体、100 倍字段
- 将 LLM 智能与精确的 CSS 选择器 / XPath 结合
- 类 SQL 的语法，更易上手

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

示例代码：

* [使用 X-SQL 抓取 100+ 个亚马逊商品字段](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [针对不同类型亚马逊页面的 X-SQL 样例](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)


### 高速并行处理

通过并行浏览器控制与智能资源优化，实现极高吞吐量。

**性能：**
- 每台机器每日 100,000+ 次页面访问
- 并发管理多个会话
- 阻止资源以加速页面加载

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

🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

### 自动抽取

由自/半监督机器学习驱动的大规模高精度字段发现与抽取——无需调用 LLM，零 Token 消耗，稳定且快速。

**功能：**
- 自动学习详情页可抽取的全部字段（往往几十到上百个），且准确率极高。
- 当 browser4 获得 10K stars 后，开源全部代码。

**为何不只用 LLM？**
- LLM 抽取会带来延迟、成本与 Token 限制。
- 本地 ML 抽取可复现，可扩展至每日 10~20 万页面。
- 也可组合使用：ML 提供结构化基线，LLM 负责语义增强。

**快捷命令 (PulsarRPAPro)：**
```bash
curl -L -o PulsarRPAPro.jar https://github.com/platonai/PulsarRPAPro/releases/download/v3.0.0/PulsarRPAPro.jar
```

**集成状态：**
- 目前可通过配套项目 [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro) 使用。
- 计划在 Browser4 API 中提供原生支持，敬请关注发布。

**关键优势：**
- 高精度：>95% 字段被发现，大多数字段准确率 >99%（基于测试域）。
- 抗噪：对选择器变化与 HTML 噪声具备鲁棒性。
- 无外部依赖（无需 API Key），大规模运行成本更低。
- 可解释：生成的选择器与 SQL 可审计。

👽 机器学习智能体抽取示意：

![Auto Extraction Result Snapshot](docs/assets/images/amazon.png)

（即将提供更多仓库内示例与直接 API 接入。）

---

## 📦 模块总览

| 模块 | 说明 |
|------|------|
| `pulsar-core` | 核心引擎：会话、调度、DOM、浏览器控制 |
| `pulsar-rest` | Spring Boot REST 层与命令入口 |
| `pulsar-client` | 客户端 SDK / CLI 工具 |
| `browser4-spa` | 浏览器智能体的单页应用 |
| `browser4-agents` | 智能体与爬虫编排及产品化打包 |
| `pulsar-tests` | 重型集成与场景测试 |
| `pulsar-tests-common` | 共享测试工具与夹具 |

---

## 📜 SDK

Python / Node.js SDK 正在筹备中。

## 📜 文档

* 🛠️ [配置指南](docs/config.md)
* 📚 [源码构建](docs/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理与解锁

<details>

将环境变量 PROXY_ROTATION_URL 设置为代理服务提供的轮换地址：

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问该地址，应返回一个或多个新的代理 IP。请向代理服务商获取对应 URL。

</details>

---

## ✨ 特性

### AI 与智能体
- 具备问题求解能力的浏览器智能体
- 并行智能体会话
- LLM 辅助的页面理解与抽取

### 浏览器自动化与 RPA
- 基于工作流的浏览器动作
- 精准的协程安全控制（滚动、点击、抽取）
- 灵活的事件处理与生命周期管理

### 数据抽取与查询
- 一行命令完成数据抽取
- 面向 DOM / 内容的 X-SQL 拓展查询语言
- 结构化 + 非结构化混合抽取（LLM + ML + 选择器）

### 性能与可扩展
- 高效并行渲染
- 抗封锁设计与智能重试
- 以普通硬件实现每日 100,000+ 页（参考值）

### 隐身与可靠性
- 先进的反检测技术
- IP 与浏览器配置轮换
- 韧性调度与质量保障

### 开发者体验
- 简洁的 API 集成（REST、原生、文本命令）
- 丰富的配置分层
- 结构化日志与指标

### 存储与监控
- 本地文件系统与 MongoDB 支持（可扩展）
- 详尽日志，透明可追踪
- 完整的指标与生命周期可视化

---

## 🤝 支持与社区

- 💬 微信：galaxyeye
- 🌐 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter：galaxyeye8
- 🌍 官网：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="WeChat QR Code" />
</div>

---

> 中文文档请参阅本文件，以及更多资料可见 `docs/zh/`。
