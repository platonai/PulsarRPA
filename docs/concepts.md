# PulsarRPA Concepts

English | [简体中文](concepts-CN.adoc)

This document describes the concepts of PulsarRPA.

## Core Concepts

### Web Scraping
Web scraping, web harvesting, or web data extraction is the process of extracting data from websites.

### Network As A Database
The Internet, especially the World Wide Web (WWW), is the largest database in the world. However, extracting data from the Web has never been easy.

PulsarRPA treats the network as a database. For each web page, PulsarRPA first checks the local storage. If it does not exist, has expired, or any other fetch condition is triggered, Pulsar retrieves it from the Web.

PulsarRPA has SQL support, so we can turn the Web into tables and charts using simple SQL queries, and we can also query the Web using SQL directly.

### Auto Extract
Web data extraction has evolved over the last three decades from using statistical methods to more advanced machine learning methods. Machine learning techniques are much preferred nowadays due to demand for time-to-market, developer productivity, and cost concerns. Using our cutting-edge technology, the entire end-to-end Web data extraction lifecycle is automated without any manual intervention.

We provided a preview project to show how to use our world-leading machine learning algorithm to extract almost every field in web pages automatically: PulsarRPAPro.

You can also use PulsarRPAPro as an intelligent CSS selector generator. The generated CSS selectors can be used in any traditional scraping systems to extract data from web pages.

### Browser Rendering
Although PulsarRPA supports various web scraping methods, browser rendering is the primary way PulsarRPA scrapes web pages.

Browser rendering means every web page is opened by a real browser to ensure all fields on the web page are present correctly.

### Pulsar Context
PulsarContext consists of a set of highly customizable components that provide the core set of interfaces of the system and is used to create PulsarSession.

The PulsarContext is the interface to all other contexts.

A StaticPulsarContext consists of the default components.

A ClassPathXmlPulsarContext consists of components which are customized using Spring bean configuration files.

A SQLContext consists of components to support X-SQL.

Programmers can write their own Pulsar Contexts to extend the system.












# Pulsar Session

PulsarSession defines an interface to load web pages from local storage or fetch from the Internet, as well as methods for parsing, extracting, saving, indexing, and exporting web pages.

Key methods:

- `.load()`: load a web page from local storage, or fetch it from the Internet.
- `.parse()`: parse a web page into a document.
- `.scrape()`: load a web page, parse it into a document, and then extract fields from the document.
- `.submit()`: submit a URL to the URL pool, which will be processed in the main loop later.

And also the batch versions:

- `.loadOutPages()`: load the portal page and out pages.
- `.scrapeOutPages()`: load the portal page and out pages, extract fields from out pages.

For a comprehensive understanding of all methods, please refer to PulsarSession.

The first step is to understand how to load a page. Load methods like `session.load()` first check the local storage and return the local version if the required page exists and meets the requirements; otherwise, it will be fetched from the Internet.

The `load parameters` or `load options` can be used to specify when the system will fetch a web page from the Internet:

- Expiration
- Force refresh
- Page size
- Required fields
- Other conditions

```kotlin
val session = PulsarContexts.createSession()
val url = "..."
val page = session.load(url, "-expires 1d")
val document = session.parse(page)
val document2 = session.loadDocument(url, "-expires 1d")
val pages = session.loadOutPages(url, "-outLink a[href~=item] -expires 1d -itemExpires 7d -itemRequireSize 300000")
val fields = session.scrape(url, "-expires 1d", "li[data-sku]", listOf(".p-name em", ".p-price"))
// ...
```

Once a page is loaded from local storage, or fetched from the Internet, the next processing steps are:

1. Parse the page content into an HTML document.
2. Extract fields from the HTML document.
3. Write the fields into a destination, such as:
    - Plain file, Avro file, CSV, Excel, MongoDB, MySQL, etc.
    - Solr, Elastic, etc.

There are various ways to fetch the page content from the Internet:

- Through the HTTP protocol.
- Through a real browser.

As webpages become increasingly complex, fetching webpages through real browsers is the primary method used today.

When fetching webpages using a real browser, we may need to interact with the pages to ensure the desired fields are loaded correctly and completely. To achieve this, activate PageEvent and use WebDriver.

```kotlin
val options = session.options(args)
options.event.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
  driver.scrollDown()
}
session.load(url, options)
```

WebDriver provides a complete set of methods for RPA, similar to selenium, playwright, and puppeteer. All actions and behaviors are optimized to mimic real people as closely as possible.

## Web Driver

WebDriver defines a concise interface to visit and interact with web pages, with all actions and behaviors optimized to mimic real people as closely as possible, such as scrolling, clicking, typing text, dragging and dropping, etc.

The methods in this interface fall into three categories:

- Control of the browser itself.
- Selection of elements, extracting textContent and attributes.
- Interaction with the webpage.

Key methods:

- `.navigateTo()`: load a new webpage.
- `.scrollDown()`: scroll down on a webpage to fully load the page. Most modern webpages support lazy loading using AJAX technology, where the page content only starts to load when it is scrolled into view.
- `.pageSource()`: retrieve the source code of a webpage.

## URLs

