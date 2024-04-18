# PulsarRPA 简介

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🚄 开始

💖 PulsarRPA - 您的全方位自动化解决方案！💖

PulsarRPA 是一款高性能、分布式、开源的机器人流程自动化（RPA）框架，专为轻松应对大规模 RPA 任务而设计，为浏览器自动化、网络内容理解和数据提取提供了全面解决方案。

作为面向大规模网络数据抽取领域的顶级开源解决方案，PulsarRPA 结合了高性能、分布式 RPA 的优势，旨在解决在快速演变且日益复杂的网站环境中进行浏览器自动化以及抽取准确、全面网络数据所固有的挑战。

### 大规模网页数据提取面临的挑战

1. **网页内容智能提取**：互联网拥有数十亿个网站，每个网站都包含海量数据。为了从如此众多的网站中提取信息，智能化的网页内容采集技术至关重要。传统的网页抓取方法无法有效处理大量网页，导致数据提取效率低下。
2. **频繁的网站变更**：在线平台不断更新其布局、结构和内容，使得长期保持可靠的提取流程颇具挑战。传统的抓取工具难以迅速适应这些变化，导致获取到的数据过时或不再相关。
3. **复杂的网站架构**：现代网站常采用精巧的设计模式、动态内容加载及先进的安全措施，为常规抓取方法设立了严峻的难关。从这类网站中提取数据需深入理解其结构与行为，并具备像人类用户一样与其交互的能力。

### PulsarRPA：革新网页数据采集方式

为应对上述挑战，PulsarRPA 集成了多项创新技术，确保高效、精准、可扩展的网页数据提取：

1. **浏览器渲染**：利用浏览器渲染和AJAX数据抓取从网站提取内容。
2. **RPA（机器人流程自动化）**：采用类人类行为与网页互动，实现从现代复杂网站中收集数据。
3. **智能抓取**：PulsarRPA采用智能抓取技术，能够自动识别并理解网页内容，从而确保数据提取的准确性和及时性。利用智能算法和机器学习技术，PulsarRPA能够自主学习和应用数据提取模型，显著提高数据检索的效率和精确度。
4. **高级DOM解析**：利用高级文档对象模型（DOM）解析技术，PulsarRPA能够轻松导航复杂的网站结构。它能准确识别并提取现代网页元素中的数据，处理动态内容渲染，绕过反爬虫措施，即使面对网站的复杂性，也能提供完整准确的数据集。
5. **分布式架构**：基于分布式架构构建的PulsarRPA，能够有效地处理大规模提取任务，因为它利用了多个节点组合的计算能力。这使得并行抓取、快速数据检索成为可能，并随着数据需求的增加实现无缝扩展，同时不损害性能或可靠性。
6. **开源与可定制**：作为一个开源解决方案，PulsarRPA提供了无与伦比的灵活性和可扩展性。开发者可以轻松定制其组件、集成现有系统或贡献新功能以满足特定项目需求。

综上所述，PulsarRPA 凭借其网页内容理解、智能抓取、先进 DOM 解析、分布式处理及开源特性，成为大规模网页数据提取首选的开源解决方案。其独特的技术组合使用户能够有效应对与大规模提取宝贵网页数据相关的复杂性和挑战，最终推动更明智的决策制定和竞争优势。

### 大多数抓取尝试可以从几乎一行代码开始：

*Kotlin:*

```kotlin
fun main() = PulsarContexts.createSession().scrapeOutPages(
  "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

上面的代码从一组产品页面中抓取由 css 选择器 #title 和 #acrCustomerReviewText 指定的字段。 示例代码：[kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/topEc/english/amazon/AmazonCrawler.kt), [java](/pulsar-app/pulsar-examples/src/main/java/ai/platon/pulsar/examples/sites/amazon/AmazonCrawler.java).

### 大多数 *生产环境* 数据采集项目可以从以下代码片段开始：

*Kotlin:*

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

示例代码：
[kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_5_ContinuousCrawler.kt), [java](/pulsar-app/pulsar-examples/src/main/java/ai/platon/pulsar/examples/ContinuousCrawler.java).

### **最复杂** 的数据采集项目需要使用 RPA：

*Kotlin:*

```kotlin
val options = session.options(args)
val event = options.event.browseEventHandlers
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

