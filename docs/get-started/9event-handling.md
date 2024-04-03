Event Handling
=

The event handling mechanism provides a way to capture and process events throughout the entire lifecycle of a web page. The following simple program prints the order of all page events and their names.

Web page event handlers are defined by `PageEvent` and are divided into three categories to handle events at different stages:

1. `CrawlEvent` - Handles events in the crawl loop
2. `LoadEvent` - Handles events in the loading and parsing process
3. `BrowseEvent` - Handles events during the web page browsing stage, such as interacting with the browser to trigger the loading or display of related fields

```kotlin
class PrintFlowEvent: DefaultPageEvent() {
    private val sequencer = AtomicInteger()
    private val seq get() = sequencer.incrementAndGet()

    init {
        loadEvent.apply {
            onNormalize.addLast { url ->
                println("$seq. load - onNormalize")
                url
            }
            onWillLoad.addLast { url ->
                println("$seq. load - onWillLoad")
                null
            }
            onWillFetch.addLast { page ->
                println("$seq. load - onWillFetch")
            }
            onFetched.addLast { page ->
                println("$seq. load - onFetched")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. load - onWillParseHTMLDocument")
            }
            onWillExtractData.addLast { page ->
                println("$seq. load - onWillExtractData")
            }
            onDataExtracted.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. load - onDataExtracted")
            }
            onHTMLDocumentParsed.addLast { page: WebPage, document: FeaturedDocument ->
                println("$seq. load - onHTMLDocumentParsed")
            }
            onParsed.addLast { page ->
                println("$seq. load - onParsed")
            }
            onLoaded.addLast { page ->
                println("$seq. load - onLoaded")
            }
        }

        browseEvent.apply {
            onWillLaunchBrowser.addLast { page ->
                println("$seq. browse - onWillLaunchBrowser")
            }
            onBrowserLaunched.addLast { page, driver ->
                println("$seq. browse - onBrowserLaunched")
            }
            onWillNavigate.addLast { page, driver ->
                println("$seq. browse - onWillNavigate")
            }
            onWillInteract.addLast { page, driver ->
                println("$seq. browse - onWillInteract")
            }
            onWillCheckDocumentState.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillCheckDocumentState")
            }
            onDocumentActuallyReady.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onDocumentActuallyReady")
            }
            onWillComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillComputeFeature")
            }
            onFeatureComputed.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onFeatureComputed")
            }
            onDidInteract.addLast { page, driver ->
                println("$seq. browse - onWillInteract")
            }
            onNavigated.addLast { page, driver ->
                println("$seq. browse - onNavigated")
            }
            onWillStopTab.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillStopTab")
            }
            onTabStopped.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onTabStopped")
            }
        }

        crawlEvent.apply {
            onWillLoad.addLast { url: UrlAware ->
                println("$seq. crawl - onWillLoad")
                url
            }
            onLoad.addLast { url: UrlAware ->
                println("$seq. crawl - onLoad")
                url
            }
            onLoaded.addLast { url, page ->
                println("$seq. crawl - onLoaded")
            }
        }
    }
}
```

And the entry point for the call:

```kotlin
/**
 * Demonstrates how to use event handlers.
 * */
fun main() {
    val url = "https://www.amazon.com/dp/B0C1H26C46"
    val session = PulsarContexts.createSession()
    val link = ListenableHyperlink(url, args = "-refresh -parse", event = PrintFlowEvent())
    session.load(link)
}
```

The example program outputs the following:

```
1. crawl - onWillLoad
2. load - onNormalize
3. load - onWillLoad
4. load - onWillFetch
5. browse - onWillLaunchBrowser
6. browse - onBrowserLaunched
7. browse - onWillNavigate
8. browse - onNavigated
9. browse - onWillNavigate
10. browse - onWillCheckDocumentState
11. browse - onDocumentActuallyReady
12. browse - onWillComputeFeature
13. browse - onFeatureComputed
14. browse - onWillInteract
15. browse - onWillStopTab
16. browse - onTabStopped
17. load - onFetched
18. load - onWillParseHTMLDocument
19. load - onHTMLDocumentParsed
20. load - onParsed
21. load - onLoaded
22. crawl - onLoaded
```

------

[Prev](8continuous-crawling.md) [Home](1home.md) [Next](10RPA.md)