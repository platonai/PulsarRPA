# Command API - AI 友好参考指南

## 概述

Command API 提供了 AI 系统通过 Browser4 执行网页自动化任务的端点。它支持两种输入模式：

1. **结构化 JSON 命令** - 强类型请求，字段明确
2. **纯文本命令** - 自然语言指令转换为结构化命令

本指南帮助 AI 代理理解如何生成高质量、强约束的命令，确保 Browser4 能够准确理解并执行。

**主要入口**: `ai.platon.pulsar.rest.api.controller.CommandController`

---

## API 端点

### 基础 URL
```
http://localhost:8182/api/commands
```

### 可用端点

| 方法 | 端点 | 描述 |
|--------|----------|-------------|
| POST | `/api/commands` | 执行结构化 JSON 命令 |
| POST | `/api/commands/plain` | 执行纯文本命令 |
| GET | `/api/commands/{id}/status` | 获取命令执行状态 |
| GET | `/api/commands/{id}/result` | 获取命令执行结果 |
| GET | `/api/commands/{id}/stream` | 通过 SSE 流式获取状态 |

---

## 执行模式

### 同步执行（默认）
```bash
# JSON 命令
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com"}'

# 纯文本命令
curl -X POST "http://localhost:8182/api/commands/plain" \
  -H "Content-Type: text/plain" \
  -d '访问 https://example.com 并总结页面内容。'
```

### 异步执行
```bash
# 带 async 标志的 JSON 命令
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "async": true}'

# 带 async 参数的纯文本命令
curl -X POST "http://localhost:8182/api/commands/plain?async=true" \
  -H "Content-Type: text/plain" \
  -d '访问 https://example.com 并总结页面内容。'
```

---

## 数据结构

### CommandRequest

JSON 命令的主要输入结构。

**位置**: `ai.platon.pulsar.rest.api.entities.CommandRequest`

```kotlin
data class CommandRequest(
    var url: String,                           // 必需：目标页面 URL
    var args: String? = null,                  // 可选：加载选项（CLI 风格）
    var onBrowserLaunchedActions: List<String>? = null,  // 浏览器启动时的操作
    var onPageReadyActions: List<String>? = null,        // 页面就绪时的操作
    var pageSummaryPrompt: String? = null,     // 页面总结提示
    var dataExtractionRules: String? = null,   // 数据提取规则
    var uriExtractionRules: String? = null,    // URI 提取规则
    var xsql: String? = null,                  // X-SQL 查询
    var richText: Boolean? = null,             // 是否保留富文本格式
    var async: Boolean? = null,                // 异步执行标志
    var id: String? = null                     // 可选的命令 ID
)
```

#### 字段详情

| 字段 | 类型 | 必需 | 描述 |
|------|------|------|------|
| `url` | String | **是** | 目标页面 URL。必须是有效的 HTTP/HTTPS URL。 |
| `args` | String | 否 | CLI 风格的加载选项（如 `-expires 1d -refresh`）。参见 [LoadOptions 指南](./load-options-guide-zh.md)。 |
| `onBrowserLaunchedActions` | List<String> | 否 | 浏览器启动时、导航到 URL 之前执行的操作。 |
| `onPageReadyActions` | List<String> | 否 | 页面内容完全加载后执行的操作。 |
| `pageSummaryPrompt` | String | 否 | LLM 总结页面内容的自然语言提示。 |
| `dataExtractionRules` | String | 否 | 从页面提取结构化字段的规则。 |
| `uriExtractionRules` | String | 否 | 从页面提取 URI 的模式或描述。 |
| `xsql` | String | 否 | 高级 DOM 数据提取的 X-SQL 查询。 |
| `richText` | Boolean | 否 | 如果为 true，在提取的文本内容中保留格式。 |
| `async` | Boolean | 否 | 如果为 true，异步执行命令并返回 ID。 |
| `id` | String | 否 | 命令的自定义标识符（如果未提供则自动生成）。 |

---

### CommandStatus

跟踪命令的执行状态。

**位置**: `ai.platon.pulsar.rest.api.entities.CommandStatus`

