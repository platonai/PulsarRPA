连续采集
=

在 Browser4 中，连续采集非常简单，只需要将链接提交到 UrlPool 就行了，采集循环会自动启动。而 Browser4 的基础设施也会去确保数据质量、调度质量等核心问题。

在小规模的数据采集项目中，譬如每天监控竞争对手的数百个产品价格、库存状态、新增评论等，可以使用连续采集。

连续采集可以从下面的代码开始：

```kotlin
fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // do something wonderful with the document
        println(document.title + "\t|\t" + document.baseUri)

        // extract more links from the document
        context.submitAll(document.selectHyperlinks("a[href~=/dp/]"))
    }

    val urls = LinkExtractors.fromResource("seeds10.txt").map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls).await()
}
```

------

[上一章](7Kotlin-style-async.md) [目录](1home.md) [下一章](9event-handling.md)
