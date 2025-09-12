Robotic Process Automation (RPA)
=

[Prev](9event-handling.md) [Home](1home.md) [Next](11WebDriver.md)

Web data collection is very challenging, as websites often change and become increasingly complex, resulting in collected data that is usually inaccurate or incomplete.

Tools like Scrapy and requests are becoming less and less suitable for modern web pages. Against this backdrop, browser automation technology, along with RPA technology, is the solution to the problem. It replaces manual web page visits, performing the same actions as a human to collect all the data that humans can see.

Browser4 includes an RPA subsystem to implement web interactions: scrolling, typing, screen capture, mouse drag and drop, clicking, etc. This subsystem is similar to well-known technologies like selenium, playwright, and puppeteer, but optimizes all actions, such as more realistic simulation operations, better execution performance, better parallelism, better fault tolerance, and so on.

The most complex data collection projects often require complex interactions with web pages. For this, we provide a simple yet powerful API. The following is a typical RPA code snippet that is **essential** for collecting data from top e-commerce websites.

```kotlin
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver ->
    // Warm up the browser to avoid being blocked by the website,
    // or choose global settings, such as your location.
    warmUpBrowser(page, driver)
}
event.onWillFetch.addLast { page, driver ->
    // Have to visit a referrer page before we can visit the desired page.
    waitForReferrer(page, driver)
    // Websites may prevent us from opening too many pages at a time, so we should open links one by one.
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page, driver ->
    // Wait for special fields to appear on the page.
    driver.waitForSelector("body h1[itemprop=name]")
    // Close the mask layer, which might be promotions, ads, or something else.
    driver.click(".mask-layer-close-button")
}
// Visit the URL and trigger events.
session.load(url, options)
```

In the next chapter, we will delve into how to use WebDriver for complex interactions with web pages.

------

[Prev](9event-handling.md) [Home](1home.md) [Next](11WebDriver.md)
