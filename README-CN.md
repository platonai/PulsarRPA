# 🤖 PulsarRPA

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/pulsar-rpa?style=flat-square)](https://hub.docker.com/r/galaxyeye88/pulsar-rpa)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/PulsarRPA/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 简介

💖 **PulsarRPA: 人工智能驱动的超快速浏览器自动化解决方案！** 💖

### ✨ 核心能力：

- 🤖 **AI 集成与大语言模型** – 由大型语言模型驱动的智能自动化。
- ⚡ **超快速自动化** – 协程安全的浏览器自动化并发，爬虫级别的抓取性能。
- 🧠 **网页理解** – 深度理解动态网页内容。
- 📊 **数据提取 API** – 轻松提取结构化数据的强大工具。

---

通过简单的文本指令实现浏览器自动化和大规模数据提取。

```text
访问 https://www.amazon.com/dp/B0C1H26C46
页面加载后: 滚动到页面中部。

总结产品信息。
提取: 产品名称、价格、评分。
查找所有包含 /dp/ 的链接。
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 哔哩哔哩:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

# 🚀 快速入门指南

## ▶️ 运行 PulsarRPA

### 📦 运行可执行 Jar 包 - 最佳体验

下载:

```bash
# Linux/macOS 和 Windows (如果安装了 curl)
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.1.0/PulsarRPA.jar
```

```bash
java -DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPA.jar
```

> 🔍 提示: 确保环境中设置了 DEEPSEEK_API_KEY 以访问 AI 功能。

<details>
<summary>📂 相关资源</summary>

* 🟦 [Official GitHub Release](https://github.com/platonai/PulsarRPA/releases/download/v3.0.4/PulsarRPA.jar)
* 📁 [Mirror / Backup Download](http://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠️ [Configuration Guide](docs/config.md)
* 🤖 [Switch to a Different LLM Provider](docs/config/llm/llm-config)

</details>

### ▶ 在 IDE 中运行

<details>

- 在 IDE 中打开项目
- 运行 `ai.platon.pulsar.app.PulsarApplicationKt` 主类

</details>

### 🐳 Docker 用户

<details>

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```

</details>

---

## 🌟 初学者指南 – 只需文本，无需代码！

使用 `ai/command` API 通过自然语言指令执行操作和提取数据。

### 📥 示例请求（基于文本）:

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    访问 https://www.amazon.com/dp/B0C1H26C46
    页面加载后: 点击 #title，然后滚动到页面中部。
    
    总结产品信息。
    提取: 产品名称、价格、评分。
    查找所有包含 /dp/ 的链接。
  '
```

💡 **提示:** 你不需要填写所有字段 — 只需填写你需要的部分。

### 📄 基于 JSON 的版本:

<details>

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "pageSummaryPrompt": "简要介绍这个产品。",
    "dataExtractionRules": "产品名称、价格和评分",
    "linkExtractionRules": "页面上所有包含 `/dp/` 的链接",
    "onPageReadyActions": ["点击 #title", "滚动到页面中部"]
  }'
```

</details>

## 🎓 进阶用户指南 — LLM + X-SQL: 精确、灵活、强大

利用 `x/e` API 进行高度精确、灵活和智能的数据提取。

  ```bash
  curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称、价格、评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
  "
  ```

提取的数据示例:

```json
{
  "llm_extracted_data": {
    "产品名称": "Apple iPhone 15 Pro Max",
    "价格": "$1,199.00",
    "评分": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

* X-SQL 指南: [X-SQL](docs/x-sql.md)

---

## 👨‍💻 专家指南 - 原生 API: 强大无比!

### 🎮 浏览器控制:

<details>

```kotlin
val prompts = """
将光标移动到 id 为 'title' 的元素上并点击它
滚动到页面中部
滚动到页面顶部
获取 id 为 'title' 的元素的文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例: [查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

</details>

---

### 🤖 完整的机器人流程自动化能力:

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
📝 示例: [查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

</details>

---

### 🔍 使用 X-SQL 进行复杂数据提取:

<details>

```sql
select
    llm_extract(dom, '产品名称、价格、评分、评分分数') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码:
* [亚马逊产品页面抓取 (100+ 字段)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [亚马逊所有页面类型抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

</details>

---

## 📜 文档

* 📖 [REST API 示例](docs/rest-api-examples.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理 - 解除网站限制

<details>

设置环境变量 PROXY_ROTATION_URL 为代理服务提供的 URL:

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问轮换 URL 时，它应该返回一个包含一个或多个新代理 IP 的响应。

</details>

---

## ✨ 特性

🕷️ **网络爬虫**
- 可扩展爬取
- 浏览器渲染
- AJAX 数据提取

🤖 **AI 驱动**
- 自动字段提取
- 模式识别
- 准确数据捕获

🧠 **LLM 集成**
- 自然语言网页内容分析
- 直观内容描述

🎯 **文本转动作**
- 简单语言命令
- 直观浏览器控制

🤖 **RPA 能力**
- 类人任务自动化
- SPA 爬取支持
- 高级工作流自动化

🛠️ **开发者友好**
- 一行数据提取
- 类 SQL 查询界面
- 简单 API 集成

📊 **X-SQL 强大功能**
- 扩展 SQL 用于网络数据
- 内容挖掘能力
- 网络商业智能

🛡️ **机器人保护**
- 高级隐身技术
- IP 轮换
- 隐私上下文管理

⚡ **性能**
- 并行页面渲染
- 高效处理
- 防屏蔽设计

💰 **成本效益**
- 每天 100,000+ 页面
- 最小硬件需求
- 资源高效运行

✅ **质量保障**
- 智能重试机制
- 精确调度
- 完整生命周期管理

🌐 **可扩展性**
- 完全分布式架构
- 大规模能力
- 企业级就绪

📦 **存储选项**
- 本地文件系统
- MongoDB
- HBase
- Gora 支持

📊 **监控**
- 全面日志记录
- 详细指标
- 完全透明

## 📞 联系我们

- 💬 微信: galaxyeye
- 🌐 微博: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter: galaxyeye8
- 🌍 网站: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="微信二维码" />
</div>
