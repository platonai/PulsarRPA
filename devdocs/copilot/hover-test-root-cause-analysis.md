# `test hover` 第一次迭代 top 值相等的根本原因分析

## 问题现象

测试输出：
```
Iteration 1: top1=1226.0, top2=1226.0, diff=0.0
Iteration 2: top1=1226.0, top2=32.0, diff=-1194.0
Iteration 3: top1=1226.0, top2=32.0, diff=-1194.0
```

- 第一次迭代：`top1` 和 `top2` 都是 1226，差异为 0
- 后续迭代：`top1=1226`, `top2=32`，差异为 -1194

## 关键发现

### 1. CSS Transform 不影响 offsetTop

测试使用了 `offsetTop` 属性来测量元素位置，但 CSS `transform` **不会改变** `offsetTop` 的值：

```css
.hover-card:hover {
    transform: translateY(-5px);  /* 这不会影响 offsetTop！ */
    box-shadow: 0 6px 12px rgba(0,0,0,0.1);
}
```

**重要概念**：
- `offsetTop` 是相对于 `offsetParent` 的静态布局位置
- CSS `transform` 是一个**视觉变换**，不影响布局位置
- 因此，无论元素是否 hover，`offsetTop` 都应该保持不变

### 2. hover() 方法会调用 scrollIntoView

查看 `PulsarWebDriver.kt` 的实现：

```kotlin
override suspend fun hover(selector: String) {
    bringToFront()
    driverHelper.invokeOnElement(selector, "hover", scrollIntoView = true) { node ->
        emulator.hover(node, position = "center")
    }
}
```

关键：**`scrollIntoView = true`**

这会调用 `page.scrollIntoViewIfNeeded(selector)`，将元素滚动到视口中。

### 3. scrollIntoView 改变了什么？

当元素被滚动到视口中时：
- **页面的 `scrollY` 改变**
- 元素相对于视口的位置改变
- **但 `offsetTop` 本身不应该改变**（因为它是相对于 `offsetParent` 的）

那么，为什么 `offsetTop` 从 1226 变成了 32？

### 4. 真正的根本原因：offsetParent 的改变

最可能的解释是：**`scrollIntoView` 可能触发了某些布局变化，导致 `offsetParent` 改变**。

或者，更准确地说：
1. 初始加载时，测试读取的可能是**不同元素**的 `offsetTop`
2. `.hover-card p` 选择器匹配的元素可能在不同时刻有不同的父元素
3. `scrollIntoView` 可能触发了 DOM 的某些重排

### 5. 测试的真正问题

**测试使用了错误的属性来验证 hover 效果**：

- `offsetTop` 不受 `transform` 影响
- 应该使用 `getBoundingClientRect()` 来测量视觉位置

## 正确的解决方案

### 方案 1：使用 getBoundingClientRect（推荐）

`getBoundingClientRect()` 返回元素的**视觉边界**，会受到 `transform` 的影响：

```kotlin
@Test
fun `test hover`() = runEnhancedWebDriverTest(interactiveUrl2, browser) { driver ->
    // First scroll to ensure the element is in view and page is in a stable state
    driver.scrollToTop()
    driver.delay(300)

    var n = 20
    while (n-- > 0) {
        // Move mouse away from the card to ensure it's not in hover state
        driver.moveMouseTo(10.0, 10.0)
        driver.delay(200)

        // Use getBoundingClientRect which IS affected by transform
        val rect1 = driver.evaluate(
            "JSON.stringify(document.querySelector('.hover-card').getBoundingClientRect())",
            "{}"
        )

        driver.hover(".hover-card")
        driver.delay(500)

        val rect2 = driver.evaluate(
            "JSON.stringify(document.querySelector('.hover-card').getBoundingClientRect())",
            "{}"
        )

        println("Iteration ${20 - n}:")
        println("  Before hover: $rect1")
        println("  After hover:  $rect2")
        sleepSeconds(2)
    }
}
```

