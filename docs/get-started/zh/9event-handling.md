事件处理
=

事件处理机制提供了一种方法，可以在网页的整个生命周期中捕获和处理事件。下面的简单程序，打印所有页面事件的调用顺序和事件名称。

网页事件处理器由 PageEvent 定义，并被分成三个类别，以处理三个不同阶段的事件：

1. CrawlEvent - 处理在 crawl loop 中的事件
2. LoadEvent - 处理在加载、解析流程中的事件
3. BrowseEvent - 处理在网页浏览阶段的事件，譬如和浏览器交互，以触发相关字段被加载或者显示

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

以及调用入口：

```kotlin
/**
 * Demonstrates how to use event handlers.
 * */
fun main() {
    val url = "https://www.amazon.com/dp/B09V3KXJPB"
    val session = PulsarContexts.createSession()
    val link = ListenableHyperlink(url, args = "-refresh -parse", event = PrintFlowEvent())
    session.load(link)
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

[上一章](8continuous-crawling.md) [目录](1catalogue.md) [下一章](10RPA.md)
