事件处理
=

事件处理机制为捕捉和处理网页整个生命周期中的事件提供了途径。以下简洁的程序记录了所有页面事件的顺序及其名称。
页面事件处理程序被包含在`PageEventHandlers`类中，分为三个不同的组别，以管理页面事件的各个阶段：

1. `CrawlEventHandlers` - 处理在爬取迭代中发生的事件。
2. `LoadEventHandlers` - 管理与页面加载和解析相关的事件。
3. `BrowseEventHandlers` - 控制交互式浏览阶段发生的事件，包括促使加载或显示相关元素的动作。

```kotlin

/**
 * Print the call sequence and the event name of all page event handlers
 * */
class PrintFlowEventHandlers: DefaultPageEventHandlers() {
    private val sequencer = AtomicInteger()
    private val seq get() = sequencer.incrementAndGet()
    
    init {
        loadEventHandlers.apply {
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
            onWillParse.addLast { page ->
                println("$seq. load - onWillParse")
            }
            onWillParseHTMLDocument.addLast { page ->
                println("$seq. load - onWillParseHTMLDocument")
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
        
        browseEventHandlers.apply {
            onWillLaunchBrowser.addLast { page ->
                println("$seq. browse - onWillLaunchBrowser")
            }
            onBrowserLaunched.addLast { page, driver ->
                println("$seq. browse - onBrowserLaunched")
            }
            onWillNavigate.addLast { page, driver ->
                println("$seq. browse - onWillNavigate")
            }
            onNavigated.addLast { page, driver ->
                println("$seq. browse - onNavigated")
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
            onWillScroll.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillScroll")
            }
            onDidScroll.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onDidScroll")
            }
            onDocumentSteady.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onDocumentSteady")
            }
            onWillComputeFeature.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillComputeFeature")
            }
            onFeatureComputed.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onFeatureComputed")
            }
            onDidInteract.addLast { page, driver ->
                println("$seq. browse - onDidInteract")
            }
            onWillStopTab.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onWillStopTab")
            }
            onTabStopped.addLast { page: WebPage, driver: WebDriver ->
                println("$seq. browse - onTabStopped")
            }
        }
        
        crawlEventHandlers.apply {
            onWillLoad.addLast { url: UrlAware ->
                println("$seq. crawl - onWillLoad")
                url
            }
            onLoaded.addLast { url, page ->
                println("$seq. crawl - onLoaded")
            }
        }
    }
}
```

以及调用入口：

```kotlin
/**
 * Demonstrates how to use event handlers.
 * */
fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    
    val url = "https://www.amazon.com/dp/B0FFTT2J6N"
    val session = PulsarContexts.createSession()
    val link = ListenableHyperlink(url, "", args = "-refresh -parse", event = PrintFlowEventHandlers())
    
    // submit the link to the fetch pool.
    session.submit(link)
    
    // wait until all done.
    PulsarContexts.await()
}
```

该示例程序输出如下：

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
12. browse - onWillScroll
13. browse - onDidScroll
14. browse - onDocumentSteady
15. browse - onWillComputeFeature
16. browse - onFeatureComputed
17. browse - onDidInteract
18. browse - onWillStopTab
19. browse - onTabStopped
20. load - onFetched
21. load - onWillParse
22. load - onWillParseHTMLDocument
23. load - onHTMLDocumentParsed
24. load - onParsed
25. load - onLoaded
26. crawl - onLoaded
```

------

[上一章](8continuous-crawling.md) [目录](1home.md) [下一章](10RPA.md)