示例代码: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt).

### *大批量*站点的数据提取可以使用自动化提取技术:

```kotlin
val document = session.harvest("https://www.eeo.com.cn/2024/0330/648712.shtml")

println(document.contentTitle)
println(document.textContent)
```

示例代码: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/article/EEO.kt).

在这个案例中，我们使用流行的 biolerpipe 技术来自动提取新闻网页。在 PulsarRPAPro 中，我们支持更加智能的数据提取技术，来自动提取各种各样的网页，譬如商品详情页。

### *最复杂* 的 Web 数据抽取难题需要用 X-SQL 来解决:

1. 您的 Web 数据提取规则非常复杂，例如，每个单独的页面有 100 多个规则
2. 需要维护的数据提取规则很多，比如全球 20 多个亚马逊网站，每个网站 20 多个数据类型

```sql
select
      dom_first_text(dom, '#productTitle') as title,
      dom_first_text(dom, '#bylineInfo') as brand,
      dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
      dom_first_text(dom, '#acrCustomerReviewText') as ratings,
      str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

示例代码:
- [X-SQL  to scrape 100+ fields from an Amazon's product page](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
- [X-SQLs  to scrape all types of Amazon webpages](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

## 🥁 简介

PulsarRPA 是大规模采集 Web 数据的终极开源方案，可满足几乎所有规模和性质的网络数据采集需要。

大规模提取 Web 数据非常困难。网站经常变化并且变得越来越复杂，这意味着收集的网络数据通常不准确或不完整，PulsarRPA 开发了一系列尖端技术来解决这些问题。

我们发布了一些最大型电商网站的全站数据采集的完整解决方案，这些解决方案满足最高标准的性能、质量和成本要求，他们将永久免费并开放源代码，例如：
- [Exotic Amazon](https://github.com/platonai/exotic-amazon)
- [Exotic Walmart](https://github.com/platonai/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/walmart)
- [Exotic Dianping](https://github.com/platonai/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/food/dianping)











# 🚀 主要特性

- 网络爬虫：支持多种数据采集模式，包括浏览器渲染、ajax数据采集、普通协议采集等。
- RPA：机器人流程自动化，能够模仿人类行为，采集单网页应用程序或执行其他有价值的任务。
- 简洁的 API：通过一行代码抓取数据，或者使用一条 SQL 语句将整个网站栏目转换成表格。
- X-SQL：扩展 SQL 功能来管理 Web 数据，包括网络爬取、数据采集、Web 内容挖掘和 Web BI。
- 爬虫隐身：通过浏览器驱动隐身技术，IP 轮换和隐私上下文轮换，确保爬虫不会被屏蔽。
- 高性能：经过高度优化，能够在单机上并行渲染数百页而不被屏蔽。
- 低成本：每天能够抓取 100,000 个浏览器渲染的电子商务网页，或者 n * 10,000,000 个数据点，仅需 8 核 CPU 和 32G 内存。
- 数据质量保证：通过智能重试、精准调度和 Web 数据生命周期管理来确保数据质量。
- 大规模采集：采用完全分布式架构，专为大规模数据采集而设计。
- 大数据支持：支持多种后端存储，包括本地文件、MongoDB、HBase 和 Gora。
- 日志和指标：密切监控并记录系统中的每个事件。

# ♾ 核心概念

PulsarRPA 的核心概念包括以下内容，了解这些概念后，您可以利用 PulsarRPA 解决各种高要求的数据采集任务：

- 网络数据采集（Web Scraping）：使用机器人自动从网站中提取内容和数据。
- 自动提取（Auto Extract）：通过自动学习数据模式并从网页中提取每个字段，由先进的人工智能算法驱动。
- RPA：机器人流程自动化，是抓取现代网页的有效方法。
- 网络即数据库（Network As A Database）：像访问本地数据库一样访问网络资源。
- X-SQL：使用 SQL 语言直接查询 Web 数据。
- Pulsar Session：提供一组简单、强大和灵活的 API 来执行 Web 抓取任务。
- Web Driver：定义了一个简洁的接口来访问和交互网页，所有行为都经过优化以尽可能接近真实人类的行为。
- URL：在 PulsarRPA 中，URL 不仅是普通的网址，还包含了描述任务的额外信息。PulsarRPA 中的每个任务都被定义为某种形式的 URL。
- Hyperlink：在 PulsarRPA 中，超链接不仅是普通的链接，还包含了描述任务的额外信息。
- Load Options：加载选项或加载参数会影响 PulsarRPA 如何加载、获取和抓取网页。
- Event Handlers：在网页的整个生命周期中捕获和处理事件。

# 🧮 通过可执行 jar 使用 PulsarRPA

我们发布了一个基于 PulsarRPA 的独立可执行 jar，它包括：

- 顶尖站点的数据采集示例。
- 基于自监督机器学习自动进行信息提取的小程序，AI 算法可以识别详情页的所有字段，字段精确度达到 99% 以上。
- 基于自监督机器学习自动学习并输出所有采集规则的小程序。
- 可以直接从命令行执行网页数据采集任务，无需编写代码。
- PulsarRPA 服务器，可以向服务器发送 SQL 语句来采集 Web 数据。
- 一个 Web UI，可以编写 SQL 语句并通过它发送到服务器。

下载 [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro#download) 并使用以下命令行探索其能力：

```shell
java -jar exotic-standalone.jar
```

# 🎁 将 PulsarRPA 用作软件库

要利用 PulsarRPA 的强大功能，最简单的方法是将其作为库添加到您的项目中。

使用 Maven 时，可以在 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
  <groupId>ai.platon.pulsar</groupId>
  <artifactId>pulsar-all</artifactId>
  <version>1.12.4</version>
</dependency>
```