```kotlin
data class CommandStatus(
    val id: String,                            // 唯一命令标识符
    var statusCode: Int,                       // HTTP 风格状态码
    var event: String,                         // 最后事件描述
    var isDone: Boolean,                       // 执行是否完成
    var pageStatusCode: Int,                   // 页面获取状态码
    var pageContentBytes: Int,                 // 页面内容大小（字节）
    var message: String?,                      // 附加信息
    var request: CommandRequest?,              // 原始请求
    var commandResult: CommandResult?,         // 执行结果
    var instructResults: MutableList<InstructResult>  // 各指令结果
)
```

#### 状态码

| 代码 | 名称 | 描述 |
|------|------|------|
| 200 | OK | 命令成功完成 |
| 201 | CREATED | 命令已创建，尚未执行 |
| 202 | PROCESSING | 命令正在执行 |
| 400 | BAD_REQUEST | 输入无效（如空命令） |
| 404 | NOT_FOUND | 命令 ID 未找到 |
| 417 | EXPECTATION_FAILED | 命令执行失败 |

---

### CommandResult

包含命令执行的输出数据。

**位置**: `ai.platon.pulsar.rest.api.entities.CommandResult`

```kotlin
data class CommandResult(
    var summary: String?,                      // 总体摘要（用于 Agent 命令）
    var pageSummary: String?,                  // LLM 生成的页面摘要
    var fields: Map<String, String>?,          // 提取的数据字段
    var links: List<String>?,                  // 提取的 URI/链接
    var xsqlResultSet: List<Map<String, Any?>>?  // X-SQL 查询结果
)
```

---

## 命令类型

### 1. 基于 URL 的命令（结构化）

包含有效 URL 的命令通过标准命令执行流程处理：

```
CommandService.executeCommand(request, status, eventHandlers)
```

#### 流程：
1. 解析和验证 CommandRequest
2. 使用指定选项加载页面
3. 执行页面操作（如果指定）
4. 执行 LLM 提示进行总结/提取
5. 使用指定规则提取 URI
6. 执行 X-SQL 查询（如果指定）
7. 返回带结果的 CommandStatus

### 2. Agent 命令（开放式）

当纯文本命令无法转换为结构化 CommandRequest（没有 URL 或指令复杂）时，由伴侣 Agent 执行：

```
CommandService.executeAgentCommand(plainCommand)
```

#### 流程：
1. 接收纯文本指令
2. 传递给 `session.companionAgent.run(plainCommand)`
3. Agent 自主决定操作（导航、交互、提取）
4. 返回最终 Agent 状态作为 CommandStatus

---

## 纯文本命令转换

纯文本命令通过 LLM 规范化为 JSON：

```
ConversationService.normalizePlainCommand(plainCommand) -> CommandRequest?
```

### 转换过程：
1. 从纯文本中提取 URL
2. 如果未找到 URL，路由到 Agent 命令
3. 使用 LLM 将纯文本转换为 JSON 结构
4. 将 JSON 解析为 CommandRequest
5. 作为结构化命令执行

### 支持的纯文本模式：

```text
访问 https://www.amazon.com/dp/B08PP5MSVB
总结这个产品。
提取：产品名称、价格、评分。
找到所有包含 /dp/ 的链接。
页面加载后：点击 #title，然后滚动到中间。
```

转换为：

```json
{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "pageSummaryPrompt": "总结这个产品。",
  "dataExtractionRules": "产品名称、价格、评分",
  "uriExtractionRules": "所有包含 /dp/ 的链接",
  "onPageReadyActions": ["点击 #title", "滚动到中间"]
}
```

---

## 操作规范

### onBrowserLaunchedActions

导航到目标 URL 之前执行的操作。用于浏览器设置任务。

**常用操作：**
- `"清除浏览器 cookies"` / `"clear browser cookies"` - 清除所有 cookies
- `"导航到 [url]"` / `"navigate to [url]"` - 先导航到其他 URL
- `"点击 [选择器]"` / `"click [selector]"` - 点击元素
- `"等待 [时长]"` / `"wait [duration]"` - 等待指定时间

**示例：**
```json
{
  "onBrowserLaunchedActions": [
    "clear browser cookies",
    "navigate to the home page",
    "click a random link"
  ]
}
```

