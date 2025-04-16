# PulsarRPA

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/PulsarRPA)

## 🥁 Introduce

💖 **PulsarRPA: Your Ultimate AI-RPA Solution!** 💖

**PulsarRPA** is a **high-performance**, **distributed**, and **open-source** Robotic Process Automation (RPA) framework.
Designed for **large-scale automation**, it excels in **browser automation**, **web content understanding**,
and **data extraction**. PulsarRPA tackles the challenges of modern web automation,
ensuring **accurate** and **comprehensive** data extraction even from the most **complex** and **dynamic** websites.

## 🎥 Videos

YouTube:
[![Watch the video](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)





## 🐳 Docker

Scrape without LLM features:

```shell
  docker run -d -p 8182:8182 galaxyeye88/pulsar-rpa:latest
```

LLM Integration:

Click the link to get your own API key:
https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey?apikey=xxx

```shell
  docker run -d -p 8182:8182 \
    -e llm.provider=volcengine \
    -e llm.name=ep-20250218201413-f54pj \
    -e llm.apiKey=${YOUR-LLM_API_KEY} \
    galaxyeye88/pulsar-rpa:latest
```

## 🚀 Quick start

### For beginners - no special skill required, just ask LLM to get jobs done

Talk about a webpage:
```shell
  curl -X POST "http://localhost:8182/api/ai/chat-about" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "prompt": "introduce this product"
  }'
```

Extract data from a webpage:
```shell
  curl -X POST "http://localhost:8182/api/ai/extract" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "prompt": "product name, price, and description"
  }'
```

### For advanced users - combine LLM and X-SQL

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

### 🚀 For experts - use native API

#### Chat about a webpage:

```kotlin
val document = session.loadDocument(url)
val response = session.chat("Tell me something about this webpage", document)
```

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/ChatAboutPage.kt).

#### Tell the browser to get jobs done:

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

Example code: [kotlin](/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/llm/TalkToActivePage.kt).

#### One line of code to scrape:

```kotlin
session.scrapeOutPages(
    "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

#### Crawl with Robotic Process Automation (RPA):

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
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

#### Resolve *super complex* web data extraction problems using X-SQL:

```sql
select
    llm_extract(dom, 'product name, price, ratings, score') as llm_extracted_data,
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

#### More advanced guides:

[advanced guides](docs/advanced-guides.md)





## 🚄 Features

- Web Spider: Scalable crawling, browser rendering, AJAX data extraction, and more.

- LLM Integration: Analyze and describe web content using natural, everyday language.

- Text-to-Action: Control browser actions through simple, intuitive language commands.

- RPA (Robotic Process Automation): Automate human-like tasks, including Single Page Application (SPA) crawling and other high-value workflows.

- Simple API: Extract data with a single line of code or transform websites into structured tables with a single SQL query.

- X-SQL: Extended SQL for managing web data—crawling, scraping, content mining, and web-based business intelligence.

- Bot Stealth: Advanced evasion techniques, including web driver stealth, IP rotation, and privacy context rotation to avoid detection and bans.

- High Performance: Optimized for efficiency, capable of rendering hundreds of pages in parallel on a single machine without being blocked.

- Low Cost: Scrape 100,000+ browser-rendered e-commerce pages or process tens of millions of data points daily with minimal hardware requirements (8-core CPU, 32GB RAM).

- Data Quantity Assurance: Smart retry mechanisms, precise scheduling, and comprehensive web data lifecycle management.

- Large-Scale Capability: Fully distributed architecture designed for massive-scale web crawling.

- Big Data Support: Flexible backend storage options, including Local File, MongoDB, HBase, and Gora.

- Logs & Metrics: Comprehensive monitoring and detailed event logging for full transparency.

- Auto Extraction: AI-powered pattern recognition to automatically and accurately extract all fields from webpages.

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
