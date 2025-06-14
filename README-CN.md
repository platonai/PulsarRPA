# 🤖 PulsarRPA

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/pulsar-rpa?style=flat-square)](https://hub.docker.com/r/galaxyeye88/pulsar-rpa)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/PulsarRPA/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 简介

💖 **PulsarRPA: 人工智能驱动的闪电般快速浏览器自动化解决方案!** 💖

### ✨ 核心能力：

- 🤖 **AI与大语言模型集成** – 由大型语言模型支持的更智能自动化
- ⚡ **超快速自动化** – 协程安全的浏览器自动化并发，爬虫级别的爬取性能
- 🧠 **Web理解** – 深度理解动态网页内容
- 📊 **数据提取API** – 轻松提取结构化数据的强大工具

---

使用简单文本实现浏览器自动化和大规模数据提取。

```text
访问 https://www.amazon.com/dp/B0C1H26C46
页面加载后：滚动到页面中部。

概述产品。
提取：产品名称、价格、评分。
查找所有包含/dp/的链接。
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)


---

# 🚀 快速开始指南

## ▶️ 运行PulsarRPA

### 📦 运行可执行JAR — 最佳体验

#### 🧩 下载

```bash
# 适用于Linux/macOS/Windows(使用curl)
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.1.0/PulsarRPA.jar
```

#### 🚀 运行

```bash
java -DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPA.jar
```

> 🔍 **提示：** 确保在环境中设置了`DEEPSEEK_API_KEY`，否则AI功能将不可用。

---

<details>
<summary>📂 资源</summary>

* 🟦 [GitHub发布下载](https://github.com/platonai/PulsarRPA/releases/download/v3.1.0/PulsarRPA.jar)
* 📁 [镜像/备份下载](http://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠️ [LLM配置指南](docs/config/llm/llm-config.md)
* 🛠️ [配置指南](docs/config.md)

</details>

### ▶ 使用IDE运行

<details>

- 在IDE中打开项目
- 运行`ai.platon.pulsar.app.PulsarApplicationKt`主类

</details>

### 🐳 Docker用户

<details>

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```

</details>

---

## 🌟 初学者 – 只需文本，无需代码！

使用`commands` API执行浏览器操作、提取网页数据、分析网站等。

### 📥 示例请求（基于文本）：

WebUI: http://localhost:8182/command.html

<img src="docs/images/commander-ui.png" alt="commander" width="500" />

<details>
<summary>REST API</summary>

#### 📄 基于纯文本版本：
```bash
curl -X POST "http://localhost:8182/api/commands/plain" \
  -H "Content-Type: text/plain" \
  -d '
    访问 https://www.amazon.com/dp/B0C1H26C46
    页面加载后：点击#title，然后滚动到中间。

    概述产品。
    提取：产品名称、价格、评分。
    查找所有包含/dp/的链接。
  '
```

#### 📄 基于JSON版本：

```bash
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "pageSummaryPrompt": "提供该产品的简要介绍。",
    "dataExtractionRules": "产品名称、价格和评分",
    "linkExtractionRules": "页面上所有包含`/dp/`的链接",
    "onPageReadyActions": ["点击#title", "滚动到中间"]
  }'
```

💡 **提示：** 您不需要填写每个字段 — 只需填写您需要的部分。

</details>

## 🎓 进阶用户 — LLM + X-SQL：精确、灵活、强大

利用 `scrape/tasks/submit` API进行高度精确、灵活和智能的数据提取。

  ```bash
  curl -X POST "http://localhost:8182/api/scrape/tasks/submit" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '产品名称、价格、评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
  "
  ```

提取数据示例：

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

* X-SQL指南: [X-SQL](docs/x-sql.md)

---

## 👨‍💻 专家用户 - 原生API：功能强大！

### 🎮 浏览器控制：

<details>

```kotlin
val prompts = """
将光标移动到id为'title'的元素并点击
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

</details>

---

### 🤖 完整的机器人流程自动化能力：

<details>

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

</details>

---

### 🔍 使用X-SQL进行复杂数据提取：

<details>

```sql
select
    llm_extract(dom, '产品名称、价格、评分、评分值') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码:
* [亚马逊产品页面抓取(100+字段)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [所有亚马逊页面类型抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

</details>

---

## 📜 文档

* 📖 [REST API示例](docs/rest-api-examples.md)
* 🛠️ [LLM配置指南](docs/config/llm/llm-config.md)
* 🛠️ [配置指南](docs/config.md)
* 📚 [从源码构建](docs/development/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理 - 解除网站限制

<details>

设置环境变量PROXY_ROTATION_URL为您的代理服务提供的URL：

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问轮换URL时，应返回包含一个或多个新代理IP的响应。
向您的代理提供商询问此类URL。

</details>

---

## ✨ 功能

🕷️ **Web爬虫**
- 可扩展爬取
- 浏览器渲染
- AJAX数据提取

🤖 **AI驱动**
- 自动字段提取
- 模式识别
- 精准数据捕获

🧠 **LLM集成**
- 自然语言网页内容分析
- 直观内容描述

🎯 **文本到行动**
- 简单语言命令
- 直观浏览器控制

🤖 **RPA能力**
- 类人任务自动化
- 单页应用爬取支持
- 高级工作流自动化

🛠️ **开发者友好**
- 一行数据提取
- SQL类查询接口
- 简单API集成

📊 **X-SQL强大功能**
- 扩展的Web数据SQL
- 内容挖掘能力
- Web商业智能

🛡️ **机器人保护**
- 先进隐身技术
- IP轮换
- 隐私上下文管理

⚡ **性能**
- 并行页面渲染
- 高效处理
- 防阻断设计

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

## 📞 联系我们

- 💬 微信: galaxyeye
- 🌐 微博: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 推特: galaxyeye8
- 🌍 网站: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="微信二维码" />
</div>
