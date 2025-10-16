# Pulsar Agents

## 2. 使用前必读 (Prerequisites)
在执行任何操作前，快速阅读：
1. 根目录 `README-AI.md`

---
## 3. 背景 (Background)
已在代码库中检测到 `pulsar-agentic` 模块（含 `AgenticSession`, `QLAgenticSession`, `AgenticContexts` 等），这些为 AI Agent 提供了会话级 SQL/抓取/上下文能力。同时已有的 `PulsarSession`（基础抓取 & X-SQL 能力）与 WebDriver（浏览器自动化、事件回调、DOM 操作）构成可组合工具集。当前文件目标：
给出一套“从 0 到 可用” 的 AI Agent 设计与实施路线，用于逐步交付：数据采集 Agent、浏览器交互 Agent、组合/编排 Agent。

---
## 4. 目标 (Goals)
中文：
- 构建可扩展 AI Agent 框架：支持任务解析、步骤计划、工具调用、状态与记忆、错误恢复。
- 统一封装 `PulsarSession` 与 WebDriver 的能力为标准化“Tool”接口，便于 LLM 或规则引擎调用。
- 支持多类型 Agent：数据抽取、页面交互、导航探索、策略性采集。
- 提供清晰测试策略与测量指标（成功率 / 重试率 / 平均步骤数）。
- 渐进交付，不破坏现有会话/爬取逻辑。

English:
- Deliver an extensible AI Agent framework (task parsing → planning → tool execution → memory → recovery).
- Expose `PulsarSession` + WebDriver as normalized tool adapters.
- Support multiple agent archetypes: data extractor, browser interaction, exploratory navigator, strategic crawler.
- Provide measurable KPIs (success rate, retries, steps, latency).
- Incremental, backward-compatible delivery.

---
## 5. 非目标 (Non-Goals)
- 不在初期内嵌大型向量数据库（先用内存/轻量 KV）。
- 不直接实现多模型路由（后续扩展）。
- 不引入外部编排框架（如 LangChain）— 先内部最小实现。

---
## 6. 用例 (Primary Use Cases)
1. 结构化数据采集：给定 URL + schema → 自动加载/滚动/提取表格或列表数据。
2. 导航交互：根据自然语言指令（如“登录后搜索‘XX’并抓取前 5 条”）执行点击、输入、等待、抽取。
3. 复杂站点多步流程：分页跟进、条件过滤、提交表单、抓取详情页。
4. 策略采集：结合 X-SQL（站点规则或 URL 模式）与浏览器 DOM 交互混合执行。
5. 失败恢复：元素变更或导航失败时自动重试或重规划。

---
## 7. 关键设计概念 (Key Concepts)
| 概念 | 说明 |
|------|------|
| AgentTask | 用户或上游系统下发的高层目标 + 约束 (目标/输出格式/限制) |
| Plan / Step | 由规划器生成的可执行步骤序列（每步引用 Tool + 参数 + 成功条件） |
| Tool | 封装底层能力的原子操作单元，返回结构化 Observation |
| Observation | 工具执行后的结果/上下文（文本、DOM 片段、表格、错误码） |
| Memory | 短期（对话/上下文窗口）+ 长期（采集痕迹、缓存的解析结果） |
| State | 任务状态机：CREATED → PLANNING → RUNNING → WAITING → COMPLETED/FAILED/CANCELLED |
| Policy | 重试/超时/降级/安全访问策略 |
| Guardrails | 限制危险操作（删除/提交/过度点击）与资源配额 |

---
## 8. 模块分层 (Layered Architecture)
1. Agent Core（pulsar-agentic / 新增子包）
   - Task / Plan / Step 数据模型
   - Planner（LLM / 规则 / 混合）
   - Executor（调度工具执行、记录 Observation）
2. Tools Layer
   - SessionTools（X-SQL 查询、页面抓取、批量 URL 提交）
   - BrowserTools（navigate, click, fill, waitFor, extractDOM, scroll, screenshot）
   - CompoundTools（LoginFlow, Pagination, DetailPageExtractor）
3. Memory & State
   - InMemoryShortTermMemory（限大小滚动窗口）
   - PersistentMemory (后续扩展：基于 H2 / KV)
4. Policies & Guards
   - RetryPolicy（指数退避/最大次数）
   - RateLimiter / Quota（页面/分钟，操作/分钟）
5. Adapters
   - 将现有 `PulsarSession` 方法与 WebDriver 操作映射为 Tool 实例
6. Evaluation
   - MetricsCollector（成功率、平均步骤、重试次数、耗时）

