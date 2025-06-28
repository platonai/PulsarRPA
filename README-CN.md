# 🤖 PulsarRPA

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/pulsar-rpa?style=flat-square)](https://hub.docker.com/r/galaxyeye88/pulsar-rpa)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/PulsarRPA/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 简介

💖 **PulsarRPA：AI 驱动的极速浏览器自动化解决方案！** 💖

### ✨ 主要功能：

- 🤖 **AI 大模型集成** —— 由大语言模型驱动的智能自动化。
- ⚡ **超快自动化** —— 协程安全的浏览器自动化并发，爬虫级抓取性能。
- 🧠 **网页理解能力** —— 深度理解动态网页内容。
- 📊 **数据提取 API** —— 轻松提取结构化数据的强大工具。

---

用简单的文本批量自动化浏览器并提取数据。

```text
前往 https://www.amazon.com/dp/B0C1H26C46

浏览器启动后：清除浏览器 Cookie。
页面加载后：滚动到页面中部。

总结该商品。
提取：商品名称、价格、评分。
查找所有包含 /dp/ 的链接。
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

# 🚀 快速开始指南

## ▶️ 运行 PulsarRPA

### 📦 运行可执行 JAR —— 推荐体验

#### 🧩 下载

```shell
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.14/PulsarRPA.jar
```

#### 🚀 运行

```shell
# 请确保已设置 LLM API 密钥。也支持 VOLCENGINE_API_KEY/OPENAI_API_KEY。
echo $DEEPSEEK_API_KEY
java -D"DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}" -jar PulsarRPA.jar
```

> 🔍 **提示：** 请确保环境变量中已设置 `DEEPSEEK_API_KEY` 或其他 LLM API 密钥，否则 AI 功能不可用。

> 🔍 **Tip:** Windows PowerShell syntax: `$env:DEEPSEEK_API_KEY` (environment variable) vs `$DEEPSEEK_API_KEY` (script variable).

---

<details>
<summary>📂 资源</summary>

* 🟦 [GitHub Release 下载](https://github.com/platonai/PulsarRPA/releases/download/v3.0.14/PulsarRPA.jar)
* 📁 [镜像/备份下载](https://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠️ [LLM 配置指南](docs/config/llm/llm-config.md)
* 🛠️ [通用配置指南](docs/config.md)

</details>

### ▶ 使用 IDE 运行

<details>

- 用你的 IDE 打开项目
- 运行 `ai.platon.pulsar.app.PulsarApplicationKt` 主类

</details>

### 🐳 Docker 用户

<details>

```shell
# 请确保已设置 LLM API 密钥。也支持 VOLCENGINE_API_KEY/OPENAI_API_KEY。
echo $DEEPSEEK_API_KEY
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```

</details>

---

## 🌟 新手入门 �?纯文本描述，无需编程�?

使用 `commands` API 执行浏览器操作、提取网页数据、分析网站等功能�?

### 📥 示例请求（基于文本描述）�?

网页界面：http://localhost:8182/command.html

<img src="docs/images/commander-ui.png" alt="commander" width="500" />

<details>
<summary>REST API</summary>

#### 📄 纯文本版本：
```shell
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d '
    访问 https://www.amazon.com/dp/B0C1H26C46
    
    浏览器启动后：清除浏览器cookies
    页面加载后：滚动到页面中�?
    
    总结产品信息
    提取：产品名称、价格、评�?
    查找所有包�?/dp/ 的链�?
  '
```

#### 📄 JSON版本�?

```shell
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "onBrowserLaunchedActions": ["清除浏览器cookies"],
    "onPageReadyActions": ["滚动到页面中�?],
    "pageSummaryPrompt": "提供这个产品的简要介�?,
    "dataExtractionRules": "产品名称、价格和评分",
    "uriExtractionRules": "页面上所有包�?`/dp/` 的链�?
  }'
```

💡 **提示�?* 您不需要填写每个字�?�?只需填写您需要的内容�?

</details>

## 🎓 进阶用户 �?大语言模型 + X-SQL：精确、灵活、强�?

利用 `x/e` API 的强大功能，实现高精度、灵活且智能的数据提取�?

  ```shell
  curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称、价格、评�?) as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
  "
  ```

提取数据示例�?

```json
{
  "llm_extracted_data": {
    "产品名称": "Apple iPhone 15 Pro Max",
    "价格": "$1,199.00",
    "评分": "4.5星（满分5星）"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

* X-SQL指南：[X-SQL](docs/x-sql.md)

---

## 👨‍�?专家用户 - 原生API：功能强大！

### 🚀 超快速页面访问和数据提取�?

PulsarRPA 以协程速度并行访问网页，高效提取数据的同时最小化资源消耗�?

<details>

```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
val links =
    LinkExtractors.fromResource(resource).asSequence().map { ListenableHyperlink(it, "", args = args) }.onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }.toList()

session.submitAll(links)
```

📝 Example: [View Kotlin Code](https://github.com/platonai/PulsarRPA/blob/master/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/advanced/HighPerformanceCrawler.kt)

</details>

### 🎮 浏览器控制：

PulsarRPA 实现了协程安全的浏览器控制�?

<details>

```kotlin
val prompts = """
将鼠标移动到id�?title'的元素并点击
滚动到页面中�?
滚动到页面顶�?
获取id�?title'的元素文�?
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例：[查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

</details>

---

### 🤖 机器人流程自动化能力�?

PulsarRPA 提供灵活的机器人流程自动化�?

<details>

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver ->
    warnUpBrowser(page, driver)
}
event.onWillFetch.addLast { page, driver ->
    waitForReferrer(page, driver)
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page, driver ->
    driver.waitForSelector("body h1[itemprop=name]")
    driver.click(".mask-layer-close-button")
}
session.load(url, options)
```
📝 示例：[查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

</details>

---

### 🔍 使用X-SQL进行复杂数据提取�?

PulsarRPA 提供 X-SQL 进行复杂数据提取�?

<details>

```sql
select
    llm_extract(dom, '产品名称、价格、评分、评分数�?) as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码�?
* [亚马逊产品页面抓取（100+字段）](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [全部亚马逊页面类型抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

</details>

---

## 📜 文档

* 📖 [REST API示例](docs/rest-api-examples.md)
* 🛠�?[大语言模型配置指南](docs/config/llm/llm-config.md)
* 🛠�?[配置指南](docs/config.md)
* 📚 [从源码构建](docs/development/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理服务�?- 解锁网站访问

<details>

设置环境变量 PROXY_ROTATION_URL 为您的代理服务提供的URL�?

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问轮换URL时，它应该返回包含一个或多个新鲜代理IP的响应�?
请向您的代理服务提供商询问此类URL�?

</details>

---

## �?功能特�?

🕷�?**网络爬虫**
- 可扩展爬�?
- 浏览器渲�?
- AJAX数据提取

🤖 **AI驱动**
- 自动字段提取
- 模式识别
- 精确数据捕获

🧠 **大语言模型集成**
- 自然语言网页内容分析
- 直观的内容描�?

🎯 **文本转操�?*
- 简单语言命令
- 直观的浏览器控制

🤖 **机器人流程自动化能力**
- 类人任务自动�?
- 单页应用程序爬取支持
- 高级工作流自动化

🛠�?**开发者友�?*
- 一行代码数据提�?
- 类SQL查询接口
- 简单API集成

📊 **X-SQL强大功能**
- 为网页数据扩展的SQL
- 内容挖掘能力
- 网络商业智能

🛡�?**反爬虫保�?*
- 高级隐身技�?
- IP轮换
- 隐私上下文管�?

�?**高性能**
- 并行页面渲染
- 高效处理
- 抗阻塞设�?

💰 **成本效益**
- 每天100,000+页面
- 最低硬件要�?
- 资源高效运行

�?**质量保证**
- 智能重试机制
- 精确调度
- 完整生命周期管理

🌐 **可扩展�?*
- 完全分布式架�?
- 大规模处理能�?
- 企业级就�?

📦 **存储选项**
- 本地文件系统
- MongoDB
- HBase
- Gora支持

📊 **监控功能**
- 全面日志记录
- 详细指标
- 完全透明

## 📞 联系我们

- 💬 微信：galaxyeye
- 🌐 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 推特：galaxyeye8
- 🌍 官网：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="微信二维�? />
</div>
