# 🤖 PulsarRPA

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 简介

💖 **PulsarRPA：您的终极 AI-RPA 解决方案！** 💖

**PulsarRPA** 是一个**高性能** 🚀、**分布式** 🌐 和**开源** 🔓 的机器人流程自动化（RPA）框架。
专为**大规模自动化** 🏭 设计，在以下方面表现出色：
- 🌐 **浏览器自动化，超级快⚡⚡**
- 🧠 **网页内容理解**
- 📊 **数据提取**

PulsarRPA 解决了现代网页自动化的挑战，
确保从最**复杂** 🔄 和**动态** ⚡ 的网站中也能进行**准确** ✅ 和**全面** 📚 的数据提取。

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

## 🐳 Docker 设置

### 🔧 基础设置（无 LLM）

```shell
docker run -d -p 8182:8182 galaxyeye88/pulsar-rpa:latest
```

### 🧠 LLM 集成

🔑 获取您的 API 密钥：
https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey?apikey=xxx

```shell
docker run -d -p 8182:8182 \
  -e llm.provider=volcengine \
  -e llm.name=${YOUR-MODEL_NAME} \
  -e llm.apiKey=${YOUR-LLM_API_KEY} \
  galaxyeye88/pulsar-rpa:latest
```

## 🚀 快速入门指南

### 🌟 面向初学者 - 无需特殊技能！

#### 💬 与网页对话
```shell
curl -X POST "http://localhost:8182/api/ai/chat-about" \
-H "Content-Type: application/json" \
-d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "介绍这个产品"
}'
```

#### 📊 提取数据
```shell
curl -X POST "http://localhost:8182/api/ai/extract" \
-H "Content-Type: application/json" \
-d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "产品名称、价格和描述"
}'
```

### 🎓 面向高级用户 - LLM + X-SQL

```bash
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称、价格、评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

### 👨‍💻 面向专家 - 原生 API

#### 💭 与网页对话：
```kotlin
val document = session.loadDocument(url)
val response = session.chat("告诉我关于这个网页的信息", document)
```
📝 示例：[查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt)

#### 🎮 浏览器控制：
```kotlin
val prompts = """
将光标移动到 id 为 'title' 的元素并点击
滚动到中间
滚动到顶部
获取 id 为 'title' 的元素的文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例：[查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

#### ⚡ 一行代码抓取：
```kotlin
session.scrapeOutPages(
    "https://www.amazon.com/",  
    "-outLink a[href~=/dp/]", 
    listOf("#title", "#acrCustomerReviewText")
)
```

#### 🤖 RPA 爬取：
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
📝 示例：[查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

#### 🔍 使用 X-SQL 进行复杂数据提取：
```sql
select
    llm_extract(dom, '产品名称、价格、评分、分数') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码：
* [亚马逊产品页面抓取（100+字段）](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [所有亚马逊页面类型抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

📖 [高级指南](docs/advanced-guides.md)

## ✨ 特性

🕷️ **网页爬虫**
- 可扩展的爬取
- 浏览器渲染
- AJAX 数据提取

🧠 **LLM 集成**
- 自然语言网页内容分析
- 直观的内容描述

🎯 **文本到动作**
- 简单的语言命令
- 直观的浏览器控制

🤖 **RPA 能力**
- 类人任务自动化
- SPA 爬取支持
- 高级工作流自动化

🛠️ **开发者友好**
- 一行代码数据提取
- SQL 式查询接口
- 简单的 API 集成

📊 **X-SQL 能力**
- 网页数据扩展 SQL
- 内容挖掘能力
- 网页商业智能

🛡️ **机器人保护**
- 高级隐身技术
- IP 轮换
- 隐私上下文管理

⚡ **高性能**
- 并行页面渲染
- 高效处理
- 防阻塞设计

💰 **成本效益**
- 每天 100,000+ 页面
- 最低硬件要求
- 资源高效运行

✅ **质量保证**
- 智能重试机制
- 精确调度
- 完整的生命周期管理

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

🤖 **AI 驱动**
- 自动字段提取
- 模式识别
- 准确数据捕获

## 📞 联系我们

- 💬 微信：galaxyeye
- 🌐 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter：galaxyeye8
- 🌍 网站：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="微信二维码" />
  <img src="docs/images/buy-me-a-coffee.png" width="300" alt="支持我们" />
</div>