---
## 9. 数据模型草案 (Data Model Draft - Kotlin)
```
// 简化示意（后续放在 pulsar-agentic 新包内）
data class AgentTask(
  val id: String,
  val goal: String,
  val inputs: Map<String, Any?> = emptyMap(),
  val expectedSchema: String? = null,
  val constraints: TaskConstraints = TaskConstraints(),
  val createdAt: Instant = Instant.now()
)

data class TaskConstraints(
  val maxSteps: Int = 40,
  val maxDuration: Duration = Duration.ofMinutes(5),
  val allowNavigation: Boolean = true,
  val allowFormSubmission: Boolean = false,
  val userAgentProfile: String? = null
)

data class Plan(
  val taskId: String,
  val steps: MutableList<Step> = mutableListOf(),
  var status: PlanStatus = PlanStatus.CREATED
)

data class Step(
  val index: Int,
  val toolName: String,
  val arguments: Map<String, Any?>,
  val successCriteria: List<Criterion> = emptyList(),
  var state: StepState = StepState.PENDING,
  var observation: Observation? = null
)

data class Observation(
  val tool: String,
  val success: Boolean,
  val data: Map<String, Any?> = emptyMap(),
  val message: String? = null,
  val raw: String? = null,
  val error: String? = null
)
```

---
## 10. Tool 设计 (Tool Interface Design)
```
interface ToolContext {
  val session: PulsarSession
  val webDriverProvider: () -> WebDriver
  val memory: Memory
  val logger: Logger
  val clock: Clock
}

interface AgentTool {
  val name: String
  val description: String
  val inputSchema: Map<String, Any?>
  val outputSchema: Map<String, Any?>
  suspend fun invoke(args: Map<String, Any?>, ctx: ToolContext): Observation
}
```

### 10.1 SessionTools 示例
- xsqlQuery: 执行 X-SQL，返回结构化表格（行/列）
- loadAndParse: 调用 session 抓取 + 解析 DOM 提要（标题、链接、主列表）
- submitUrls: 批量加入抓取队列（用于递归扩展）

### 10.2 BrowserTools 示例
- navigate(url)
- click(selector|xpath|text)
- fill(selector, text)
- waitFor(selector|state)
- extract(selector, attr|text|html)
- scroll(mode: bottom|incremental|element)
- screenshot(region?)

### 10.3 CompoundTools
- login(username, password, selectors)
- pagination(nextSelector, limit)
- extractList(listSelector, itemSchema)
- detailPageExtractor(linksSelector, fieldsSpec)

---
## 11. 规划器 (Planner)
策略：
1. 规则优先（若任务模板识别出“分页采集”模式 → 使用预设 Plan 原型）
2. 否则：LLM 生成候选步骤（限制工具白名单 + 输出 JSON）
3. 验证器校验：步骤合法、工具存在、参数完整、未超限
4. 必要时裁剪或插入 Guard Step（如“检查是否已登录”）

Plan 校验规则：
- 步骤数 ≤ maxSteps
- 连续导航 < 3（防止导航风暴）
- 提交类操作需要 allowFormSubmission=true
- 不允许未知 toolName

---
## 12. 执行器 (Executor) 工作流
1. 拉取当前 Step
2. 超时包装执行 tool.invoke()
3. 记录 Observation + Metrics
4. 判断 successCriteria（DOM 存在 / 数据行数 / 状态码）
5. 失败 → 重试策略（可局部修改参数，如 wait 延长）
6. 多次失败 → 触发 Replan（局部：重排剩余步骤；或全量：请求 Planner 重新生成）
7. 达成终止条件：
   - 成功：所有必需 Step success 或满足 expectedSchema filled
   - 失败：超过 maxSteps / maxDuration / 连续 fatal error

---
## 13. Memory 策略 (Memory Strategy)
- 短期：最近 N (默认 30) 条 Observation（裁剪保持上下文小于 LLM token 窗口）
- 长期（Phase 2）：持久化关键结构化结果（已解析表格 / 已完成 URL）
- 记忆过滤：只保留：导航成功摘要、抽取结果摘要、错误摘要
- 向 LLM 注入格式：
```
[Recent Observations]
Step 3 click(.next) => success
Step 4 extract(list.items) => 25 items
...
```

---
## 14. 错误与重试策略 (Error & Retry)
| 错误类型 | 识别方式 | 策略 |
|----------|----------|------|
| ElementNotFound | WebDriver 抛出/空集合 | 重试（变体选择器）+ 回退滚动 |
| Timeout | 操作超时 | 延长等待 + 降级抽取模式 |
| NavigationLoop | 相同 URL 重复 n 次 | 中断 + Replan |
| DataSchemaMismatch | 输出列缺失 | 标记需要补抓补抽取步骤 |
| RateLimited | 页眉/响应码关键词 | 等待退避 + 降低频率 |

---
## 15. 安全与资源 (Safety & Resource)
- 限制：每 Task 最大导航次数（默认 20）/ 点击次数（默认 100）/ 提交次数（默认 2）。
- 黑名单：禁止访问 logout / destructive 表单（基于 URL / 选择器关键词）
- 超时：单 Step 默认 30s；工具级可配置。
- 监控：导出指标到现有 metrics（若存在）或日志结构：`agent.taskId step tool success durationMs`。

---
## 16. 性能与并发 (Performance & Concurrency)
- 同一 Agent 单线程串行执行步骤（避免 WebDriver 并发冲突）。
- 多 Agent 并发：通过 session pool / WebDriver pool（已有 WebDriverPoolManager）分配。
- DOM 提要抽取：尽可能局部（querySelectorAll 精确），避免全量 innerHTML。
- 缓存：同 URL 重访短期内使用快照（需策略标签：允许使用缓存）。

