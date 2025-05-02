太好了，下面是几个中英文混合的 **输入示例** 和其对应的 **标准化 JSON 输出**，帮助你更好地理解和测试这个提示。

---

### ✅ 示例 1：中文口语请求

**Input:**

```
打开淘宝首页，加载完成后往下滚动，然后点击登录按钮，帮我看看这个页面都讲了什么。
```

**Output:**

```json
{
  "url": "https://www.taobao.com",
  "talkAboutPage": "Summarize what this page is about.",
  "actionsOnDocumentReady": [
    "scroll down",
    "click 'Login' button"
  ]
}
```

---

### ✅ 示例 2：中英混合 + 模糊表达

**Input:**

```
Go to https://news.ycombinator.com，然后找一下有哪些标题是跟 AI 有关的。
```

**Output:**

```json
{
  "url": "https://news.ycombinator.com",
  "fieldDescriptions": "Extract all news titles related to AI."
}
```

---

### ✅ 示例 3：英语请求 + 参数

**Input:**

```
Load https://example.com?mode=dark&lang=en and tell me what the page is about.
```

**Output:**

```json
{
  "url": "https://example.com",
  "args": "mode=dark&lang=en",
  "talkAboutPage": "Tell me what the page is about."
}
```

---

### ✅ 示例 4：不完整请求

**Input:**

```
Scroll down after loading https://example.com/products
```

**Output:**

```json
{
  "url": "https://example.com/products",
  "actionsOnDocumentReady": [
    "scroll down"
  ]
}
```

---

你是否还需要我提供一个自动转换这类请求的 JavaScript 或 Python 脚本示例？