### onPageReadyActions

页面完全加载后执行的操作。用于页面交互。

**常用操作：**
- `"向下滚动"` / `"scroll down"` - 向下滚动页面
- `"滚动到 [元素]"` / `"scroll to [element]"` - 滚动到特定元素
- `"点击 [选择器]"` / `"click [selector]"` - 点击元素
- `"在 [选择器] 中填入 [值]"` / `"fill [selector] with [value]"` - 填写表单字段
- `"等待 [选择器]"` / `"wait for [selector]"` - 等待元素出现
- `"悬停在 [选择器] 上"` / `"hover over [selector]"` - 悬停在元素上

**示例：**
```json
{
  "onPageReadyActions": [
    "scroll down",
    "click #load-more",
    "wait for .results"
  ]
}
```

---

## 数据提取规范

### pageSummaryPrompt

页面总结的自然语言指令。

**指南：**
- 明确关注领域
- 如需要，指定输出格式
- 保持提示简洁但清晰

**示例：**
```json
{
  "pageSummaryPrompt": "提供产品的简要概述，包括主要功能、目标用户和竞争优势。"
}
```

### dataExtractionRules

提取结构化字段的规范。

**指南：**
- 列出要提取的确切字段名称
- 指定每个字段的预期格式
- 可以包含提取提示

**示例：**
```json
{
  "dataExtractionRules": "产品名称、价格（含货币符号）、评分（满分5分）、评论数量"
}
```

### uriExtractionRules

从页面提取 URI 的规则。

**格式：**
1. **自然语言**: `"所有包含 /dp/ 的链接"` - 转换为正则表达式
2. **正则模式**: `"Regex: .*/dp/[A-Z0-9]+.*"` - 直接使用

**示例：**
```json
{
  "uriExtractionRules": "包含 /dp/ 或 /product/ 的链接"
}
```

### xsql

基于 DOM 提取的 X-SQL 查询。

**示例：**
```json
{
  "xsql": "select dom_first_text(dom, '#productTitle') as title, dom_first_text(dom, '#priceblock_ourprice') as price from load_and_select(@url, 'body')"
}
```

---

## AI 生成命令的约束

当为 Browser4 生成命令时，AI 代理应遵循以下约束：

### URL 约束
1. **有效格式**: URL 必须是标准的 HTTP/HTTPS URL
2. **可访问**: URL 应该是公开可访问的（除非提供了认证）
3. **具体**: 优先使用直接页面 URL 而不是导航序列

### 操作约束
1. **明确性**: 使用 CSS 选择器或清晰的元素描述
2. **顺序**: 逻辑顺序排列操作
3. **时机**: 如果预期有动态内容，包含等待
4. **幂等性**: 操作应该可以重复执行而没有副作用

### 提取约束
1. **字段名称**: 使用清晰、描述性的字段名称
2. **格式提示**: 指定预期的数据格式
3. **备选方案**: 考虑数据缺失的情况
4. **范围**: 限制提取到可见/可访问的内容

### 性能约束
1. **最小操作**: 只包含必要的操作
2. **高效选择器**: 使用具体的 CSS 选择器
3. **合理超时**: 不超过页面加载限制
4. **资源意识**: 考虑页面复杂度

---

## 完整示例

### 示例 1：产品数据提取

**纯文本：**
```text
访问 https://www.amazon.com/dp/B08PP5MSVB
总结这个产品。
提取：产品名称、价格、评分、评论数。
找到所有相似产品链接。
```

**JSON：**
```json
{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "pageSummaryPrompt": "总结这个产品，包括主要功能和规格。",
  "dataExtractionRules": "产品名称、价格（含货币符号）、星级评分（满分5分）、总评论数",
  "uriExtractionRules": "包含 /dp/ 的相似或相关产品链接"
}
```

**cURL：**
```bash
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B08PP5MSVB",
    "pageSummaryPrompt": "总结这个产品，包括主要功能和规格。",
    "dataExtractionRules": "产品名称、价格（含货币符号）、星级评分（满分5分）、总评论数",
    "uriExtractionRules": "包含 /dp/ 的链接"
  }'
```

