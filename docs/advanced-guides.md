# PulsarRPA - Advanced Usage

## 🧮 PulsarRPA as an executable jar

We have released a standalone executable JAR based on PulsarRPA, which includes:

- Data collection examples from top-tier websites.
- A mini-program for automatic information extraction based on self-supervised machine learning. The AI algorithm can identify all fields on detail pages with field accuracy exceeding 99%.
- A mini-program that automatically learns and outputs all collection rules based on self-supervised machine learning.
- The ability to execute web data collection tasks directly from the command line without writing any code.
- An upgraded PulsarRPA server that allows you to send SQL statements to collect web data.
- A Web UI where you can write SQL statements and send them to the server.

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
    <version>3.0.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("ai.platon.pulsar:pulsar-bom:3.0.0")
```

Clone the template project from github.com:
[kotlin](https://github.com/platonai/pulsar-kotlin-template),
[java-17](https://github.com/platonai/pulsar-java-17-template).

Start your own large-scale web crawling projects based on our commercial-grade open source projects: [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro), [Exotic-amazon](https://github.com/platonai/exotic-amazon).

Check the [quick start](docs/get-started/2basic-usage.md) for more details.

# 🌐 PulsarRPA as a REST Service

When PulsarRPA runs as a REST service, X-SQL can be used to scrape webpages or to query web data directly at any time, from anywhere, without opening an IDE.

## Build from Source

```shell
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

Linux:

```shell
bin/scrape.sh
```

Windows:

```shell
bin/scrape.ps1
```
Or
```shell
bin/scrape.bat
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

Example code: [bash](bin/scrape.sh), [PowerShell](bin/scrape.ps1), [batch](bin/scrape.bat), [java](/ai/platon/pulsar/client/Scraper.java), [kotlin](/ai/platon/pulsar/client/Scraper.kt), [php](/src/main/php/Scraper.php).

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
- Website: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="wechat-author" />
  <img src="docs/images/buy-me-a-coffee.png" width="300" alt="buy-me-a-coffee" />
</div>
