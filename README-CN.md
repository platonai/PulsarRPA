# PulsarRPA 简介

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🥁 简介

💖 **PulsarRPA - 您的终极 AI-RPA 解决方案！** 💖

**PulsarRPA** 是一个**高性能**、**分布式**且**开源**的机器人流程自动化（RPA）框架。
它专为**大规模自动化**而设计，在**浏览器自动化**、**网页内容理解**和**数据提取**方面表现出色。
PulsarRPA 解决了现代网页自动化的挑战，确保即使从最**复杂**和**动态**的网站中也能实现**准确**且**全面**的数据提取。


## 视频

YouTube:
[![Watch the video](https://img.youtube.com/vi/rF4wXbFlPXk/0.jpg)](https://www.youtube.com/watch?v=rF4wXbFlPXk)

Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

## 🚀 开始

### 谈论一个网页

```kotlin
val document = session.loadDocument(url)
val response = session.chat("介绍一下这个网页", document)
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt).

### 吩咐浏览器干活

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

### 一行代码抓取

```kotlin
session.scrapeOutPages(
  "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

### 结合机器人流程自动化 (RPA) 进行网页抓取

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

### 使用 X-SQL 解决*超级复杂*的数据提取问题

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



### 连续采集

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

# 🧮 通过可执行 jar 使用 PulsarRPA

我们发布了一个基于 PulsarRPA 的独立可执行 jar，它包括：

- 顶尖站点的数据采集示例。
- 基于自监督机器学习自动进行信息提取的小程序，AI 算法可以识别详情页的所有字段，字段精确度达到 99% 以上。
- 基于自监督机器学习自动学习并输出所有采集规则的小程序。
- 可以直接从命令行执行网页数据采集任务，无需编写代码。
- 升级的 PulsarRPA 服务器，可以向服务器发送 SQL 语句来采集 Web 数据。
- 一个 Web UI，可以编写 SQL 语句并通过它发送到服务器。

下载 [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro#download) 并使用以下命令行探索其能力：

```shell
java -jar PulsarRPAPro.jar
```

# 🎁 将 PulsarRPA 用作软件库

要利用 PulsarRPA 的强大功能，最简单的方法是将其作为库添加到您的项目中。

使用 Maven 时，可以在 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-bom</artifactId>
    <version>2.3.0-SNAPSHOT</version>
</dependency>
```

使用 Gradle 时，可以在 `build.gradle` 文件中添加以下依赖：

```kotlin
implementation("ai.platon.pulsar:pulsar-bom:2.3.0-SNAPSHOT")
```

也可以从 Github 克隆模板项目，包括 [kotlin](https://github.com/platonai/pulsar-kotlin-template),
[java-17](https://github.com/platonai/pulsar-java-17-template)。

您还可以基于我们的商业级开源项目启动自己的大规模网络爬虫项目: [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro), [Exotic-amazon](https://github.com/platonai/exotic-amazon)。

点击 [基本用法](docs/get-started/2basic-usage.md) 查看详情。

# 🌐 将 PulsarRPA 作为 REST 服务运行

当 PulsarRPA 作为 REST 服务运行时，X-SQL 可用于随时随地抓取网页或直接查询 Web 数据，无需打开 IDE。

## 从源代码构建

```shell
git clone https://github.com/platonai/PulsarRPA.git 
cd PulsarRPA && bin/build-run.sh
```

对于国内开发者，我们强烈建议您按照 [这个](https://github.com/platonai/pulsar/blob/master/bin/tools/maven/maven-settings.md) 指导来加速构建。

## 使用 X-SQL 查询 Web

如果未启动，则启动 pulsar 服务器：

```shell
bin/pulsar
```

在另一个终端窗口中抓取网页：

```shell
bin/scrape.sh
```

该 bash 脚本非常简单，只需使用 curl 发送 X-SQL：

```shell
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

示例代码: [bash](bin/scrape.sh), [PowerShell](bin/scrape.ps1), [batch](bin/scrape.bat), [java](/pulsar-client/src/main/java/ai/platon/pulsar/client/Scraper.java), [kotlin](/pulsar-client/src/main/kotlin/ai/platon/pulsar/client/Scraper.kt), [php](/pulsar-client/src/main/php/Scraper.php).

点击 [X-SQL](docs/x-sql.md) 查看有关X-SQL的详细介绍和功能描述。






# 📖 循序渐进的课程

我们提供了一个循序渐进的示例课程，帮助您逐步了解和掌握 PulsarRPA 的使用：

1. [Home](docs/zh/get-started/1home.md)
2. [Basic Usage](docs/zh/get-started/2basic-usage.md)
3. [Load Options](docs/zh/get-started/3load-options.md)
4. [Data Extraction](docs/zh/get-started/4data-extraction.md)
5. [URL](docs/zh/get-started/5URL.md)
6. [Java-style Async](docs/zh/get-started/6Java-style-async.md)
7. [Kotlin-style Async](docs/zh/get-started/7Kotlin-style-async.md)
8. [Continuous Crawling](docs/zh/get-started/8continuous-crawling.md)
9. [Event Handling](docs/zh/get-started/9event-handling.md)
10. [RPA](docs/zh/get-started/10RPA.md)
11. [WebDriver](docs/zh/get-started/11WebDriver.md)
12. [Massive Crawling](docs/zh/get-started/12massive-crawling.md)
13. [X-SQL](docs/zh/get-started/13X-SQL.md)
14. [AI Extraction](docs/zh/get-started/14AI-extraction.md)
15. [REST](docs/zh/get-started/15REST.md)
16. [Console](docs/zh/get-started/16console.md)
17. [Top Practice](docs/zh/get-started/17top-practice.md)
18. [Miscellaneous](docs/zh/get-started/18miscellaneous.md)

# 📊 日志和指标

PulsarRPA 精心设计了日志和指标子系统，以记录系统中发生的每一个事件。通过 PulsarRPA 的日志系统，您可以轻松地了解系统中发生的每一件事情，
判断系统运行是否健康，以及成功获取了多少页面、重试了多少页面、使用了多少代理 IP 等信息。

通过观察几个简单的符号，您可以快速了解整个系统的状态：💯 💔 🗙 ⚡ 💿 🔃 🤺。以下是一组典型的页面加载日志。要了解如何阅读日志，
请查看 [日志格式](docs/log-format.md)，以便快速掌握整个系统的状态。

```text
2022-09-24 11:46:26.045  INFO [-worker-14] a.p.p.c.c.L.Task - 3313. 💯 ⚡ U for N got 200 580.92 KiB in 1m14.277s, fc:1 | 75/284/96/277/6554 | 106.32.12.75 | 3xBpaR2 | https://www.walmart.com/ip/Restored-iPhone-7-32GB-Black-T-Mobile-Refurbished/329207863 -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:09.190  INFO [-worker-32] a.p.p.c.c.L.Task - 3738. 💯 💿 U  got 200 452.91 KiB in 55.286s, last fetched 9h32m50s ago, fc:1 | 49/171/82/238/6172 | 121.205.2.0.6 | https://www.walmart.com/ip/Boost-Mobile-Apple-iPhone-SE-2-Cell-Phone-Black-64GB-Prepaid-Smartphone/490934488 -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:28.567  INFO [-worker-17] a.p.p.c.c.L.Task - 2269. 💯 🔃 U for SC got 200 565.07 KiB <- 543.41 KiB in 1m22.767s, last fetched 16m58s ago, fc:6 | 58/230/98/295/6272 | 27.158.125.76 | 9uwu602 | https://www.walmart.com/ip/Straight-Talk-Apple-iPhone-11-64GB-Purple-Prepaid-Smartphone/356345388?variantFieldId=actual_color -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:18.390  INFO [r-worker-8] a.p.p.c.c.L.Task - 3732. 💔 ⚡ U for N got 1601 0 <- 0 in 32.201s, fc:1/1 Retry(1601) rsp: CRAWL, rrs: EMPTY_0B | 2zYxg52 | https://www.walmart.com/ip/Apple-iPhone-7-256GB-Jet-Black-AT-T-Locked-Smartphone-Grade-B-Used/182353175?variantFieldId=actual_color -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:13.860  INFO [-worker-60] a.p.p.c.c.L.Task - 2828. 🗙 🗙 U for SC got 200 0 <- 348.31 KiB <- 684.75 KiB in 0s, last fetched 18m55s ago, fc:2 | 34/130/52/181/5747 | 60.184.124.232 | 11zTa0r2 | https://www.walmart.com/ip/Walmart-Family-Mobile-Apple-iPhone-11-64GB-Black-Prepaid-Smartphone/209201965?athbdg=L1200 -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
```

# 💻 系统要求

- 内存 4G+
- Maven 3.2+
- Java 11 JDK 最新版本
- java 和 jar 必须在 PATH 中
- Google Chrome 90+

PulsarRPA 已在 Ubuntu 18.04、Ubuntu 20.04、Windows 7、Windows 11、WSL 上进行了测试，任何其他满足要求的操作系统也应该可以正常工作。

# 🛸 高级主题

如果您对 PulsarRPA 的高级主题感兴趣，可以查看 [advanced topics](/docs/faq/advanced-topics.md) 以获取以下问题的答案：

- 大规模网络爬虫有什么困难？
- 如何每天从电子商务网站上抓取一百万个产品页面？
- 如何在登录后抓取页面？
- 如何在浏览器上下文中直接下载资源？
- 如何抓取单页应用程序（SPA）？
- 资源模式
- RPA 模式
- 如何确保正确提取所有字段？
- 如何抓取分页链接？
- 如何抓取新发现的链接？
- 如何爬取整个网站？
- 如何模拟人类行为？
- 如何安排优先任务？
- 如何在固定时间点开始任务？
- 如何删除计划任务？
- 如何知道任务的状态？
- 如何知道系统中发生了什么？
- 如何为要抓取的字段自动生成 css 选择器？
- 如何使用机器学习自动从网站中提取内容并具有商业准确性？
- 如何抓取 amazon.com 以满足行业需求？

# 🆚 同其他方案的对比

PulsarRPA 在 “主要特性” 部分中提到的特性都得到了良好的支持，而其他解决方案可能不支持或者支持不好。您可以点击 [solution comparison](docs/faq/solution-comparison.md) 查看以下问题的答案：

- PulsarRPA vs selenium/puppeteer/playwright
- PulsarRPA vs nutch
- PulsarRPA vs scrapy+splash

# 🤓 技术细节

如果您对 PulsarRPA 的技术细节感兴趣，可以查看 [technical details](docs/faq/technical-details.md) 以获取以下问题的答案：

- 如何轮换我的 IP 地址？
- 如何隐藏我的机器人不被检测到？
- 如何以及为什么要模拟人类行为？
- 如何在一台机器上渲染尽可能多的页面而不被屏蔽？

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

