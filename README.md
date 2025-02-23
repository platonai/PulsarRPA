# PulsarRPA

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🚄 Get started

💖 **PulsarRPA is All You Need!** 💖

PulsarRPA is a high-performance, distributed, open-source Robotic Process Automation (RPA) framework. It is designed to handle large-scale RPA tasks with ease, providing a comprehensive solution for browser automation, web content understanding, and data extraction. It addresses the inherent challenges of browser automation and extracting accurate, comprehensive web data amidst rapidly evolving and increasingly intricate websites.

## Videos

YouTube:
[![Watch the video](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

### Most scraping attempts can start with (almost) a single line of code:

```kotlin
fun main() = PulsarContexts.createSession().scrapeOutPages(
  "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

The code above scrapes fields specified by CSS selectors #title and #acrCustomerReviewText from a set of product pages.

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/topEc/english/amazon/AmazonCrawler.kt).

### The majority of *real-world* web crawling projects can initiate with the following code template:

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

### The *most complicated* web crawling challenges can be overcome with cutting-edge robotic process automation (RPA) technologies:

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

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt).

### The *most complicated* Web data extraction problems can be effectively managed using X-SQL technology:

```sql
select
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

# 🥁 Introduction

Extracting Web data at scale is extremely hard. Websites change frequently and are becoming more complex, meaning web data collected is often inaccurate or incomplete. PulsarRPA has developed a range of cutting-edge technologies to solve this problem.

### Challenges in Large-Scale Web Data Extraction

1. **Frequent Website Changes**: Online platforms continuously update their layouts, structures, and content, making it difficult to maintain reliable extraction processes over time. Traditional scraping tools may struggle to adapt promptly to these changes, leading to outdated or irrelevant data.
2. **Intelligent Extraction of Web Content**: The internet hosts billions of websites, each containing vast amounts of data. To extract information from such a diverse range of websites and keep up with frequent changes, intelligent web content extraction techniques are crucial. Traditional web scraping methods fail to effectively handle large volumes of webpages, leading to inefficient data extraction.
3. **Complex Website Architecture**: Modern websites often employ sophisticated design patterns, dynamic content loading, and advanced security measures, presenting formidable obstacles for conventional scraping techniques. Extracting data from such sites requires deep understanding of their structure and behavior, as well as the ability to interact with them as a human user would.

### PulsarRPA: A Comprehensive Solution for Web Data Collection

To conquer these challenges, PulsarRPA incorporates a suite of innovative technologies that ensure efficient, accurate, and scalable web data extraction:

1. **Browser Rendering:** Utilizes browser rendering and AJAX data crawling to extract content from websites.
2. **RPA (Robotic Process Automation):** Employs human-like behaviors to interact with webpages, enabling data collection from modern, complex websites.
3. **Intelligent Scraping:** PulsarRPA employs intelligent scraping technology that can automatically recognize and understand web content, ensuring accurate and timely data extraction. Utilizing smart algorithms and machine learning techniques, PulsarRPA can independently learn and apply data extraction models, significantly improving the efficiency and accuracy of data retrieval.
4. **Advanced DOM Parsing:** Leveraging advanced Document Object Model (DOM) parsing techniques, PulsarRPA can navigate complex website architectures with ease. It accurately identifies and extracts data from elements in modern web pages, handles dynamic content rendering, and bypasses anti-scraping measures, delivering complete and accurate datasets despite website intricacies.
5. **Distributed Architecture:** Built on a distributed architecture, PulsarRPA harnesses the combined processing power of multiple nodes to handle large-scale extraction tasks efficiently. This allows for parallel crawling, faster data retrieval, and seamless scalability as your data requirements grow, without compromising performance or reliability.
6. **Open-Source & Customizable:** As an open-source solution, PulsarRPA offers unparalleled flexibility and extensibility. Developers can easily customize its components, integrate with existing systems, or contribute new features to meet specific project requirements.

In summary, PulsarRPA, with its web content understanding, intelligent scraping, advanced DOM parsing, distributed processing, and open-source features, becomes the preferred open-source solution for large-scale web data extraction. Its unique technology combination allows users to effectively address the complexities and challenges associated with extracting valuable web data on a large scale, ultimately facilitating wiser decision-making and competitive advantage.

We have released complete solutions for site-wide Web scraping for some of the largest e-commerce websites. These solutions meet the highest standards of performance, quality, and cost. They will be free and open source forever, such as:

* [Exotic Amazon](https://github.com/platonai/exotic-amazon)
* [Exotic Walmart](https://github.com/platonai/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/walmart)

🕷 PulsarRPA supports high-quality, large-scale Web data collection and processing. We have developed a range of infrastructure and cutting-edge technologies to ensure the highest standards of performance, quality, and TCO (total cost of ownership), even in very large-scale data collection scenarios.

🏪 PulsarRPA supports the Network-As-A-Database paradigm. PulsarRPA treats the external network as a database. If the required data is not in the local storage, or the existing version does not meet the analysis needs, the system will collect the latest version of the data from the Internet. We also developed X-SQL to query the Web directly and convert webpages into tables and charts.

🌈 PulsarRPA supports browser rendering as the primary method to collect Web data. By using browser rendering as the primary method to collect Web data, we achieve an optimal balance between data point scale, data quality, labor cost, and hardware cost, and achieve the lowest TCO (total cost of ownership). With optimizations such as blocking unnecessary resource files, the performance of browser rendering can even be comparable to the traditional single resource collection method.

💫 PulsarRPA supports RPA based Web scraping. PulsarRPA includes an RPA subsystem for Web interaction: scrolling, typing, screen capture, dragging and dropping, clicking, etc. This subsystem is similar to the well-known selenium, playwright, puppeteer, but all behaviors are optimized, such as more realistic simulation, better execution performance, better parallelism, better fault tolerance, and so on.

🔪 PulsarRPA supports single resource collection. PulsarRPA's default data collection method is to harvest the `complete` Web data through browser rendering, but if the data you need can be retrieved through a single link, for example, it can be returned through an ajax interface, you can also call PulsarRPA's resource collection method for super High-speed collection.

💯 PulsarRPA plans to support cutting-edge information extraction technology. We plan to release an advanced AI to automatically extract every field from all valuable webpages (e.g., product detail pages) with remarkable accuracy, and we currently offer a [preview](https://github.com/platonai/PulsarRPAPro#run-auto-extract) version.

## 🚀 Features

* Web spider: browser rendering, ajax data crawling
* RPA: robotic process automation, mimic human behaviors, SPA crawling, or do something else valuable
* Simple API: single line of code to scrape, or single SQL to turn a website into a table
* X-SQL: extended SQL to manage web data: Web crawling, scraping, Web content mining, Web BI
* Bot stealth: web driver stealth, IP rotation, privacy context rotation, never get banned
* High performance: highly optimized, rendering hundreds of pages in parallel on a single machine without being blocked
* Low cost: scraping 100,000 browser-rendered e-comm webpages, or n * 10,000,000 data points each day, only 8 core CPU/32G memory are required
* Data quantity assurance: smart retry, accurate scheduling, web data lifecycle management
* Large scale: fully distributed, designed for large scale crawling
* Big data: various backend storage support: Local File/MongoDB/HBase/Gora
* Logs & metrics: monitored closely and every event is recorded
* [Preview] Information Extraction: Learns Web data patterns and automatically extracts every field in a webpage with remarkable precision

## ♾ Core concepts

To unlock the full potential of PulsarRPA and tackle even the most complex data scraping tasks, a solid understanding of its core concepts is essential. By grasping these fundamental principles, you'll be equipped to wield PulsarRPA as a powerful tool for extracting valuable information from the web.
Let's delve into the key concepts that form the foundation of your data scraping journey with PulsarRPA:

* Web Scraping: the process of using bots to extract content and data from a website
* Auto Extract: learn the data schema automatically and extract every field from webpages, powered by cutting-edge AI algorithm
* RPA: stands for robotic process automation which is the only way to scrape modern webpages
* Network As A Database: access the network just like a database
* X-SQL: query the Web using SQL directly
* Pulsar Session: provides a set of simple, powerful, and flexible APIs to do web scraping tasks
* Web Driver: defines a concise interface to visit and interact with webpages, all behaviors are optimized to mimic real people as closely as possible
* UrlAware: This concept encompasses URLs and additional information describing a task. In PulsarRPA, every task is defined as a type of UrlAware, with the main types being: PlainUrl, HyperLink, ListenableHyperlink, and ParsableHyperlink.
* Load Options: also known as `load arguments`. These control parameters fine-tune how PulsarRPA loads and scrapes webpages, allowing you to tailor the process to specific requirements.
* Event Handlers: capture and process various events that occur throughout the entire lifecycle of webpage scraping

Check [PulsarRPA concepts](docs/concepts.md) for details.

## 🧮 PulsarRPA as an executable jar

We have released a standalone executable jar based on PulsarRPA, which includes:

* Web scraping examples of a set of top sites
* An applet based on `self-supervised` machine learning for information extraction, AI identifies all fields on the detail page with over 90% field accuracy of 99.9% or more
* An applet based on `self-supervised` machine learning and outputs all extract rules, which can help traditional Web scraping methods
* An applet that scrape Web data directly from the command line, like wget or curl, without writing code
* An upgraded PulsarRPA server to which we can send SQLs to collect Web data
* A Web UI from which we can write SQLs and send them to the server

Download [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro#download) and explore its capabilities with a single command line:

```shell
java -jar PulsarRPAPro.jar
```

## 🎁 PulsarRPA as a java library

The simplest way to leverage the power of PulsarRPA is to add it to your project as a library.

Maven:

```xml
<dependency>
  <groupId>ai.platon.pulsar</groupId>
  <artifactId>pulsar-bom</artifactId>
  <version>3.0.0-SNAPSHOT</version>
</dependency>
```

Gradle:

```kotlin
implementation("ai.platon.pulsar:pulsar-bom:3.0.0-SNAPSHOT")
```

You can clone the template project from github.com: [kotlin](https://github.com/platonai/pulsar-kotlin-template), [java-11](https://github.com/platonai/pulsar-java-template), [java-17](https://github.com/platonai/pulsar-java-17-template).

You can also start your own large-scale web crawling projects based on our commercial-grade open source projects: [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro), [Exotic-amazon](https://github.com/platonai/exotic-amazon).

Check the [quick start](docs/get-started/2basic-usage.md) for more details.

# 🌐 PulsarRPA as a REST Service

When PulsarRPA runs as a REST service, X-SQL can be used to scrape webpages or to query web data directly at any time, from anywhere, without opening an IDE.

## Build from Source

```
git clone https://github.com/platonai/PulsarRPA.git
cd PulsarRPA && bin/build-run.sh
```

For Chinese developers, we strongly suggest you to follow [this](/bin/tools/maven/maven-settings.md) instruction to accelerate the building process.

## Use X-SQL to Query the Web

Start the pulsar server if it is not started:

```shell
bin/pulsar
```

Scrape a webpage in another terminal window:

```shell
bin/scrape.sh
```

The bash script is straightforward. It merely uses curl to send a POST request with an X-SQL.


```shell
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

Example code: [bash](bin/scrape.sh), [batch](bin/scrape.bat), [java](/pulsar-client/src/main/java/ai/platon/pulsar/client/Scraper.java), [kotlin](/pulsar-client/src/main/kotlin/ai/platon/pulsar/client/Scraper.kt), [php](/pulsar-client/src/main/php/Scraper.php).

Click [X-SQL](docs/x-sql.md) to see a detailed introduction and function descriptions about X-SQL.

# 📖 Step-by-Step Course

We have a step-by-step course by example:

- [Home](docs/get-started/1home.md)
- [Basic Usage](docs/get-started/2basic-usage.md)
- [Load Options](docs/get-started/3load-options.md)
- [Data Extraction](docs/get-started/4data-extraction.md)
- [URL](docs/get-started/5URL.md)
- [Java-style Async](docs/get-started/6Java-style-async.md)
- [Kotlin-style Async](docs/get-started/7Kotlin-style-async.md)
- [Continuous Crawling](docs/get-started/8continuous-crawling.md)
- [Event Handling](docs/get-started/9event-handling.md)
- [RPA](docs/get-started/10RPA.md)
- [WebDriver](docs/get-started/11WebDriver.md)
- [Massive Crawling](docs/get-started/12massive-crawling.md)
- [X-SQL](docs/get-started/13X-SQL.md)
- [AI Extraction](docs/get-started/14AI-extraction.md)
- [REST](docs/get-started/15REST.md)
- [Console](docs/get-started/16console.md)
- [Top Practice](docs/get-started/17top-practice.md)
- [Miscellaneous](docs/get-started/18miscellaneous.md)

# 📊 Logs & Metrics

PulsarRPA has carefully designed the logging and metrics subsystem to record every event that occurs in the system. PulsarRPA logs the status for every load execution, providing a clear and comprehensive overview of system performance. This detailed logging allows for quick assessment of the system’s health and efficiency. It answers key questions such as: Is the system operating smoothly? How many pages have been successfully retrieved? How many attempts were made to reload pages? And how many proxy IP addresses have been utilized? This information is invaluable for monitoring and troubleshooting purposes, ensuring that any issues can be promptly identified and addressed.

By focusing on a concise set of indicators, you can unlock a deeper understanding of the system’s overall condition: 💯 💔 🗙  ?💿 🔃 🤺.

Typical page loading logs are shown below. Check the [log-format](docs/log-format.md) to learn how to read the logs and gain insight into the state of the entire system at a glance.

```plaintext
2022-09-24 11:46:26.045  INFO [-worker-14] a.p.p.c.c.L.Task - 3313. 💯  ?U for N got 200 580.92 KiB in 1m14.277s, fc:1 | 75/284/96/277/6554 | 106.32.12.75 | 3xBpaR2 | https://www.walmart.com/ip/Restored-iPhone-7-32GB-Black-T-Mobile-Refurbished/329207863  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:09.190  INFO [-worker-32] a.p.p.c.c.L.Task - 3738. 💯 💿 U got 200 452.91 KiB in 55.286s, last fetched 9h32m50s ago, fc:1 | 49/171/82/238/6172 | 121.205.2.0.5 | https://www.walmart.com/ip/Boost-Mobile-Apple-iPhone-SE-2-Cell-Phone-Black-64GB-Prepaid-Smartphone/490934488  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:28.567  INFO [-worker-17] a.p.p.c.c.L.Task - 2269. 💯 🔃 U for SC got 200 565.07 KiB <- 543.41 KiB in 1m22.767s, last fetched 16m58s ago, fc:6 | 58/230/98/295/6272 | 27.158.125.76 | 9uwu602 | https://www.walmart.com/ip/Straight-Talk-Apple-iPhone-11-64GB-Purple-Prepaid-Smartphone/356345388?variantFieldId=actual_color  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:18.390  INFO [r-worker-8] a.p.p.c.c.L.Task - 3732. 💔  ?U for N got 1601 0 <- 0 in 32.201s, fc:1/1 Retry(1601) rsp: CRAWL, rrs: EMPTY_0B | 2zYxg52 | https://www.walmart.com/ip/Apple-iPhone-7-256GB-Jet-Black-AT-T-Locked-Smartphone-Grade-B-Used/182353175?variantFieldId=actual_color  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:13.860  INFO [-worker-60] a.p.p.c.c.L.Task - 2828. 🗙 🗙 U for SC got 200 0 <- 348.31 KiB <- 684.75 KiB in 0s, last fetched 18m55s ago, fc:2 | 34/130/52/181/5747 | 60.184.124.232 | 11zTa0r2 | https://www.walmart.com/ip/Walmart-Family-Mobile-Apple-iPhone-11-64GB-Black-Prepaid-Smartphone/209201965?athbdg=L1200  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
```
# 💻 System Requirements

- Memory 4G+
- JDK 17+
- `java` on the PATH
- Latest Google Chrome
- [Optional] MongoDB started

PulsarRPA is tested on Ubuntu 18.04, Ubuntu 20.04, Windows 7, Windows 11, WSL, and any other operating system that meets the requirements should work as well.

# 🛸 Advanced Topics

Check the [advanced topics](docs/faq/advanced-topics.md) to find out the answers for the following questions:

- What’s so difficult about scraping web data at scale?
- How to scrape a million product pages from an e-commerce website a day?
- How to scrape pages behind a login?
- How to download resources directly within a browser context?
- How to scrape a single page application (SPA)?
- Resource mode
- RPA mode
- How to make sure all fields are extracted correctly?
- How to crawl paginated links?
- How to crawl newly discovered links?
- How to crawl the entire website?
- How to simulate human behaviors?
- How to schedule priority tasks?
- How to start a task at a fixed time point?
- How to drop a scheduled task?
- How to know the status of a task?
- How to know what's going on in the system?
- How to automatically generate the CSS selectors for fields to scrape?
- How to extract content from websites using machine learning automatically with commercial accuracy?
- How to scrape amazon.com to match industrial needs?

# 🆚 Compare with Other Solutions

In general, the features mentioned in the Feature section are well-supported by PulsarRPA, but other solutions do not.

Check the [solution comparison](docs/faq/solution-comparison.md) to see the detailed comparison to the other solutions:

- PulsarRPA vs selenium/puppeteer/playwright
- PulsarRPA vs nutch
- PulsarRPA vs scrapy+splash

# 🤓 Technical Details

Check the [technical details](docs/faq/technical-details.md) to see answers for the following questions:

- How to rotate my IP addresses?
- How to hide my bot from being detected?
- How & why to simulate human behaviors?
- How to render as many pages as possible on a single machine without being blocked?

# 🐦 Contact

- Wechat: galaxyeye
- Weibo: [galaxyeye](https://weibo.com/galaxyeye)
- Email: galaxyeye@live.cn, ivincent.zhang@gmail.com
- Twitter: galaxyeye8
- Website: [platon.ai](http://platon.ai)
