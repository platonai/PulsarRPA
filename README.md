# 🤖 Browser4

[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/browser4?style=flat-square)](https://hub.docker.com/r/galaxyeye88/browser4)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/browser4/blob/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.8-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

---

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/Browser4)

<!-- TOC -->
**Table of Contents**
- [🤖 Browser4](#-browser4)
    - [🌟 Introduction](#-introduction)
        - [✨ Key Capabilities](#-key-capabilities)
    - [🎥 Demo Videos](#-demo-videos)
    - [🚀 Quick Start](#-quick-start)
    - [Usage Examples](#usage-examples)
        - [Browser Agents](#browser-agents)
        - [Workflow](#workflow)
        - [LLM + X-SQL](#llm--x-sql)
        - [Native API](#native-api)
    - [Modules Overview](#modules-overview)
    - [📜 Documents](#-documents)
    - [🔧 Proxies - Unblock Websites](#-proxies---unblock-websites)
    - [Features](#features)
    - [🤝 Support & Community](#-support--community)
<!-- /TOC -->


## 🌟 Introduction

💖 **Browser4: a lightning-fast, coroutine-safe browser engine for AI automation** 💖

### ✨ Key Capabilities

* 👽 **Browser Agents** — Autonomous agents that reason, plan, and act within the browser.
* 🤖 **Browser Automation** — High-performance automation for workflows, navigation, and data extraction.
* ⚡ **Extreme Performance** — Fully coroutine-safe; supports 100k+ page visits per machine per day.
* 🧠 **Web Understanding** — Deep understanding of dynamic, script-driven, and interactive web pages.
* 📊 **Data Extraction APIs** — Robust APIs for extracting structured data with minimal effort.

---

## 🎥 Demo Videos

🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)

---

## 🚀 Quick Start

1. Edit [application.properties](application.properties) to add your LLM API key
2. Run any examples in module `pulsar-examples`

---

## Usage Examples

### Browser Agents
```kotlin
val agent = AgenticContexts.getOrCreateAgent()

val problem = """
    1. go to amazon.com
    2. search for pens to draw on whiteboards
    3. compare the first 4 ones
    4. write the result to a markdown file
    """

agent.resolve(problem)
```

### Workflow
Low-level browser automation & data extractions.

1. direct and full CDP control
2. precise element interactions
3. fast data extractions

```kotlin
    val session = AgenticContexts.getOrCreateSession()
    val agent = session.companionAgent
    val driver = session.getOrCreateBoundDriver()
    var page = session.open(url)
    var document = session.parse(page)
    var fields = session.extract(document, mapOf("title" to "#title"))
    var result = agent.act("search for 'browser'")
    var content = driver.selectFirstTextOrNull("body")
    content = driver.selectFirstTextOrNull("body")
    result = agent.resolve("search for 'web scraping', read each result and give me a summary")
    page = session.capture(driver)
    document = session.parse(page)
    fields = session.extract(document, mapOf("title" to "#title"))
```

### LLM + X-SQL (10x entities & 100x fields)
X-SQL is suited for high-complexity data-extraction pipelines, including cases with
multiple-dozen entities and several hundred fields per entity.
```kotlin
val context = AgenticContexts.create()
val sql = """
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_text(dom, '#bylineInfo') as brand,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
  dom_first_text(dom, '#acrCustomerReviewText') as ratings,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB -i 1s -njr 3', 'body');
"""
val rs = context.executeQuery(sql)
println(ResultSetFormatter(rs, withHeader = true))
```

### High-speed parallel browser control (100k+ page visits per machine per day)
High-speed parallel scraping & browser control examples are shown below (see advanced sections for more).
```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val blockingUrls = listOf("*.png", "*.jpg")
val links = LinkExtractors.fromResource("urls.txt")
    .map { ListenableHyperlink(it, "", args = args) }
    .onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }

session.submitAll(links)
```
---

## Modules Overview
| Module | Description                                             |
|--------|----------------------------------------------------------------|
| `pulsar-core` | Core engine: sessions, scheduling, DOM, browser control |
| `pulsar-rest` | Spring Boot REST layer & command endpoints              |
| `pulsar-client` | Client SDK / CLI utilities                             |
| `browser4-spa` | Browser4 API for agents with Single Page Application    |
| `browser4-crawler` | Browser4 API for crawler & product packaging        |
| `pulsar-tests` | Heavy integration & scenario tests                      |
| `pulsar-tests-common` | Shared test utilities & fixtures                 |

---

## 📜 Documents

* 📖 [REST API Examples](docs/rest-api-examples.md)
* 🛠️ [LLM Configuration Guide](docs/config/llm/llm-config.md)
* 🛠️ [Configuration Guide](docs/config.md)
* 📚 [Build from Source](docs/development/build.md)
* 🧠 [Expert Guide](docs/advanced-guides.md)

---

## 🔧 Proxies - Unblock Websites

<details>

Set the environment variable PROXY_ROTATION_URL to the URL provided by your proxy service:

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

Each time the rotation URL is accessed, it should return a response containing one or more fresh proxy IPs.
Ask your proxy provider for such a URL.

</details>

---

## Features

### AI & Agents
- Problem-solving autonomous browser agents
- Parallel agent sessions
- LLM-assisted page understanding & extraction

### Browser Automation & RPA
- Workflow-based browser actions
- Precise coroutine-safe control (scroll, click, extract)
- Flexible event handlers & lifecycle management

### Data Extraction & Query
- One-line data extraction commands
- X-SQL extended query language for DOM/content
- Structured + unstructured hybrid extraction (LLM + selectors)

### Performance & Scalability
- High-efficiency parallel page rendering
- Block-resistant design & smart retries
- 100,000+ pages/day on modest hardware (indicative)

### Stealth & Reliability
- Advanced anti-bot techniques
- IP & profile rotation
- Resilient scheduling & quality assurance

### Developer Experience
- Simple API integration (REST, native, text commands)
- Rich configuration layering
- Clear structured logging & metrics

### Storage & Monitoring
- Local FS & MongoDB support (extensible)
- Comprehensive logs & transparency
- Detailed metrics & lifecycle visibility

---

## 🤝 Support & Community

- 💬 WeChat: galaxyeye
- 🌐 Weibo: [galaxyeye](https://weibo.com/galaxyeye)
- 📧 Email: galaxyeye@live.cn, ivincent.zhang@gmail.com
- 🐦 Twitter: galaxyeye8
- 🌍 Website: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="WeChat QR Code" />
</div>

---

> For Chinese documentation see [简体中文 README](README-CN.md).
