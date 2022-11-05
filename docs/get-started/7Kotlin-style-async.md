Kotlin风格异步编程
=

Kotlin 处理异步代码的方法是使用协程，协程是可暂停计算的，即函数可以在某个点暂停执行，稍后再继续执行。

协程的一个好处是，对于开发人员来说，编写非阻塞代码与编写阻塞代码本质上是一样的，编程模型本身并没有真正改变。

下述代码演示了如何通过协程来并行加载网页：

```kotlin
val jobs = LinkExtractors.fromResource("seeds10.txt")
    .map { scope.launch { session.loadDeferred("$it -expires 1s").also { println(it.url) } } }
jobs.joinAll()
```

或者全部加载完成后批处理：

```kotlin
val deferredPages = LinkedBlockingQueue<Deferred<WebPage>>()
val jobs = LinkExtractors.fromResource("seeds10.txt")
    .map { scope.launch { async { session.loadDeferred(it) }.also(deferredPages::add) } }

// suspends current coroutine until all given jobs are complete.
jobs.joinAll()
deferredPages.map { it.await() }.forEach { println(it.url) }
```

------

[上一章](6Java-style-async.md) [目录](1catalogue.md) [下一章](8continuous-crawling.md)