使用 Gradle 时，可以在 `build.gradle` 文件中添加以下依赖：

```kotlin
implementation("ai.platon.pulsar:pulsar-all:1.12.4")
```

也可以从 Github 克隆模板项目，包括 [kotlin](https://github.com/platonai/pulsar-kotlin-template), [java-11](https://github.com/platonai/pulsar-java-template), [java-17](https://github.com/platonai/pulsar-java-17-template)。







# 基本用法

## Kotlin:

```kotlin
// 创建一个 Pulsar 会话
val session = PulsarContexts.createSession()
// 我们正在处理的主要 URL
val url = "https://www.amazon.com/dp/B0C1H26C46" 

// 从本地存储加载页面，或者如果不存在或已过期，则从互联网获取
val page = session.load(url, "-expires 10s")

// 将 URL 提交到 URL 池，提交的 URL 将在爬取循环中处理
session.submit(url, "-expires 10s")

// 解析页面内容为文档
val document = session.parse(page)
// 对文档进行操作
// ...

// 加载并解析
val document2 = session.loadDocument(url, "-expires 10s")
// 对文档进行操作
// ...

// 加载门户页面，然后加载由 `-outLink` 指定的所有链接。
// 选项 `-outLink` 指定了在门户页面中选择链接以加载的 css 选择器。
// 选项 `-topLinks` 指定了由 `-outLink` 选择的最大链接数。
val pages = session.loadOutPages(url, "-expires 10s -itemExpires 10s -outLink a[href~=/dp/] -topLinks 10")

// 加载门户页面并将由 `-outLink` 指定的外部链接提交到 URL 池。
// 选项 `-outLink` 指定了在门户页面中选择链接以提交的 css 选择器。
// 选项 `-topLinks` 指定了由 `-outLink` 选择的最大链接数。
session.submitOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=/dp/] -topLinks 10")

// 加载、解析并抓取字段
val fields = session.scrape(url, "-expires 10s", "#centerCol",
    listOf("#title", "#acrCustomerReviewText"))

// 加载、解析并抓具名字段
val fields2 = session.scrape(url, "-i 10s", "#centerCol",
    mapOf("title" to "#title", "reviews" to "#acrCustomerReviewText"))

// 加载、解析并抓具名字段
val fields3 = session.scrapeOutPages(url, "-i 10s -ii 10s -outLink a[href~=/dp/] -topLink 10", "#centerCol",
    mapOf("title" to "#title", "reviews" to "#acrCustomerReviewText"))

// 添加 `-parse` 选项以激活解析子系统（激活后可以注册 DOM 文档相关的事件处理器，并进一步处理 DOM 文档）
val page10 = session.load(url, "-parse -expires 10s")

// Kotlin 挂起调用
val page11 = runBlocking { session.loadDeferred(url, "-expires 10s") }

// Java 风格的异步调用
session.loadAsync(url, "-expires 10s").thenApply(session::parse).thenAccept(session::export)
```

示例代码: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_0_BasicUsage.kt), [java](/pulsar-app/pulsar-examples/src/main/java/ai/platon/pulsar/examples/BasicUsage.java).