---
## 17. 测试策略 (Testing Strategy)
| 级别 | 内容 | 示例 |
|------|------|------|
| 单元 | Planner 规则 / 工具参数校验 | `PlannerRuleTests` |
| 工具 | 每个 Tool 的输入输出 | `NavigateToolTest` |
| 集成 | 典型 Plan 执行（分页抽取） | `PaginationAgentIT` |
| 回归 | 常见站点 DSL 模板 | `ECommerceListExtractIT` |
| 性能 | 平均步骤耗时、资源使用 | Benchmark（后期） |

Mock 策略：
- Session 工具可用 in-memory stub
- WebDriver 工具：提供 Headless + Mock DOM 层（Phase 2）

---
## 18. KPI (Evaluation Metrics)
- 任务成功率（成功终止 / 总数）≥ 85%（初期）
- 平均步骤数：<25（典型列表抽取场景）
- 重试率：<15%
- Replan 触发占比：<10%
- 单任务平均耗时：< 90s（中等复杂度）

---
## 19. 里程碑 (Roadmap)
| 阶段 | 交付 | 说明 |
|------|------|------|
| M1 | 数据模型 + Tool 接口 + 2 个基础 Tool(navigate, extract) | 验证最小执行链 |
| M2 | Planner(规则) + 执行器 + 观测记录 | 支持简单线性任务 |
| M3 | 扩展 BrowserTools(click/fill/wait/scroll) + SessionTools(xsqlQuery/loadAndParse) | 融合抓取与交互 |
| M4 | CompoundTools(pagination, listExtract) + RetryPolicy | 实用化采集 |
| M5 | Memory + Replan + MetricsCollector | 鲁棒与可观测 |
| M6 | 登录/表单工具 + Guardrails | 更复杂场景 |
| M7 | 优化性能 + Headless 多实例压力测试 | 稳定性提升 |
| M8 | LLM Planner(可选) 注入上下文 - YAML/JSON Plan | 智能规划 |

每阶段需：
1. 最小增量 PR
2. 单元+集成测试通过
3. 文档（本文件或 README 补充）
4. 指标评估记录

---
## 20. 与现有结构接入 (Integration Points)
- `AgenticSession`: 作为 session 适配器 (ToolContext.session)
- `PulsarSession`: 保持向后兼容；为工具提供底层抓取 API
- WebDriver Pool: 通过 provider 注入（延迟获取）避免空闲占用
- 配置：新增 `agentic.*` 前缀（如 `agentic.maxSteps`, `agentic.defaultTimeoutMs`）
- 日志：使用统一 logger 名称前缀 `agent.`

---
## 21. 最小示例 (Pseudo Flow Example)
```
Task: 抓取 https://example.com/products 第 3 页内所有商品名与价格
Planner → Plan:
  0 navigate(url="https://example.com/products")
  1 pagination(nextSelector="a.next", limit=3)
  2 extract(listSelector=".product-item", fields=[name:".title", price:".price"]) expectedSchema=product(name,price)
Executor 逐步执行，收集 Observation → 汇总结构化 JSON/CSV 输出。
```

---
## 22. Prompt 模板 (LLM Planner Prompt Template - Phase M8)
```
System: You are a web automation planning assistant. Use only the provided tools.
Tools: {toolCatalogJson}
Constraints: maxSteps={maxSteps}, allowNavigation={allowNavigation}
Goal: {goal}
Recent Observations:
{observations}
Output JSON:
{ "steps": [ {"tool":"navigate","args":{"url":"..."}}, ... ] }
```

验证：解析 JSON → 校验工具名称 → 剪裁超限 → 若合格写入 Plan。

---
## 23. 未来扩展 (Future Enhancements)
- 向量化长记忆（URL 内容摘要 → 相似度检索）
- 并行子任务（多分支 Plan）
- 采集质量评分（字段覆盖率 / 数据完整度）
- 反爬虫策略自适应（动态等待 / 节奏随机化）
- DSL：自然语言 → X-SQL 模板 → 自动参数补全

---
## 24. 验收清单 (Acceptance Checklist)
- [ ] 核心数据结构定义
- [ ] Tool 接口与基础实现
- [ ] 执行器串行模型
- [ ] 重试/超时策略
- [ ] Metrics 输出
- [ ] 文档更新（本文件）
- [ ] 单元 + 集成测试覆盖
- [ ] 无破坏性变更（公共 API 稳定）

---
## 25. 总结 (Summary)
以上方案将现有 `PulsarSession` + WebDriver 能力标准化为可规划、可执行、可观察的 Agent 体系，遵循最小增量路线，确保：低耦合（工具适配层）、高可测试性（Plan/Tool 分层）、可扩展性（CompoundTools、LLM Planner）、可观测性（Metrics + Observation 记录）。按路线图实施可在 M4 达到初步实用，在 M8 达到智能规划水平。

若需偏离规范（如引入外部框架），需在 PR 模板中注明：偏离项 + 原因 + 风险 + 回滚策略。

---
(End of Plan)
