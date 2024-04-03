Kotlin Style Asynchronous Programming
=

Kotlin handles asynchronous code using coroutines, which are suspendable computations, meaning a function can pause at some point and then resume later.

One of the benefits of coroutines is that for developers, writing non-blocking code is essentially the same as writing blocking code; the programming model itself does not really change.

The following code demonstrates how to parallel load web pages using coroutines:

```kotlin
val jobs = LinkExtractors.fromResource("seeds10.txt")
    .map { scope.launch { session.loadDeferred("$it -expires 1s").also { println(it.url) } } }
jobs.joinAll()
```

Or batch process after all have been loaded:

```kotlin
val deferredPages = LinkedBlockingQueue<Deferred<WebPage>>()
val jobs = LinkExtractors.fromResource("seeds10.txt")
    .map { scope.launch { async { session.loadDeferred(it) }.also(deferredPages::add) } }

// suspends the current coroutine until all given jobs are complete.
jobs.joinAll()
deferredPages.map { it.await() }.forEach { println(it.url) }
```

------

[Prev](6Java-style-async.md) [Home](1home.md) [Next](8continuous-crawling.md)