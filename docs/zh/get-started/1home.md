目录
=

1. [基本用法](2basic-usage.md)
2. [加载参数](3load-options.md)
3. [数据提取](4data-extraction.md)
4. [URL](5URL.md)
5. [Java 风格异步编程](6Java-style-async.md)
6. [Kotlin 特性](7Kotlin-style-async.md)
7. [连续采集](8continuous-crawling.md) 
8. [事件处理](9event-handling.md)
9. [机器人流程自动化(RPA)](10RPA.md)
10. [WebDriver](11WebDriver.md)
11. [大规模采集](12massive-crawling.md)
12. [X-SQL](13X-SQL.md)
13. [自动提取](14AI-extraction.md)
14. [REST 服务](15REST.md)
15. [控制台](16console.md)
16. [顶尖项目实战](17top-practice.md)
17. [杂项](18miscellaneous.md)

------

💖 PulsarRPA - 您的全方位自动化解决方案！💖

[PulsarRPA](https://github.com/platonai/PulsarRPA) （[国内镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)）是一款高性能、分布式、开源的机器人流程自动化（RPA）框架，专为轻松应对大规模 RPA 任务而设计，为浏览器自动化、网络内容理解和数据提取提供了全面解决方案。

作为面向大规模网络数据抽取领域的顶级开源解决方案，PulsarRPA 结合了高性能、分布式 RPA 的优势，旨在解决在快速演变且日益复杂的网站环境中进行浏览器自动化以及抽取准确、全面网络数据所固有的挑战。

*大规模网页数据提取面临的挑战*

1. **频繁的网站变更**：在线平台不断更新其布局、结构和内容，使得长期保持可靠的提取流程颇具挑战。传统的抓取工具可能难以迅速适应这些变化，导致获取到的数据过时或不再相关。
2. **复杂的网站架构**：现代网站常采用精巧的设计模式、动态内容加载及先进的安全措施，为常规抓取方法设立了严峻的难关。从这类网站中提取数据需深入理解其结构与行为，并具备像人类用户一样与其交互的能力。

*PulsarRPA：革新网页数据采集方式*

为应对上述挑战，PulsarRPA 集成了多项创新技术，确保高效、精准、可扩展的网页数据提取：

1. **浏览器渲染：**利用浏览器渲染和AJAX数据抓取从网站提取内容。
2. **RPA（机器人流程自动化）：**采用类人类行为与网页互动，实现从现代复杂网站中收集数据。
3. **智能抓取：**PulsarRPA采用智能抓取技术，能够自动识别并理解网页内容，从而确保数据提取的准确性和及时性。利用智能算法和机器学习技术，PulsarRPA 能够自主学习和应用数据提取模型，显著提高数据检索的效率和精确度。
4. **高级DOM解析：**利用高级文档对象模型（DOM）解析技术，PulsarRPA能够轻松导航复杂的网站结构。它能准确识别并提取现代网页元素中的数据，处理动态内容渲染，绕过反爬虫措施，即使面对网站的复杂性，也能提供完整准确的数据集。
5. **分布式架构：**基于分布式架构构建的PulsarRPA，能够有效地处理大规模提取任务，因为它利用了多个节点组合的计算能力。这使得并行抓取、快速数据检索成为可能，并随着数据需求的增加实现无缝扩展，同时不损害性能或可靠性。
6. **开源与可定制：**作为一个开源解决方案，PulsarRPA提供了无与伦比的灵活性和可扩展性。开发者可以轻松定制其组件、集成现有系统或贡献新功能以满足特定项目需求。

综上所述，PulsarRPA 凭借其网页内容理解、智能抓取、先进 DOM 解析、分布式处理及开源特性，成为大规模网页数据提取首选的开源解决方案。其独特的技术组合使用户能够有效应对与大规模提取宝贵网页数据相关的复杂性和挑战，最终推动更明智的决策制定和竞争优势。





我们提供了大量顶级站点的采集示例，从入门到资深，包含各种采集模式，包括顶尖大站的**全站采集**代码、反爬天花板的站点的采集示例，你可以找一个代码示例改改就可以用于自己的项目：

- [Exotic Amazon](https://github.com/platonai/exotic-amazon)，[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon) - 顶尖电商网站全站数据采集真实项目
- [Exotic Walmart](https://github.com/platonai/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/walmart)，[国内镜像](https://gitee.com/platonai_galaxyeye/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/walmart) - 顶尖电商网站数据采集示例
- [Exotic Dianping](https://github.com/platonai/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/food/dianping)，[国内镜像](https://gitee.com/platonai_galaxyeye/exotic/tree/main/exotic-app/exotic-OCR-examples/src/main/kotlin/ai/platon/exotic/examples/sites/food/dianping) - 最困难的数据采集示例

我们的**开源代码**也包含 REST 服务、像数据库客户端一样的**网页客户端**等等，基于该网页客户端，你甚至可以稍稍完善一些用户体验就可以打造与最知名“采集器”相媲美的产品。

[PulsarRPA](https://github.com/platonai/PulsarRPA)（[国内镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)）为解决网络数据管理、多源异构数据融合、网络数据挖掘、网络数据采集等问题，开发了一系列基础设施和前沿技术：支持高质量的大规模数据采集和处理，支持网络即数据库范式，支持浏览器渲染并将其作为数据采集的首要方法，支持 RPA 采集，支持退化的单一资源采集，并计划支持最前沿的信息提取技术，提供了人工智能网页提取的预览版本。

本课程将从最基本的 API 出发，逐步介绍高级特性，从而解决最棘手的重要问题。

------

目录 [下一章](2basic-usage.md)
