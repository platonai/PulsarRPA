Continuous Crawling
=

In PulsarRPA, continuous crawling is very simple; you just need to submit links to the UrlPool, and the crawling loop will start automatically. PulsarRPA's infrastructure also ensures core issues such as data quality and scheduling quality.

For small-scale data collection projects, such as monitoring hundreds of competitor product prices, inventory status, and new reviews every day, continuous crawling can be used.

You can start continuous crawling with the following code:

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

[Prev](7Kotlin-style-async.md) [Home](1home.md) [Next](9event-handling.md)