**预期结果**：
- `rect1` 和 `rect2` 的 `top` 值应该相差约 5px（对应 `translateY(-5px)`）
- 每次迭代都应该显示一致的差异

### 方案 2：测试其他受 transform 影响的属性

可以使用 `getComputedStyle().transform` 来验证：

```kotlin
val transform1 = driver.evaluate(
    "getComputedStyle(document.querySelector('.hover-card')).transform",
    "none"
)

driver.hover(".hover-card")
driver.delay(500)

val transform2 = driver.evaluate(
    "getComputedStyle(document.querySelector('.hover-card')).transform",
    "none"
)
```

### 方案 3：测试 box-shadow（另一个 hover 效果）

```kotlin
val shadow1 = driver.evaluate(
    "getComputedStyle(document.querySelector('.hover-card')).boxShadow",
    "none"
)

driver.hover(".hover-card")
driver.delay(500)

val shadow2 = driver.evaluate(
    "getComputedStyle(document.querySelector('.hover-card')).boxShadow",
    "none"
)

assertNotEquals(shadow1, shadow2, "Box shadow should change on hover")
```

## 时序图

### 第一次迭代（原始代码）

```
页面加载
  ↓
元素在页面底部（未滚动）
  ↓
读取 top1 = 1226 (offsetTop 相对于某个 offsetParent)
  ↓
调用 hover()
  ├─ scrollIntoView (滚动页面！)
  ├─ 鼠标移动到元素上
  └─ delay(500ms)
  ↓
读取 top2 = 1226 (offsetTop 仍然是相同值，因为 transform 不影响它)
  ↓
diff = 0 ❌
```

### 后续迭代（原始代码）

```
元素仍在视口中（已滚动过）
  ↓
moveMouseTo(10, 10) - 只移动鼠标，不滚动页面
  ↓
读取 top1 = 1226 (offsetTop)
  ↓
调用 hover()
  ├─ scrollIntoView (可能再次调整位置)
  ├─ 鼠标移动
  └─ delay(500ms)
  ↓
读取 top2 = 32 (为什么变了？可能是 offsetParent 改变或其他布局问题)
  ↓
diff = -1194 ⚠️ (这个差异不是来自 CSS transform!)
```

### 修复后的第一次迭代

```
页面加载
  ↓
scrollToTop() - 统一初始状态
  ↓
delay(300ms)
  ↓
元素可能在视口外
  ↓
moveMouseTo(10, 10) - 确保鼠标在元素外
  ↓
delay(200ms)
  ↓
读取 rect1 (getBoundingClientRect - 包含 top, left, width, height)
  ↓
调用 hover()
  ├─ scrollIntoView
  ├─ 鼠标移动到元素上
  └─ delay(500ms) - 等待 CSS transition (300ms)
  ↓
读取 rect2
  ↓
比较 rect1.top 和 rect2.top
  ↓
diff ≈ -5px ✅ (对应 translateY(-5px))
```

## 总结

### 第一次迭代 top 值相等的真正原因

1. **测试使用了错误的属性**：`offsetTop` 不受 CSS `transform` 影响
2. **scrollIntoView 的副作用**：可能导致布局变化，但不是 hover 效果
3. **初始状态不一致**：第一次迭代没有统一的起始状态

### 核心问题

**`offsetTop` 永远不应该因为 CSS `transform: translateY(-5px)` 而改变！**

观察到的 1194px 的差异是由于其他原因（可能是 `scrollIntoView` 导致的布局变化，或者测试逻辑问题），而不是 hover 的 CSS transform 效果。

### 正确的修复

使用 `getBoundingClientRect()` 而不是 `offsetTop`，因为：
- `getBoundingClientRect()` 返回元素的**视觉位置**（受 transform 影响）
- `offsetTop` 是**布局位置**（不受 transform 影响）

这样，每次迭代都能一致地观察到 hover 效果带来的约 5px 的位置变化。

