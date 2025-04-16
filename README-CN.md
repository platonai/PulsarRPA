# PulsarRPA 简介

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🥁 简介

💖 **PulsarRPA - 您的终极 AI-RPA 解决方案！** 💖

**PulsarRPA** 是一个**高性能**、**分布式**且**开源**的机器人流程自动化（RPA）框架。
它专为**大规模自动化**而设计，在**浏览器自动化**、**网页内容理解**和**数据提取**方面表现出色。
PulsarRPA 解决了现代网页自动化的挑战，确保即使从最**复杂**和**动态**的网站中也能实现**准确**且**全面**的数据提取。


## 🎥 视频

YouTube:
[![Watch the video](https://img.youtube.com/vi/rF4wXbFlPXk/0.jpg)](https://www.youtube.com/watch?v=rF4wXbFlPXk)

Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)




## 🐳 Docker

不使用 LLM 特性：

```shell
  docker run -d -p 8182:8182 galaxyeye88/pulsar-rpa:latest
```

LLM 集成：

点击获取你的 API-key:
https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey?apikey=xxx

```shell
  docker run -d -p 8182:8182 \
    -e llm.provider=volcengine \
    -e llm.name=${YOUR-MODEL-NAME} \
    -e llm.apiKey=${YOUR-LLM_API_KEY} \
    galaxyeye88/pulsar-rpa:latest
```

## 🚀 快速入门

### 面向入门用户 - 仅使用自然语言

谈论一个网页：
```shell
  curl -X POST "http://localhost:8182/api/ai/chat-about" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "prompt": "introduce this product"
  }'
```

从要给网页提取数据：
```shell
  curl -X POST "http://localhost:8182/api/ai/extract" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "prompt": "product name, price, and description"
  }'
```

### 面向进阶用户 - 结合 LLM 和 X-SQL

```bash
  curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
    select
      llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      dom_first_slim_html(dom, 'img:expr(width > 400)') as img
    from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
  "
```

### 🚀 面向专家用户 - 使用本地 API

#### 谈论一个网页

```kotlin
val document = session.loadDocument(url)
val response = session.chat("介绍一下这个网页", document)
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt).

#### 吩咐浏览器干活

```kotlin
val prompts = """
移动光标到 id 为 'title' 的元素并点击
滚动到页面中间
滚动到顶部
获取 id 为 'title' 的元素的文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt).

#### 一行代码抓取

```kotlin
session.scrapeOutPages(
  "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

#### 结合机器人流程自动化 (RPA) 进行网页抓取

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver ->
    // warp up the browser to avoid being blocked by the website,
    // or choose the global settings, such as your location.
    warnUpBrowser(page, driver)
}
event.onWillFetch.addLast { page, driver ->
    // have to visit a referrer page before we can visit the desired page
    waitForReferrer(page, driver)
    // websites may prevent us from opening too many pages at a time, so we should open links one by one.
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page, driver ->
    // wait for a special fields to appear on the page
    driver.waitForSelector("body h1[itemprop=name]")
    // close the mask layer, it might be promotions, ads, or something else.
    driver.click(".mask-layer-close-button")
}
// visit the URL and trigger events
session.load(url, options)
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt).

#### 使用 X-SQL 解决*超级复杂*的数据提取问题

```sql
select
    llm_extract(dom, 'product name, price, ratings, score') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

Example code:

* [X-SQL to scrape 100+ fields from an Amazon's product page](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [X-SQLs to scrape all types of Amazon webpages](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)



#### 连续采集

```kotlin
fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // use the document
        // ...
        // and then extract further hyperlinks
        context.submitAll(document.selectHyperlinks("a[href~=/dp/]"))
    }
    val urls = LinkExtractors.fromResource("seeds10.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls).await()
}
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_5_ContinuousCrawler.kt), [java](/pulsar-app/pulsar-examples/src/main/java/ai/platon/pulsar/examples/ContinuousCrawler.java).



#### 进阶教程

[进阶教程](docs/advanced-guides.cn.md)










# 🚄 核心功能

- 网络爬虫：可扩展的爬取能力，支持浏览器渲染、AJAX数据提取等功能。

- LLM集成：使用自然语言分析和描述网页内容。

- 文生行为：通过简单直观的语言指令控制浏览器操作。

- RPA（机器人流程自动化）：模拟人类行为，自动化处理任务，包括单页应用（SPA）爬取及其他高价值工作流。

- 简易API：用一行代码提取数据，或用一条SQL语句将网页转换为结构化表格。

- X-SQL：扩展SQL功能，用于管理网络数据——爬取、抓取、内容挖掘和基于网页的商业智能分析。

- 爬虫隐身：高级反检测技术，包括Web驱动隐身、IP轮换和隐私上下文轮换，避免被封锁。

- 高性能：高度优化，单机可并行渲染数百个页面且不被屏蔽。

- 低成本：每天爬取10万+个浏览器渲染的电商页面或处理数千万数据点，仅需8核CPU/32GB内存。

- 数据量保障：智能重试机制、精准调度和全面的网页数据生命周期管理。

- 大规模支持：完全分布式架构，专为大规模网页爬取设计。

- 大数据支持：支持多种后端存储，包括本地文件、MongoDB、HBase和Gora。

- 日志与指标：全面监控和详细事件记录，确保完全透明。

- 自动提取：基于AI的模式识别，自动精准提取网页中的所有字段。

# 🐦 联系方式

- 微信：galaxyeye
- 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- Twitter: galaxyeye8
- 网站：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="wechat-author" />
  <img src="docs/images/buy-me-a-coffee.png" width="300" alt="buy-me-a-coffee" />
</div>