A Uniform Resource Locator (URL), commonly referred to as a web address, is a reference to a web resource that specifies its location on a computer network and a mechanism for retrieving it.

A URL in PulsarRPA is a normal URL with extra information to describe a task. Every task in PulsarRPA is defined as some form of URL.

There are several basic forms of URLs in PulsarRPA:

- A NormURL.
- A String.
- A UrlAware.
- A DegenerateUrl.

NormURL stands for 'normal url', which means the URL is in its final form and is usually passed to a real browser eventually.

If not specified, a URL in string format is actually a 'configured url', or 'a URL with arguments'. For example:

```kotlin
val url = "https://www.amazon.com/dp/B10000  -taskName amazon -expires 1d -ignoreFailure"
session.load(url)
```

The above code has the same meaning as the following code:

```kotlin
val url = "https://www.amazon.com/dp/B10000"
val args = "-taskName amazon -expires 1d -ignoreFailure"
session.load(url, args)
```

A UrlAware provides much more complex controls for crawl tasks. UrlAware is the interface of all Hyperlinks; for more details, see the Hyperlinks section.

Finally, a DegenerateUrl is not actually a URL; it's an interface for any task to be executed in the crawl loop.










# Browser Settings

BrowserSettings provides a convenient interface to define the behavior of browser automation, such as:

- Headed or headless?
- Single Page Application (SPA) or not?
- Enable proxy IPs or not?
- Block media resources or not?

For more details, refer to BrowserSettings.

# Event Handler

Event handlers here refer to webpage event handlers that capture and process events throughout the lifecycle of webpages.

For information on all available event handlers, refer to EventHandlerUsage.

# X-SQL

PulsarRPA supports the Network As A Database paradigm, and we have developed X-SQL to query the Web directly and convert webpages into tables and charts.

For a detailed introduction and function descriptions about X-SQL, refer to X-SQL.

# Implementation Concepts

Developers are not required to study the implementation concepts, but understanding these concepts can help us better grasp how the entire system operates.

## Url Pool

During continuous crawls, URLs are added to a UrlPool. A UrlPool contains various UrlCaches to meet different requirements, such as priority, delaying, deadline, and external loading requirements.

## Crawl Loop

During continuous crawls, a crawl loop is initiated to continuously fetch URLs from the UrlPool, and then load/fetch them asynchronously in a PulsarSession.

Remember that every task in # Browser Settings

BrowserSettings provides a convenient interface to define the behavior of browser automation, such as:

- Headed or headless?
- Single Page Application (SPA) or not?
- Enable proxy IPs or not?
- Block media resources or not?

For more details, refer to BrowserSettings.

# Event Handler

Event handlers here refer to webpage event handlers that capture and process events throughout the lifecycle of webpages.

For information on all available event handlers, refer to EventHandlerUsage.

# X-SQL

PulsarRPA supports the Network As A Database paradigm, and we have developed X-SQL to query the Web directly and convert webpages into tables and charts.

For a detailed introduction and function descriptions about X-SQL, refer to X-SQL.

# Implementation Concepts

Developers are not required to study the implementation concepts, but understanding these concepts can help us better grasp how the entire system operates.

## Url Pool

During continuous crawls, URLs are added to a UrlPool. A UrlPool contains various UrlCaches to meet different requirements, such as priority, delaying, deadline, and external loading requirements.

## Crawl Loop

During continuous crawls, a crawl loop is initiated to continuously fetch URLs from the UrlPool, and then load/fetch them asynchronously in a PulsarSession.

Remember that every task in PulsarRPA is a URL, so the crawl loop can accept and execute any type of task.

## Privacy Context

One of the biggest challenges in web scraping tasks is maintaining bot stealth. For web scraping tasks, it's crucial that the website cannot discern whether a visit is from a human or a bot. If a page visit is suspected by the website, known as a privacy leak, the privacy context must be discarded, and Pulsar will visit the page in a different privacy context.

## Proxy Management

This involves obtaining IPs from proxy vendors, recording proxy status, smart and automatic IP rotation, and more.

## Web Driver Stealth

When a browser is programmed to access a webpage, the website may detect that the visit is automated. Web Driver stealth technology is used to prevent such detection.

## Backend Storage

PulsarRPA supports a variety of backend storage solutions to meet our customers' urgent needs, such as Local File System, MongoDB, HBase, Gora, and so on. is a URL, so the crawl loop can accept and execute any type of task.

## Privacy Context

One of the biggest challenges in web scraping tasks is maintaining bot stealth. For web scraping tasks, it's crucial that the website cannot discern whether a visit is from a human or a bot. If a page visit is suspected by the website, known as a privacy leak, the privacy context must be discarded, and Pulsar will visit the page in a different privacy context.

## Proxy Management

This involves obtaining IPs from proxy vendors, recording proxy status, smart and automatic IP rotation, and more.

## Web Driver Stealth

When a browser is programmed to access a webpage, the website may detect that the visit is automated. Web Driver stealth technology is used to prevent such detection.

## Backend Storage

PulsarRPA supports a variety of backend storage solutions to meet our customers' urgent needs, such as Local File System, MongoDB, HBase, Gora, and so on.