## 加载选项

请注意，我们的大多数抓取方法都接受一个称为加载参数或加载选项的参数，以控制如何加载/获取网页。

- `-expires`     // 页面的过期时间
- `-itemExpires` // 批量抓取方法中项目页面的过期时间
- `-outLink`     // 要抓取的外部链接的选择器
- `-refresh`     // 强制（重新）获取页面，就像在真实浏览器中点击刷新按钮一样
- `-parse`       // 激活解析子系统，激活后可以注册 DOM 文档相关的事件处理器，并进一步处理 DOM 文档
- `-resource`    // 无需浏览器渲染获取 URL 作为资源

点击 [Load Options](/docs/concepts.md#load-options) 查看详情。

# 提取网页数据

PulsarRPA 使用 [jsoup](https://jsoup.org/) 从 HTML 文档中提取数据。 Jsoup 将 HTML 解析为与现代浏览器相同的 DOM。 
查看 [selector-syntax](https://jsoup.org/cookbook/extracting-data/selector-syntax) 以获取所有受支持的 CSS 选择器。
同时，我们扩展了标准 CSS 语法，增强了 CSS 选择器的能力，使得其能够轻松处理现代网页布局。

## Kotlin:

```kotlin
val document = session.loadDocument(url, "-expires 1d")
val price = document.selectFirst('.price').text()
```

# 连续采集

在 PulsarRPA 中抓取大量 URL 集合或运行连续采集非常简单。

## Kotlin:

```kotlin
fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // 对文档进行操作
        System.out.println(document.getTitle() + "\t|\t" + document.getBaseUri())
    }
    val urls = LinkExtractors.fromResource("seeds.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls)
    // 可以在这里提交数百万的 URL
    context.submitAll(urls)
    // ...
    context.await()
}
```

## Java:

```java
public class ContinuousCrawler {

    private static void onParse(WebPage page, FeaturedDocument document) {
        // 对文档进行操作
        System.out.println(document.getTitle() + "\t|\t" + document.getBaseUri());
    }

    public static void main(String[] args) {
        PulsarContext context = PulsarContexts.create();

        List<Hyperlink> urls = LinkExtractors.fromResource("seeds.txt")
                .stream()
                .map(seed -> new ParsableHyperlink(seed, ContinuousCrawler::onParse))
                .collect(Collectors.toList());
        context.submitAll(urls);
        // 可以在这里提交数百万的 URL
        context.submitAll(urls);
        // ...
        context.await();
    }
}
```

示例代码: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_9_MassiveCrawler.kt), [java](/pulsar-app/pulsar-examples/src/main/java/ai/platon/pulsar/examples/ContinuousCrawler.java).










# 👽 RPA (机器人流程自动化)

随着网站变得越来越复杂，RPA 已成为从某些网站收集数据的唯一途径，例如某些使用自定义字体技术的网站。

PulsarRPA 包含一个 RPA 子系统，提供了一种在网页生命周期中模仿真人的便捷方式，使用 Web 驱动程序与网页交互：滚动、打字、屏幕捕获、鼠标拖放、点击等。这和大家所熟知的 selenium，playwright，puppeteer 类似，不同的是，PulsarRPA 的所有行为都针对大规模数据采集进行优化。

以下是一个典型的 RPA 代码片段，它是从顶级电子商务网站收集数据所必需的：

```kotlin
val options = session.options(args)
val event = options.event.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver ->
    // 预热浏览器，以避免被网站阻止，或选择全局设置，例如您的位置
    warnUpBrowser(page, driver)
}
event.onWillFetch.addLast { page, driver ->
    // 必须先访问引荐来源页面，然后才能访问所需页面
    waitForReferrer(page, driver)
    // 网站可能会阻止我们同时打开过多页面，因此我们应该逐一打开链接
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page, driver ->
    // 等待特殊字段出现在页面上
    driver.waitForSelector("body h1[itemprop=name]")
    // 关闭遮罩层，它可能是促销、广告或其他东西
    driver.click(".mask-layer-close-button")
}
// 访问 URL 并触发事件
session.load(url, options)
```

示例代码可以在 [这里](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt) 找到。

## 使用 X-SQL 查询 Web

提取单个页面：

```sql
select
      dom_first_text(dom, '#productTitle') as title,
      dom_first_text(dom, '#bylineInfo') as brand,
      dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
      dom_first_text(dom, '#acrCustomerReviewText') as ratings,
      str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

执行 X-SQL：

```kotlin
val context = SQLContexts.create()
val rs = context.executeQuery(sql)
println(ResultSetFormatter(rs, withHeader = true))
```

结果如下：

```
TITLE                                                   | BRAND                  | PRICE   | RATINGS       | SCORE
HUAWEI P20 Lite (32GB + 4GB RAM) 5.84" FHD+ Display ... | Visit the HUAWEI Store | $6.10 | 1,349 ratings | 4.40
```

示例代码可以在 [这里](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_10_XSQL.kt) 找到。

# 🌐 将 PulsarRPA 作为 REST 服务运行

当 PulsarRPA 作为 REST 服务运行时，X-SQL 可用于随时随地抓取网页或直接查询 Web 数据，无需打开 IDE。

## 从源代码构建

```
git clone https://github.com/platonai/pulsar.git 
cd pulsar && bin/build-run.sh
```

对于国内开发者，我们强烈建议您按照 [这个](https://github.com/platonai/pulsar/blob/master/bin/tools/maven/maven-settings.md) 指导来加速构建。

## 使用 X-SQL 查询 Web

如果未启动，则启动 pulsar 服务器：

```
bin/pulsar
```

在另一个终端窗口中抓取网页：

```
bin/scrape.sh
```

该 bash 脚本非常简单，只需使用 curl 发送 X-SQL：

```
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as category,
      dom_first_slim_html(dom, '#bylineInfo') as brand,
      cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
      dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
      dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
      dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
      str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1d -njr 3', 'body');
```

示例代码可以在 [这里](https://github.com/platonai/pulsar/blob/master/bin/scrape.sh) 找到。

Json 格式的响应如下：

```json
{
    "uuid": "cc611841-1f2b-4b6b-bcdd-ce822d97a2ad",
    "statusCode": 200,
    "pageStatusCode": 200,
    "pageContentBytes": 1607636,
    "resultSet": [
        {
            "title": "Tara Toys Ariel Necklace Activity Set - Amazon Exclusive (51394)",
            "listprice": "$19.99",
            "price": "$12.99",
            "categories": "Toys & Games|Arts & Crafts|Craft Kits|Jewelry",
            "baseuri": "https://www.amazon.com/dp/B0C1H26C46"
        }
    ],
    "pageStatus": "OK",
    "status": "OK"
}
```







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
2022-09-24 11:46:09.190  INFO [-worker-32] a.p.p.c.c.L.Task - 3738. 💯 💿 U  got 200 452.91 KiB in 55.286s, last fetched 9h32m50s ago, fc:1 | 49/171/82/238/6172 | 121.205.220.179 | https://www.walmart.com/ip/Boost-Mobile-Apple-iPhone-SE-2-Cell-Phone-Black-64GB-Prepaid-Smartphone/490934488 -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
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
- 网站：[platon.ai](http://platon.ai)
