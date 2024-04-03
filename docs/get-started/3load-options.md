Load Options
=

PulsarRPA uses simple loading parameters to precisely describe a collection task, such as data lifecycle, page quality requirements, task deadlines, task retries, etc. In most cases, a data collection task can be uniquely determined by URL arguments in the form of text, so it can be easily copied, managed, stored, transferred, parallelized, and used for communication.

Starting from PulsarSession:

```kotlin
// Create a pulsar session
val session = PulsarContexts.createSession()
// The main URL we are working with
val url = "https://www.amazon.com/dp/B0C1H26C46"
```

The most common requirement is to specify the page expiration time. If the required page is already stored locally and has not expired, the local version will be returned.

The option `-expires` is used to specify the page expiration time:

```kotlin
// Load a page, or fetch it if the expiry time is exceeded.
//
// Option `-expires` specifies the expiry time and has a short form `-i`.
//
// The expiry time supports both the ISO-8601 standard and the Hadoop time duration format:
// 1. ISO-8601 standard: PnDTnHnMn.nS
// 2. Hadoop time duration format: Valid units are: ns, us, ms, s, m, h, d.
var page = session.load(url, "-expires 10s")
page = session.load(url, "-i 10s")
```

A link may actually become invalid, for example, when a product is removed from an e-commerce website; however, the link may become effective again later, such as when the product is restocked. By default, PulsarRPA marks a link as "Gone" after detecting it as invalid 3 times and will no longer collect that link.

The option `-ignoreFailure` is used to ignore the "Gone" status:

```kotlin
// Add option `-ignoreFailure` to force re-fetch, ignoring all failures even if `fetchRetries` exceeds the maximum.
page = session.load(url, "-ignoreFailure -expires 0s")
```

The option `-refresh` is used to force a re-fetch, similar to clicking the "refresh" button on a real browser:

```kotlin
// Add option `-refresh` to force re-fetch, ignoring all failures and setting `fetchRetries` to 0,
// `-refresh` = `-ignoreFailure -expires 0s` and `page.fetchRetires = 0`.
page = session.load(url, "-refresh")
```

Modern web pages often contain a large amount of **lazy-loaded** content. After a page is opened, only the data for the first visible screen is actually loaded, and the rest of the data is only loaded when the page scrolls into view.

For specific websites, we usually have an estimate of the basic situation of the web page, such as page size, the number of links on the page, the number of images on the page, etc. At this time, we can use these values to determine whether a page is fully loaded. If the web page is incomplete, it needs to be re-collected.

The option `.requireSize` specifies the minimum size of the page. If the actual collected size is smaller than this value, it needs to be re-collected.

The option `.requireImages` specifies the minimum number of images on the page. If the actual number of collected images is smaller than this value, it needs to be re-collected.

The option `.requireAnchors` specifies the minimum number of links on the page. If the actual number of collected links is smaller than this value, it needs to be re-collected.

```kotlin
// Option `-requireSize` specifies the minimum page size. The page should be re-fetched if the last page size is smaller than that.
page = session.load(url, "-requireSize 300000")

// Option `-requireImages` specifies the minimum image count. The page should be re-fetched if the image count of the last fetched page is smaller than that.
page = session.load(url, "-requireImages 10")

// Option `-requireAnchors` specifies the minimum anchor count. The page should be re-fetched if the anchor count of the last fetched page is smaller than that.
page = session.load(url, "-requireAnchors 100")
```

Web page collection tasks usually have timeliness. For example, a task is to monitor a product once a day, and if the task is not completed by midnight, it should be discarded, and a new task responsible for the next day's collection will enter the queue.

The option `-deadline` is used to specify the deadline for the task. If the task is not completed after the deadline, the task should be discarded as soon as possible.

```kotlin
// If the deadline is exceeded, the task should be abandoned as soon as possible.
page = session.load(url, "-deadline 2022-04-15T18:36:54.941Z")
```

In large-scale collection projects, we usually do not write sequential code like load() -> parse() -> select(). Instead, we activate the **parsing subsystem** and register event handlers in the parsing subsystem to perform document-related tasks through event handlers, such as extracting fields, saving fields to the database, collecting more links, etc.

The option `-parse` is used to activate the parsing subsystem.

```kotlin
// Add option `-parse` to activate the parsing subsystem.
page = session.load(url, "-parse")
```

Web page content is crucial for data analysis. However, for various considerations such as storage costs and storage performance, we sometimes choose not to save web page content.

The option `-storeContent` is used to choose whether to save web page content.

```kotlin
// Option `-storeContent` tells the system to save the page content to storage.
page = session.load(url, "-storeContent")
```

PulsarRPA provides a comprehensive retry mechanism to ensure that the collected pages meet the analysis requirements.

Due to the increasing complexity of modern web pages, the collected pages may be incomplete:

1. Lazy-loaded content is not loaded.
2. The page is temporarily unavailable.
3. Local network temporary failure.
4. Proxy network temporary failure.
5. The page redirects to other pages, such as login/CAPTCHA, etc.
6. Other exceptions.

The option `-nMaxRetry` specifies the maximum number of retries in the crawl loop. If the retries still fail after this number, the page will be marked as "Gone". Unless specifically specified, the system will not collect lost pages.

```kotlin
// Option `-nMaxRetry` specifies the maximum number of retries in the crawl loop, and if it still fails after this number, the page will be marked as `Gone`. A retry will be triggered when a RETRY(1601) status code is returned.
page = session.load(url, "-nMaxRetry 3")
```

In some scenarios, we need to retry immediately:

```kotlin
// Option `-nJitRetry` specifies the maximum number of retries for the load phase, which will be triggered when a RETRY(1601) status is returned.
page = session.load(url, "-nJitRetry 2")
```

The following demonstrates a comprehensive example. In this example, we collect the portal page, extract links from the portal page, and then collect the linked-out pages:

```kotlin
// Load or fetch the portal page, and then load or fetch the out links selected by `-outLink`.
//
// 1. `-expires` specifies the expiry time of item pages and has a short form `-ii`.
// 2. `-outLink` specifies the cssSelector for links in the portal page to load.
// 3. `-topLinks` specifies the maximum number of links selected by `-outLink`.
//
// Fetch conditions:
// 1. `-itemExpires` specifies the expiry time of item pages and has a short form `-ii`.
// 2. `-itemRequireSize` specifies the minimal page size.
// 3. `-itemRequireImages` specifies the minimal number of images in the page.
// 4. `-itemRequireAnchors` specifies the minimal number of anchors in the page.
var pages = session.loadOutPages(url, "-expires 10s" +
        " -itemExpires 7d" +
        " -outLink a[href~=item]" +
        " -topLinks 10" +
        " -itemExpires 1d" +
        " -itemRequireSize 600000" +
        " -itemRequireImages 5" +
        " -itemRequireAnchors 50"
)
```

This course provides [complete code](../../pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_1_LoadOptions.kt), [domestic mirror](https://gitee.com/platonai_galaxyeye/PulsarRPA/blob/1.10.x/pulsar-app/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/_1_LoadOptions.kt).

------

[Prev](2basic-usage.md) [Home](1home.md) [Next](4data-extraction.md)
