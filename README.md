# 🤖 PulsarRPA

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🌟 Introduction

💖 **PulsarRPA: Your Ultimate AI-RPA Solution!** 💖

**PulsarRPA** is a **high-performance** 🚀, **distributed** 🌐, and **open-source** 🔓 Robotic Process Automation (RPA) framework.
Designed for **large-scale automation** 🏭, it excels in:
- 🌐 **Browser automation, super-fast⚡, spider–grade⚡**
- 🧠 **Web content understanding**
- 📊 **Data extraction**

PulsarRPA tackles the challenges of modern web automation,
ensuring **accurate** ✅ and **comprehensive** 📚 data extraction even from the most **complex** 🔄 and **dynamic** ⚡ websites.

## 🎥 Demo Videos

🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

## 🐳 Docker Setup

### 🔧 Basic Setup (Without LLM)

```shell
docker run -d -p 8182:8182 galaxyeye88/pulsar-rpa:latest
```

### 🧠 LLM Integration

🔑 Get your API key here:
https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey?apikey=xxx

```shell
docker run -d -p 8182:8182 \
  -e llm.provider=volcengine \
  -e llm.name=${YOUR-MODEL_NAME} \
  -e llm.apiKey=${YOUR-LLM_API_KEY} \
  galaxyeye88/pulsar-rpa:latest
```

## 🚀 Quick Start Guide

### 🌟 For Beginners - No Special Skills Required!

#### 💬 Chat About a Webpage
```shell
curl -X POST "http://localhost:8182/api/ai/chat-about" \
-H "Content-Type: application/json" \
-d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "introduce this product"
}'
```

#### 📊 Extract Data
```shell
curl -X POST "http://localhost:8182/api/ai/extract" \
-H "Content-Type: application/json" \
-d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "prompt": "product name, price, and description"
}'
```

### 🎓 For Advanced Users - LLM + X-SQL

```bash
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

The extracted data:
```json
{
  "llm_extracted_data": {
    "product name": "Apple iPhone 15 Pro Max",
    "price": "$1,199.00",
    "ratings": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

### 👨‍💻 For Experts - Native API

#### 💭 Chat About a Webpage:
```kotlin
val document = session.loadDocument(url)
val response = session.chat("Tell me something about this webpage", document)
```
📝 Example: [View Kotlin Code](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt)

#### 🎮 Browser Control:
```kotlin
val prompts = """
move cursor to the element with id 'title' and click it
scroll to middle
scroll to top
get the text of the element with id 'title'
"""

val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
    val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```
📝 Example: [View Kotlin Code](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt)

#### ⚡ One-Line Scraping:
```kotlin
session.scrapeOutPages(
    "https://www.amazon.com/",  
    "-outLink a[href~=/dp/]", 
    listOf("#title", "#acrCustomerReviewText")
)
```

#### 🤖 RPA Crawling:
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
📝 Example: [View Kotlin Code](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/sites/food/dianping/RestaurantCrawler.kt)

#### 🔍 Complex Data Extraction with X-SQL:
```sql
select
    llm_extract(dom, 'product name, price, ratings, score') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 Example Code:
* [Amazon Product Page Scraping (100+ fields)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [All Amazon Page Types Scraping](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

📖 [Advanced Guides](docs/advanced-guides.md)

## ✨ Features

🕷️ **Web Spider**
- Scalable crawling
- Browser rendering
- AJAX data extraction

🧠 **LLM Integration**
- Natural language web content analysis
- Intuitive content description

🎯 **Text-to-Action**
- Simple language commands
- Intuitive browser control

🤖 **RPA Capabilities**
- Human-like task automation
- SPA crawling support
- Advanced workflow automation

🛠️ **Developer-Friendly**
- One-line data extraction
- SQL-like query interface
- Simple API integration

📊 **X-SQL Power**
- Extended SQL for web data
- Content mining capabilities
- Web business intelligence

🛡️ **Bot Protection**
- Advanced stealth techniques
- IP rotation
- Privacy context management

⚡ **Performance**
- Parallel page rendering
- High-efficiency processing
- Block-resistant design

💰 **Cost-Effective**
- 100,000+ pages/day
- Minimal hardware requirements
- Resource-efficient operation

✅ **Quality Assurance**
- Smart retry mechanisms
- Precise scheduling
- Complete lifecycle management

🌐 **Scalability**
- Fully distributed architecture
- Massive-scale capability
- Enterprise-ready

📦 **Storage Options**
- Local File System
- MongoDB
- HBase
- Gora support

📊 **Monitoring**
- Comprehensive logging
- Detailed metrics
- Full transparency

🤖 **AI-Powered**
- Automatic field extraction
- Pattern recognition
- Accurate data capture

## 📞 Contact Us

- 💬 WeChat: galaxyeye
- 🌐 Weibo: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 Email: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter: galaxyeye8
- 🌍 Website: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" alt="WeChat QR Code" />
  <img src="docs/images/buy-me-a-coffee.png" width="300" alt="Support Us" />
</div>
