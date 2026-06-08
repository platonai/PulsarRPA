# 🤖 PulsarRPA

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/pulsar-rpa?style=flat-square)](https://hub.docker.com/r/galaxyeye88/pulsar-rpa)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/PulsarRPA/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

[English](README.md) | 简体中�?| [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 介绍

💖 **PulsarRPA: 由AI驱动的超快浏览器自动化解决方�?** 💖

### �?核心能力:

- 🤖 **AI集成与大语言模型** �?由大语言模型驱动的更智能自动化�?
- �?**超快自动�?* �?协程安全的浏览器自动化并发，爬虫级别的抓取性能�?
- 🧠 **网页理解** �?深度理解动态网页内容�?
- 📊 **数据提取API** �?强大的结构化数据轻松提取工具�?

---

通过简单文本实现大规模浏览器自动化和数据提取�?

```text
访问 https://www.amazon.com/dp/B08PP5MSVB

浏览器启动后: 清除浏览器cookies�?
页面加载�? 滚动到页面中间�?

总结产品信息�?
提取: 产品名称、价格、评分�?
查找所有包�?/dp/ 的链接�?
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 哔哩哔哩:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)


---

# 🚀 快速开始指�?

## ▶️ 运行 PulsarRPA

### 📦 运行可执行JAR �?最佳体�?

#### 🧩 下载

```shell
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.17/PulsarRPA.jar
```

#### 🚀 运行

```shell
# 确保设置了LLM API密钥。支持VOLCENGINE_API_KEY/OPENAI_API_KEY�?
echo $DEEPSEEK_API_KEY
java -D"DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}" -jar PulsarRPA.jar
```

> 🔍 **提示:** 确保已在环境中设置`DEEPSEEK_API_KEY`或其他LLM API密钥，否则AI功能将不可用�?

> 🔍 **提示:** Windows PowerShell语法: `$env:DEEPSEEK_API_KEY`(环境变量) vs `$DEEPSEEK_API_KEY`(脚本变量)�?

---

<details>
<summary>📂 资源</summary>

* 🟦 [GitHub Release 下载](https://github.com/platonai/PulsarRPA/releases/download/v3.0.17/PulsarRPA.jar)
* 📁 [镜像/备份下载](https://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠�?[LLM配置指南](docs/config/llm/llm-config.md)
* 🛠�?[配置指南](docs/config.md)

</details>

### �?使用IDE运行

<details>

- 在IDE中打开项目
- 运行`ai.platon.pulsar.app.PulsarApplicationKt`主类

</details>

### 🐳 Docker用户

<details>

```shell
# 确保设置了LLM API密钥。支持VOLCENGINE_API_KEY/OPENAI_API_KEY�?
echo $DEEPSEEK_API_KEY
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```
</details>

---

## 🌟 面向初学�?�?只需文本，无需代码�?

使用`commands` API执行浏览器操作、提取网页数据、分析网站等�?

### 📥 示例请求(基于文本):

网页界面: http://localhost:8182/command.html

<img src="docs/images/commander-ui.png" alt="commander" width="500" />

<details>
<summary>REST API</summary>

#### 📄 纯文本版�?
```shell
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d '
    访问 https://www.amazon.com/dp/B08PP5MSVB

    浏览器启动后: 清除浏览器cookies�?
    页面加载�? 滚动到页面中间�?

    总结产品信息�?
    提取: 产品名称、价格、评分�?
    查找所有包�?/dp/ 的链接�?
  '
```

#### 📄 JSON版本:

```shell
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
    "url": "https://www.amazon.com/dp/B08PP5MSVB",
    "onBrowserLaunchedActions": ["清除浏览器cookies"],
    "onPageReadyActions": ["滚动到页面中�?],
    "pageSummaryPrompt": "简要介绍这个产品�?,
    "dataExtractionRules": "产品名称、价格和评分",
    "uriExtractionRules": "页面上所有包含`/dp/`的链�?
  }'
```

💡 **提示:** 您不需要填写每个字�?�?只填写您需要的�?

</details>

## 🎓 面向高级用户 �?LLM + X-SQL: 精准、灵活、强�?

利用`x/e` API进行高度精确、灵活和智能的数据提取�?

  ```shell
  curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称、价格、评�?) as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
  "
  ```

提取数据示例:

```json
{
  "llm_extracted_data": {
    "产品名称": "Apple iPhone 15 Pro Max",
    "价格": "$1,199.00",
    "评分": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

* X-SQL指南: [X-SQL](docs/x-sql.md)

---

## 👨‍�?面向专家 - 原生API: 功能强大!

### 🚀 超快页面访问和数据提�?

PulsarRPA通过基于协程的并发实现高速并行网页抓取，在最小化资源开销的同时提供高效的数据提取�?

<details>

```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
val links =
    LinkExtractors.fromResource(resource).asSequence().map { ListenableHyperlink(it, "", args = args) }.onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page: Page, driver: WebDriver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }.toList()

session.submitAll(links)
```

📝 示例: [查看Kotlin代码](https://github.com/platonai/PulsarRPA/blob/master/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/advanced/HighPerformanceCrawler.kt)

</details>

### 🎮 浏览器控�?

PulsarRPA实现了协程安全的浏览器控制�?

<details>

```kotlin
val prompts = """
将光标移到id�?title'的元素上并点�?
滚动到页面中�?
滚动到页面顶�?
获取id�?title'的元素的文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page: Page, driver: WebDriver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例: [查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

</details>

---

### 🤖 机器人流程自动化能力:

PulsarRPA提供灵活的机器人流程自动化能力�?

<details>

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page: Page, driver: WebDriver ->
    warnUpBrowser(page, driver)
}
event.onWillFetch.addLast { page: Page, driver: WebDriver ->
    waitForReferrer(page, driver)
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page: Page, driver: WebDriver ->
    driver.waitForSelector("body h1[itemprop=name]")
    driver.click(".mask-layer-close-button")
}
session.load(url, options)
```
📝 示例: [查看Kotlin代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

</details>

---

### 🔍 使用X-SQL进行复杂数据提取:

PulsarRPA提供X-SQL用于复杂数据提取�?

<details>

```sql
select
    llm_extract(dom, '产品名称、价格、评分、评分�?) as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB  -i 1s -njr 3', 'body');
```

📚 示例代码:
* [亚马逊产品页面抓�?100+字段)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [所有亚马逊页面类型抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

</details>

---

## 📜 文档

* 📖 [REST API示例](docs/rest-api-examples.md)
* 🛠�?[LLM配置指南](docs/config/llm/llm-config.md)
* 🛠�?[配置指南](docs/config.md)
* 📚 [从源码构建](docs/development/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理 - 解除网站封锁

<details>

设置环境变量PROXY_ROTATION_URL为代理服务提供的URL:

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问轮换URL时，它应返回包含一个或多个新代理IP的响应�?
请向您的代理提供商询问这样的URL�?

</details>

---

## �?特�?

🕷�?**网络爬虫**
- 可扩展爬�?
- 浏览器渲�?
- AJAX数据提取

🤖 **AI驱动**
- 自动字段提取
- 模式识别
- 准确数据捕获

🧠 **LLM集成**
- 自然语言网页内容分析
- 直观内容描述

🎯 **文本到动�?*
- 简单语言命令
- 直观浏览器控�?

🤖 **RPA能力**
- 类人任务自动�?
- SPA爬取支持
- 高级工作流自动化

🛠�?**开发者友�?*
- 一行数据提�?
- SQL式查询接�?
- 简单API集成

📊 **X-SQL强大功能**
- 扩展SQL用于网页数据
- 内容挖掘能力
- Web商业智能

🛡�?**机器人防�?*
- 高级隐身技�?
- IP轮换
- 隐私上下文管�?

�?**性能**
- 并行页面渲染
- 高效处理
- 抗封锁设�?

💰 **成本效益**
- 每天100,000+页面
- 最小硬件要�?
- 资源高效运行

�?**质量保证**
- 智能重试机制
- 精确调度
- 完整生命周期管理

🌐 **可扩展�?*
- 完全分布式架�?
- 大规模能�?
- 企业级就�?

📦 **存储选项**
- 本地文件系统
- MongoDB
- HBase
- Gora支持

📊 **监控**
- 全面日志
- 详细指标
- 完全透明�?

## 📞 联系我们

- 💬 微信: galaxyeye
- 🌐 微博: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 推特: galaxyeye8
- 🌍 网站: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="微信二维�? />
</div>
