## Playwright Java is not thread safe!!

Playwright Java is not thread safe, i.e. all its methods as well as methods on all objects created by it 
(such as BrowserContext, Browser, Page etc.) are expected to be called on the same thread where the 
Playwright object was created or proper synchronization should be implemented to ensure only one thread calls 
Playwright methods at any given time. 
Having said that it's okay to create multiple Playwright instances each on its own thread.

See also:

* https://github.com/DennisOchulor/playwright-java-multithread

## Object doesn't exist: request@496xxx

这个错误：

> **`Object doesn't exist: request@496xxx`**

是 Playwright 在 Java/Kotlin 中进行**高并发或跨线程访问对象**时常见的问题，通常意味着：

### ❗ Playwright 的某个对象（如 `Page`、`Request` 等）被：
- 在不再有效的上下文中访问（例如关闭的页面 / 浏览器）
- 被多个线程同时访问，而 Playwright 并 **不线程安全**
- 被意外清理或释放，尤其是在多线程 + PersistentContext 中更容易复现

---

## ✅ 原因分析

### 1. **跨线程访问 Page 对象**
Playwright Java/Kotlin 的所有对象（`Page`, `BrowserContext`, `Response`, `Request` 等）都**不允许跨线程访问**！

你不能在线程 A 创建的 `Page`，在线程 B 中操作它，即使你用了 `synchronized(page)`。

---

### 2. **线程池执行太快 / context 或 page 已关闭**
- 如果 `Page` 被提前关闭（或其所属的 `BrowserContext` 被关闭），它引用的对象就会失效。
- 你在 navigate 的同时，如果别的线程正在清理资源，也会导致对象不存在。

---

## ✅ 正确的解决方式（推荐改进）：

### ✅ 1. 拆分线程池，每个线程**只操作一个 context 内的页面**

Playwright 对象需要在**线程内保持一致**。最稳定的方式是：

- 每个 `PersistentContext` 和它的 `Page` 都被一个固定线程操作
- 所有测试 URL 分发到这些线程自己维护的标签页上

---

### ✅ 2. 使用 `BlockingQueue<Page>` 做 Page 池（可借还）

通过线程安全的队列借用 `Page`，避免共享访问：

```kotlin
val pagePool = LinkedBlockingQueue<Page>()

// 获取 page（借）
val page = pagePool.take()

// 使用完后放回池（还）
pagePool.put(page)
```

每个线程从池中**借一个 page**来访问，避免多线程并发冲突。

---

### ✅ 3. 避免 navigate 与 close 同时执行

如果你在测试中加了 `context.close()` 或 `page.close()`，一定要保证没有其他线程正在用这些对象！

---

## ✅ 重新设计（推荐结构）

- 每个线程拥有：
    - 1 个 PersistentContext
    - N 个页面（Page）在本线程中循环使用
- 所有 URL 被分块后分配给线程执行
- 页面仅在创建线程中使用，不跨线程

---

## 🔧 最小修复建议

如果你想快速测试一版，可以加以下代码规避错误：

```kotlin
synchronized(page) {
    try {
        if (page.isClosed) return@submit
        page.navigate(url, Page.NavigateOptions().setTimeout(10_000.0))
    } catch (e: Exception) {
        println("❌ Failed: $url on page ${page.hashCode()} - ${e.message}")
    }
}
```

不过建议还是用线程隔离来彻底解决。要不要我帮你改成线程-上下文绑定的结构？稳定性和性能会提升很多。