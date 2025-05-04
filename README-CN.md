# 🤖 PulsarRPA

[English](README.md) | 简体中文 | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 介绍

💖 **PulsarRPA：AI驱动的极速浏览器自动化新纪元！** 💖

**PulsarRPA** 是一款集 **AI智能赋能** 🤖、**极致性能** 🚀、**分布式架构** 🌐 与 **开源精神** 🔓 于一体的下一代浏览器自动化平台，专为 **大规模高强度自动化场景** 🏭 而生。

### ✨ 核心能力：

- 🤖 **LLM深度集成** – 使用大型语言模型实现更智能的自动化操作。
- ⚡ **超高速浏览器操控** – 协程安全的浏览器并发操作，蜘蛛级别的高性能爬取体验。
- 🧠 **网页内容理解** – 深度解析动态网页结构。
- 📊 **数据提取API** – 提供强大的结构化数据提取接口。

PulsarRPA 致力于突破传统网页自动化的边界，提供 **精准** ✅、**全面** 📚 的数据提取解决方案，轻松应对最 **复杂** 🔄、最 **动态变化** ⚡ 的网站挑战，助力企业极速获取核心数据，赢在未来！

---

简单文字描述，即可自动操作浏览器并大规模提取数据。

```text
访问 https://www.amazon.com/dp/B0C1H26C46
页面加载完成后：滚动到页面中部。

总结该产品。
提取字段：产品名称、价格、评分。
查找所有包含 /dp/ 的链接。
```

---

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

📺 哔哩哔哩:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

# 🚀 快速入门指南

## ▶️ 运行 PulsarRPA

下载并运行最新版本的 JAR 文件：

```bash
# Linux/macOS and Windows (if curl is available)
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.3/PulsarRPA.jar
```

```bash
java -DDEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPA.jar
```


> 🔍 **提示：** 即使不设置 `DEEPSEEK_API_KEY`，高级用户仍可以使用所有进阶功能。

🔗 [选择其他 LLM 提供商](docs/config/llm/llm-config-advanced.md)

### 📦 下载链接

- 🟦 [GitHub Release](https://github.com/platonai/PulsarRPA/releases/download/v3.0.3/PulsarRPA.jar)
- 🇨🇳 [国内用户专用](http://static.platonai.cn/repo/ai/platon/pulsar/PulsarRPA.jar)

### 🐳 Docker 用户

```bash
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```


---

# 🌟 初学者友好 – 只需对话，无需编程技能！

使用 `command` API 通过自然语言指令执行网页操作并提取数据。

### 📥 示例请求（文本格式）：

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    访问 https://www.amazon.com/dp/B0C1H26C46
    总结该产品。
    提取字段：产品名称、价格、评分。
    查找所有包含 /dp/ 的链接。
    页面加载完成后：点击 #title，然后滚动到页面中部。
  '
```


### 📄 JSON 格式请求：

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "pageSummaryPrompt": "请简要介绍该产品。",
    "dataExtractionRules": "产品名称、价格、评分",
    "linkExtractionRules": "页面上所有包含 `/dp/` 的链接",
    "onPageReadyActions": ["点击 #title", "滚动到页面中部"]
  }'
```


💡 **小贴士：** 不需要填写每一个字段，只需你关心的部分即可。

---

# 🎓 进阶用户 – LLM + X-SQL

```bash
curl -X POST "http://localhost:8182/api/x/e" \
  -H "Content-Type: text/plain" \
  -d "
  select
    llm_extract(dom, '产品名称, 价格, 评分') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```


返回的数据示例：
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


---

# 👨‍💻 专家用户 – 原生 API

## 🎮 浏览器控制示例（Kotlin）：

```kotlin
val prompts = """
将鼠标移动到id为'title'的元素并点击
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

📝 示例代码: [查看 Kotlin 示例](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

---

## 🤖 RPA 爬虫示例（Kotlin）：

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver -> warnUpBrowser(page, driver) }
event.onWillFetch.addLast { page, driver -> waitForReferrer(page, driver); waitForPreviousPage(page, driver) }
event.onWillCheckDocumentState.addLast { page, driver ->
    driver.waitForSelector("body h1[itemprop=name]")
    driver.click(".mask-layer-close-button")
}
session.load(url, options)
```

📝 示例代码: [查看餐厅爬虫示例](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

---

## 🔍 使用 X-SQL 进行复杂数据提取：

```sql
select
    llm_extract(dom, '产品名称, 价格, 评分, 得分') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```


📚 示例代码仓库：
* [亚马逊产品页面爬取 (100+ 字段)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [所有亚马逊页面类型爬取](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

---

## 📜 文档导航

- 📖 [进阶指南](docs/advanced-guides.md)
- 📊 [REST API 示例](docs/rest-api-examples.md)

---

## ✨ 特性概览

🕷️ **网络爬虫**
- 可扩展爬取
- 浏览器渲染
- AJAX 数据提取

🧠 **LLM 集成**
- 自然语言网页分析
- 直观内容描述

🎯 **文本转行动**
- 简单语言命令
- 直观浏览器控制

🤖 **RPA 能力**
- 类人任务自动化
- SPA 页面支持
- 工作流自动化

🛠️ **开发者友好**
- 一行代码提取数据
- SQL-like 查询接口
- 简单 API 集成

📊 **X-SQL 强大功能**
- 扩展 SQL 处理网页数据
- 内容挖掘能力
- Web 商业智能

🛡️ **机器人保护**
- 高级隐身技术
- IP 轮换
- 隐私上下文管理

⚡ **高性能**
- 并行页面渲染
- 高效处理
- 抗屏蔽设计

💰 **成本效益**
- 每天 10 万+ 页面
- 最低硬件要求
- 资源高效运行

✅ **质量保证**
- 智能重试机制
- 精确调度
- 完整生命周期管理

🌐 **可扩展性**
- 分布式架构
- 企业级就绪

📦 **存储选项**
- 本地文件系统
- MongoDB
- HBase
- Gora 支持

📊 **监控**
- 全面日志
- 详细指标
- 完全透明

🤖 **AI 驱动**
- 自动字段提取
- 模式识别
- 准确数据捕获

---

## 📞 联系我们

- 💬 微信: galaxyeye
- 🌐 微博: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 邮箱: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter: galaxyeye8
- 🌍 官网: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="微信二维码" />
</div>

