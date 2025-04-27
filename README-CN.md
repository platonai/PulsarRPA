# 🤖 PulsarRPA

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 介绍

💖 **PulsarRPA：一款超快速、支持 AI 的浏览器自动化解决方案！** 💖

**PulsarRPA** 是一款 **支持 AI🤖**、**高性能** 🚀、**分布式** 🌐、**开源** 🔓 的浏览器自动化解决方案。
专为 **大规模自动化** 🏭 设计，擅长：
- 🌐 **浏览器自动化，超快速⚡，爬虫级别❗**
- 🤖 **支持 AI，LLM 驱动🧠**
- 🧠 **网页内容理解**
- 📊 **数据提取**

PulsarRPA 解决了现代网络自动化的挑战，
确保即使从最**复杂** 🔄 和**动态** ⚡ 的网站也能**准确** ✅ 和**全面** 📚 地提取数据。

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

📺 哔哩哔哩:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

## 🚀 快速入门指南

### 🌟 初学者 - 无需特殊技能!

#### 下载

下载最新的可执行Jar并运行：

* [Github](https://github.com/platonai/PulsarRPA/releases/download/v3.0.2/PulsarRPA.jar)
* [For Chinese User](http://static.platonai.cn/repo/ai/platon/pulsar/PulsarRPA.jar)

```shell
java -D DEEPSEEK_API_KEY=${您的DEEPSEEK_API_KEY} -jar PulsarRPA.jar
```

如果您不需要LLM功能，可以忽略DEEPSEEK_API_KEY。

#### 💬 关于网页的对话

使用`chat-about` API与网页内容交流:

```shell
curl -X POST "http://localhost:8182/api/ai/chat-about" -H "Content-Type: application/json" -d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "介绍这个产品"
}'
```

#### 📊 提取数据

使用`extract` API从网页提取数据:

```shell
curl -X POST "http://localhost:8182/api/ai/extract" -H "Content-Type: application/json" -d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "产品名称，价格和描述"
}'
```

#### 💬 与AI对话

使用`chat` API提问:

```shell
curl http://localhost:8182/api/ai/chat?prompt=当今最神奇的技术是什么
```

使用`post`方法发送更长的提示:

```shell
curl -X POST "http://localhost:8182/api/ai/chat" -H "Content-Type: application/json" -d '
当今最神奇的技术是什么？
请返回5个项目的列表。
'
```

### 🎓 进阶用户 - LLM + X-SQL

```bash
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称, 价格, 评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

提取的数据:
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

### 👨‍💻 专家用户 - 原生API

#### 💭 关于网页的对话:
```kotlin
val document = session.loadDocument(url)
val response = session.chat("告诉我这个网页的信息", document)
```
📝 示例: [查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt)

#### 🎮 浏览器控制:
```kotlin
val prompts = """
将鼠标移动到id为'title'的元素并点击
滚动到中间
滚动到顶部
获取id为'title'的元素的文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例: [查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

#### ⚡ 一行代码爬取:
```kotlin
session.scrapeOutPages(
    "https://www.amazon.com/",
    "-outLink a[href~=/dp/]",
    listOf("#title", "#acrCustomerReviewText")
)
```

#### 🤖 RPA爬取:
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
📝 示例: [查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

#### 🔍 使用X-SQL进行复杂数据提取:
```sql
select
    llm_extract(dom, '产品名称, 价格, 评分, 得分') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码:
* [亚马逊产品页面爬取(100+字段)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [所有亚马逊页面类型爬取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

📖 [进阶指南](docs/advanced-guides.md)

## ✨ 特性

🕷️ **网络爬虫**
- 可扩展爬取
- 浏览器渲染
- AJAX数据提取

🧠 **LLM集成**
- 自然语言网页内容分析
- 直观内容描述

🎯 **文本转行动**
- 简单语言命令
- 直观浏览器控制

🤖 **RPA能力**
- 类人任务自动化
- SPA爬取支持
- 高级工作流自动化

🛠️ **开发者友好**
- 一行代码数据提取
- 类SQL查询接口
- 简单API集成

📊 **X-SQL强大功能**
- 扩展SQL用于网络数据
- 内容挖掘能力
- 网络商业智能

🛡️ **机器人保护**
- 高级隐身技术
- IP轮换
- 隐私上下文管理

⚡ **高性能**
- 并行页面渲染
- 高效处理
- 反屏蔽设计

💰 **成本效益**
- 每天10万+页面
- 最低硬件要求
- 资源高效运行

✅ **质量保证**
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
- Gora支持

📊 **监控**
- 全面日志
- 详细指标
- 完全透明

🤖 **AI驱动**
- 自动字段提取
- 模式识别
- 准确数据捕获

## 📞 联系我们

- 💬 微信: galaxyeye
- 🌐 微博: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter: galaxyeye8
- 🌍 网站: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="微信二维码" />
</div>
