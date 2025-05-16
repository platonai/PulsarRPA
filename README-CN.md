# 🤖 PulsarRPA

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/pulsar-rpa?style=flat-square)](https://hub.docker.com/r/galaxyeye88/pulsar-rpa)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/PulsarRPA/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 简介

💖 **PulsarRPA：AI 驱动的极速浏览器自动化解决方案！** 💖

### ✨ 主要功能：

- 🤖 **AI 大模型集成** —— 让自动化更智能
- ⚡ **极速自动化** —— 协程安全的浏览器并发，爬虫级抓取性能
- 🧠 **网页理解能力** —— 深度解析动态网页内容
- 📊 **数据提取 API** —— 轻松提取结构化数据

---

只需简单的文本指令，即可自动化浏览器并大规模提取数据。

```text
访问 https://www.amazon.com/dp/B0C1H26C46
页面加载后：滚动到页面中部。

总结该商品。
提取：商品名称、价格、评分。
查找所有包含 /dp/ 的链接。
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

# 🚀 快速开始

## ▶️ 运行 PulsarRPA

### 📦 运行可执行 JAR —— 推荐体验

#### 🧩 下载

```bash
# 适用于 Linux/macOS/Windows（需 curl）
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.7/PulsarRPA.jar
```

#### 🚀 运行

```bash
java -DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPA.jar
```

> 🔍 **提示：** 请确保环境变量 `DEEPSEEK_API_KEY` 已设置，否则 AI 功能不可用。

---

<details>
<summary>📂 资源下载</summary>

* 🟦 [GitHub Release 下载](https://github.com/platonai/PulsarRPA/releases/download/v3.0.7/PulsarRPA.jar)
* 📁 [国内镜像/备用下载](http://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠️ [大模型配置指南](docs/config/llm/llm-config.md)
* 🛠️ [配置指南](docs/config.md)

</details>

### ▶ 使用 IDE 运行

<details>

- 用 IDE 打开项目
- 运行主类 `ai.platon.pulsar.app.PulsarApplicationKt`

</details>

### 🐳 Docker 用户

<details>

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```

</details>

---

## 🌟 零基础入门 —— 纯文本，无需编码！

通过 `ai/command` API，使用自然语言指令即可执行操作和提取数据。

### 📥 示例请求（文本版）：

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    访问 https://www.amazon.com/dp/B0C1H26C46
    页面加载后：点击 #title，然后滚动到页面中部。
    
    总结该商品。
    提取：商品名称、价格、评分。
    查找所有包含 /dp/ 的链接。
  '
```

### 📄 JSON 版：

<details>

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "pageSummaryPrompt": "请简要介绍该商品。",
    "dataExtractionRules": "商品名称、价格、评分",
    "linkExtractionRules": "页面上所有包含 `/dp/` 的链接",
    "onPageReadyActions": ["点击 #title", "滚动到页面中部"]
  }'
```

💡 **提示：** 只需填写你需要的字段即可。

</details>

## 🎓 进阶用户 —— LLM + X-SQL：精准、灵活、强大

利用 `x/e` API，实现高精度、灵活、智能的数据提取。

  ```bash
  curl -X POST "http://localhost:8182/api/scrape/execute" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, '商品名称、价格、评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
  "
  ```

提取结果示例：

```json
{
  "llm_extracted_data": {
    "商品名称": "Apple iPhone 15 Pro Max",
    "价格": "$1,199.00",
    "评分": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

* X-SQL 指南: [X-SQL](docs/x-sql.md)

---

## 👨‍💻 专家模式 - 原生 API，极致强大！

### 🎮 浏览器控制：

<details>

```kotlin
val prompts = """
移动鼠标到 id 为 'title' 的元素并点击
滚动到页面中部
滚动到顶部
获取 id 为 'title' 的元素文本
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 示例：[查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

</details>

---

### 🤖 完整 RPA 能力：

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
📝 示例：[查看 Kotlin 代码](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

</details>

---

### 🔍 X-SQL 复杂数据提取：

<details>

```sql
select
    llm_extract(dom, '商品名称、价格、评分、分数') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 示例代码：
* [亚马逊商品页抓取（100+字段）](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [亚马逊全类型页面抓取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

</details>

---

## 📜 文档

* 📖 [REST API 示例](docs/rest-api-examples.md)
* 🛠️ [大模型配置指南](docs/config/llm/llm-config.md)
* 🛠️ [配置指南](docs/config.md)
* 📚 [源码编译](docs/development/build.md)
* 🧠 [专家指南](docs/advanced-guides.md)

---

## 🔧 代理 - 解锁网站

<details>

设置环境变量 PROXY_ROTATION_URL 为你的代理服务提供的 URL：

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问该 URL 时，应返回一个或多个新的代理 IP。
请向你的代理服务商索取此类 URL。

</details>

---

## ✨ 功能亮点

🕷️ **网页爬虫**
- 可扩展抓取
- 浏览器渲染
- AJAX 数据提取

🤖 **AI 驱动**
- 自动字段提取
- 模式识别
- 精准数据捕获

🧠 **大模型集成**
- 自然语言网页内容分析
- 直观内容描述

🎯 **文本指令自动化**
- 简单语言命令
- 直观浏览器控制

🤖 **RPA 能力**
- 拟人化任务自动化
- SPA 页面抓取支持
- 高级工作流自动化

🛠️ **开发者友好**
- 一行代码提取数据
- 类 SQL 查询接口
- 简单 API 集成

📊 **X-SQL 强大能力**
- 扩展 SQL 用于网页数据
- 内容挖掘能力
- 网页商业智能

🛡️ **反爬保护**
- 高级隐身技术
- IP 轮换
- 隐私上下文管理

⚡ **高性能**
- 并行页面渲染
- 高效处理
- 抗封锁设计

💰 **高性价比**
- 日处理 10 万+ 页面
- 极低硬件需求
- 资源高效利用

✅ **质量保障**
- 智能重试机制
- 精准调度
- 完整生命周期管理

🌐 **可扩展性**
- 全分布式架构
- 大规模能力
- 企业级支持

📦 **多存储选项**
- 本地文件系统
- MongoDB
- HBase
- Gora 支持

📊 **监控能力**
- 全面日志
- 详细指标
- 完全透明

## 📞 联系我们

- 💬 微信：galaxyeye
- 🌐 微博：[galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱：galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 推特：galaxyeye8
- 🌍 官网：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="WeChat QR Code" />
</div>
