package ai.platon.pulsar.manual

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts.createSession
import java.util.concurrent.CompletableFuture

/**
 * # Java-Style Async Operations - CompletableFuture Integration
 *
 * This example demonstrates how to use Browser4 with Java's CompletableFuture API,
 * which is useful for Java codebases or when explicit future handling is needed.
 *
 * ## Async Patterns Covered:
 * 1. **Parallel Stream Processing** - Using Java 8 parallel streams
 * 2. **CompletableFuture Chains** - Async operations with thenApply/thenAccept
 * 3. **Batch Async Loading** - loadAllAsync() for multiple URLs
 * 4. **Future Composition** - Combining multiple futures
 *
 * ## When to Use Java Async vs Kotlin Coroutines:
 * - **Java Async (CompletableFuture)**:
 *   - Java codebases that can't use Kotlin coroutines
 *   - When you need to integrate with Java libraries
 *   - When you prefer explicit future handling
 * - **Kotlin Coroutines** (see _4_Coroutine.kt):
 *   - Kotlin codebases
 *   - Simpler sequential-looking async code
 *   - Better for complex async workflows
 *
 * ## AI Integration Notes:
 * - CompletableFuture chains can include AI analysis steps
 * - Use thenApply() to transform data between stages
 * - Use CompletableFuture.allOf() to wait for all futures
 *
 * @see CompletableFuture Java's async primitive
 * @see LinkExtractors Utility for loading URLs from resources
 */
internal object JvmAsync {
    // Create a session at object initialization
    // AI Note: Session can be reused across multiple async operations
    val session = createSession()

    /**
     * Pattern 1: Parallel Stream Processing
     *
     * Uses Java 8 parallel streams for concurrent page loading.
     * Each stream element is processed in parallel by the ForkJoinPool.
     *
     * AI Note: parallelStream() automatically parallelizes the operations,
     * but may not be optimal for I/O-bound tasks like web scraping.
     */
    fun loadAll() {
        LinkExtractors.fromResource("seeds10.txt").parallelStream()
            .map(session::load)          // Load each URL
            .map(session::parse)         // Parse to FeaturedDocument
            .map(FeaturedDocument::guessTitle)  // Extract title
            .forEach { println(it) }     // Print results
    }

    /**
     * Pattern 2: Explicit CompletableFuture Chains
     *
     * Demonstrates building async pipelines with CompletableFuture.
     * Each thenApply/thenAccept creates a new stage in the pipeline.
     *
     * AI Note: This pattern gives you explicit control over the async flow.
     * You can add error handling with exceptionally() if needed.
     */
    fun loadAllAsync2() {
        val futures = LinkExtractors.fromResource("seeds10.txt")
            .asSequence()
            .map { "$it -i 1d" }                    // Add expiration option
            .map { session.loadAsync(it) }          // Start async load (returns CompletableFuture<WebPage>)
            .map { it.thenApply { session.parse(it) } }  // Chain: parse when loaded
            .map { it.thenApply { it.guessTitle() } }    // Chain: extract title
            .map { it.thenAccept { println(it) } }       // Chain: print result
            .toList()
            .toTypedArray()
        // Wait for all futures to complete
        CompletableFuture.allOf(*futures).join()
    }

    /**
     * Pattern 3: Batch Async Loading
     *
     * Uses loadAllAsync() to start loading multiple URLs concurrently.
     * Returns a list of CompletableFuture<WebPage> that can be chained.
     *
     * AI Note: loadAllAsync() is more efficient than individual loadAsync()
     * calls as it can batch operations internally.
     */
    fun loadAllAsync3() {
        val futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt"))
            .map { it.thenApply { session.parse(it) } }
            .map { it.thenApply { it.guessTitle() } }
            .map { it.thenAccept { println(it) } }
            .toTypedArray()
        CompletableFuture.allOf(*futures).join()
    }

    /**
     * Pattern 4: Compact Chain Syntax
     *
     * Same as Pattern 3 but with chained operations in a single expression.
     * More concise but potentially harder to debug.
     *
     * AI Note: This is the most compact form. Choose based on readability needs.
     */
    fun loadAllAsync4() {
        val futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt"))
            .map { it.thenApply { session.parse(it) }.thenApply { it.guessTitle() }.thenAccept { println(it) } }
            .toTypedArray()
        CompletableFuture.allOf(*futures).join()
    }
}

/**
 * Main entry point demonstrating all Java async patterns.
 *
 * AI Note: Each pattern achieves the same result (loading and printing page titles)
 * but with different trade-offs in code clarity and control.
 */
fun main() {
    // Use the default browser which has an isolated profile.
    // You can also try other browsers, such as system default, prototype, sequential, temporary, etc.
    PulsarSettings.withDefaultBrowser()

    // Run all patterns sequentially for demonstration
    JvmAsync.loadAll()        // Pattern 1: Parallel streams
    JvmAsync.loadAllAsync2()  // Pattern 2: Explicit futures
    JvmAsync.loadAllAsync3()  // Pattern 3: Batch loading
    JvmAsync.loadAllAsync4()  // Pattern 4: Compact syntax
}
