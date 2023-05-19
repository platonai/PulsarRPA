机器人流程自动化(RPA)
=

网络数据采集非常困难，网站经常变化并且变得越来越复杂，采集的数据通常不准确或不完整。

Scrapy, requests 等，已经越来越不适应现代网页了。在这个背景下，浏览器自动化技术，以及 RPA 技术就是解决问题的良方，它代替人工访问网页，执行和人工同样的动作，采集人能够看到的一切数据。

PulsarRPA 包含一个 RPA 子系统，来实现网页交互：滚动、打字、屏幕捕获、鼠标拖放、点击等。该子系统和大家所熟知的 selenium, playwright, puppeteer 是类似的，但对所有行为进行了优化，譬如更真实的模拟操作，更好的执行性能，更好的并行性，更好的容错处理，等等。

最复杂的数据采集项目往往需要和网页进行复杂交互，为此我们提供了简洁强大的 API。以下是一个典型的 RPA 代码片段，它是从顶级电子商务网站收集数据所**必需**的。

```kotlin
val options = session.options(args)
val event = options.event.browseEvent
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

在下一章，我们将深入介绍如何使用 WebDriver 来同网页进行复杂交互。

------

[Prev](9event-handling.md) [Home](1home.md) [Next](11WebDriver.md)
