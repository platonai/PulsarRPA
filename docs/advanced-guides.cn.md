# PulsarRPA - 高级用法

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
    <version>VERSION</version>
</dependency>
```

使用 Gradle 时，可以在 `build.gradle` 文件中添加以下依赖：

```kotlin
implementation("ai.platon.pulsar:pulsar-bom:VERSION")
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
curl -X POST --location "http://localhost:8182/api/scrape/execute" -H "Content-Type: text/plain" -d "
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