### 示例 2：带操作的交互式页面

**JSON：**
```json
{
  "url": "https://www.example.com/search",
  "args": "-refresh -requireSize 50000",
  "onBrowserLaunchedActions": [
    "clear browser cookies"
  ],
  "onPageReadyActions": [
    "fill #search-input with '浏览器自动化'",
    "click #search-button",
    "wait for .search-results",
    "scroll down",
    "scroll down"
  ],
  "dataExtractionRules": "结果标题、结果描述、结果 URL",
  "async": true
}
```

### 示例 3：Agent 命令（开放式）

**纯文本（无 URL）：**
```text
搜索关于 AI 浏览器自动化的最新新闻。
访问前 3 个结果。
总结每篇文章的要点。
创建趋势的综合摘要。
```

由于没有提供具体的 URL，这将作为 Agent 命令执行。

---

## 响应格式

### 同步响应

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "statusCode": 200,
  "status": "OK",
  "event": "pageSummary,fields,links",
  "isDone": true,
  "pageStatusCode": 200,
  "pageStatus": "OK",
  "pageContentBytes": 125000,
  "message": "created,textContent,pageSummary,fields,links",
  "commandResult": {
    "pageSummary": "这是一个亚马逊产品页面...",
    "fields": {
      "产品名称": "示例产品",
      "价格": "$29.99",
      "评分": "4.5"
    },
    "links": [
      "https://www.amazon.com/dp/B08ABC123",
      "https://www.amazon.com/dp/B08DEF456"
    ]
  },
  "createTime": "2024-01-05T10:30:00Z",
  "lastModifiedTime": "2024-01-05T10:30:15Z",
  "finishTime": "2024-01-05T10:30:15Z"
}
```

### 异步响应

初始响应（状态 ID）：
```
"550e8400-e29b-41d4-a716-446655440000"
```

轮询状态：
```bash
curl "http://localhost:8182/api/commands/550e8400-e29b-41d4-a716-446655440000/status"
```

---

## 错误处理

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 400 BAD_REQUEST | 空白或无效命令 | 提供有效的 URL 或命令文本 |
| 404 NOT_FOUND | 命令 ID 未找到 | 验证命令 ID 是否正确 |
| 417 EXPECTATION_FAILED | 页面加载或提取失败 | 检查 URL 可访问性，调整选项 |

### 验证

系统验证：
1. URL 格式（必须是标准 HTTP/HTTPS）
2. 命令非空
3. 提取前页面成功加载

---

## 相关文档

- [LoadOptions 指南](./load-options-guide-zh.md) - URL 级别配置选项
- [LoadOptions 快速参考](./load-options-quick-ref.md) - 快速选项参考
- [PageEventHandlers](./page-event-handlers.md) - 页面生命周期事件处理
- [PulsarSettings 指南](./pulsar-settings-guide.md) - 全局浏览器配置
- [REST API 示例](../../docs/rest-api-examples.md) - 更多 API 示例
- [X-SQL 文档](../../docs/x-sql.md) - X-SQL 查询参考

---

## AI 代理最佳实践

### 生成结构化命令

1. **优先使用 JSON 而非纯文本** 以获得确定性行为
2. **包含 args** 进行页面加载优化（如 `-refresh`, `-requireSize`）
3. **明确指定操作** 使用 CSS 选择器
4. **使用结构化提取规则** 带清晰的字段名称
5. **设置 async=true** 用于长时间运行的命令

### 生成纯文本命令

1. **首先包含 URL** 以启用结构化转换
2. **使用清晰的动作动词**：访问、点击、滚动、提取、总结
3. **分离关注点**：导航 → 操作 → 提取
4. **明确** 预期的数据字段
5. **使用"页面加载后："**前缀表示 onPageReadyActions

### 处理结果

1. **检查 isDone** 后再处理结果
2. **验证 statusCode** 为 200 表示成功
3. **处理部分结果**（某些字段可能为 null）
4. **调整后重试** 如果提取失败

---

**版本**: 2026-01-11  
**维护者**: Browser4 团队  
**状态**: 持续更新文档 - 随 Command API 演进更新
