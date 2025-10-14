# 🚦 Inference Developer Guide

## 📋 Prerequisites

Before starting development, ensure you understand:

1. Root `README-AI.md` – Global development guidelines and project structure
2. Project architecture – Multi-module Maven project with Kotlin as primary language

## 🎯 Overview

`InferenceEngine.kt` 实现两个方法：`extract` 与 `observe`。

本文介绍 `extract` 与 `observe` 两个核心方法的执行流程、输入输出契约、日志/度量采集点，以及典型边界情况，并给出伪代码与实现要点。

你需要根据本文介绍来实现这两个方法。

- 代码位置：`pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/InferenceEngine.kt`
- 相关类型：`WebDriver`, `ChatModel`, `DomService`
- 相关提示词构造：`../Prompt.kt` 中的 `build*Prompt` 系列（已提供）
- 参考脚本（行为等价的 TypeScript 版本）：`devdocs/inference.ts`（包含 `appendSummary` 和写文件示例）

---

## 🔑 关键组件与依赖

- DOM 获取
  - 使用 `DomService.getAllTrees(PageTarget(), SnapshotOptions())` 获取可访问树/DOM 片段。
  - 当前 Kotlin `Prompt.kt` 中 `build*UserPrompt` 接口均接收 `domElements: String`。若上层产出 `List<String>`，请先 `joinToString("\n\n")`。
- 提示词构造（均在 `Prompt.kt`）：
  - `buildExtractSystemPrompt(userProvidedInstructions?)`
  - `buildExtractUserPrompt(instruction, domElements)`
  - `buildMetadataSystemPrompt()` / `buildMetadataPrompt(instruction, extractedData, chunksSeen, chunksTotal)`
  - `buildObserveSystemPrompt(userProvidedInstructions?)` / `buildObserveUserMessage(instruction, domElements)`
- LLM 调用
  - 使用 `ChatModel` 的对话式接口；当支持“结构化响应模型”时，请传入 `response_model.schema` 以约束 JSON 输出。
  - 温度策略：`gpt-5*` 使用 1，其余 0.1（稳定优先）。
- 日志与度量（参考 `devdocs/inference.ts`）
  - 可选开关：`logInferenceToFile`。
  - 写入调用/响应快照：`writeTimestampedTxtFile(<prefix>_summary, <call|response>_type, payload)`。
  - 汇总：`appendSummary(prefix, entry)`，聚合 token 与耗时：
    - entry 字段：`<prefix>_inference_type`、`timestamp`、`LLM_input_file`、`LLM_output_file`、`prompt_tokens`、`completion_tokens`、`inference_time_ms`。
  - 前缀：`extract`、`metadata`（落在 `extract_summary`）、`observe` 或 `act`（当从 act 路径触发时）。

---

## 一、`extract` 流程

目的：给定用户指令与 DOM 片段，使用结构化 Schema 解析出目标数据，并使用二次调用生成元数据（过程进度与完成态）。

### 输入/输出契约
- 输入
  - `instruction: String` – 用户意图/目标
  - `domElements: String` – DOM/可访问树片段（如来自 `DomService` 的序列化文本）。若外部为 `List<String>`，需先合并
  - `schema: JSON Schema` – 目标结构的 JSON Schema（用于强约束 LLM 输出）
  - `chunksSeen: Int` / `chunksTotal: Int` – 流式分块抽取时的进度计数
  - `requestId: String` – 透传以便审计
  - `userProvidedInstructions?: String` – 用户附加规范（将拼入 system prompt）
  - `logInferenceToFile?: Boolean` – 是否将消息与响应落盘并汇总
- 输出
  - 展开的结构化数据对象（遵循入参 `schema`）
  - `metadata: { progress: String; completed: Boolean }`
  - `prompt_tokens: Int`，`completion_tokens: Int`（缺失视为 0）
  - `inference_time_ms: Long` – 两次调用耗时之和

### 元数据 Schema
```
Metadata := {
  progress: string  // extracted so far 的简洁进度描述
  completed: boolean // 当已满足 instruction 时为 true（即便仍有剩余 chunk）
}
```

### 执行步骤
1) 第一次调用（抽取）
- 构造消息：
  - `buildExtractSystemPrompt(userProvidedInstructions)`
  - `buildExtractUserPrompt(instruction, domElements)`
- 结构化调用：`response_model = { schema, name: "Extraction" }`
- 记录耗时与 `usage`（若存在）
- 开启落盘时（可选）：
  - `writeTimestampedTxtFile("extract_summary", "extract_call", { requestId, messages })`
  - `writeTimestampedTxtFile("extract_summary", "extract_response", { requestId, rawResponse })`
  - `appendSummary("extract", { ...tokens/time... })`

2) 第二次调用（元数据）
- 构造消息：
  - `buildMetadataSystemPrompt()`
  - `buildMetadataPrompt(instruction, extractedData, chunksSeen, chunksTotal)`
- 结构化调用：`response_model = { schema: Metadata, name: "Metadata" }`
- 记录耗时与 `usage`
- 开启落盘时（可选）：同上，前缀与类型使用 `metadata_call` / `metadata_response`

3) 归并与返回
- tokens：两次调用 `prompt_tokens` 与 `completion_tokens` 分别累加
- 耗时：两段耗时相加
- 返回：展开 `extractedData` + `metadata` + 累计 tokens/耗时

### 伪代码（框架不定、语义稳定）
```
function extract(args) {
  // 1) 抽取
  const extractMessages = [
    buildExtractSystemPrompt(userProvidedInstructions),
    buildExtractUserPrompt(instruction, domElements),
  ];
  maybeLogCall('extract', extractMessages);
  const t0 = now();
  const extractResp = chatModel.chat({
    messages: extractMessages,
    response_model: { schema, name: 'Extraction' },
    requestId,
    temperature: isGPT5 ? 1 : 0.1,
  });
  const t1 = now();
  const { data: extractedData, usage: u1 } = extractResp;
  maybeLogResp('extract', extractedData, t1 - t0, u1);

  // 2) 元数据
  const metadataMessages = [
    buildMetadataSystemPrompt(),
    buildMetadataPrompt(instruction, extractedData, chunksSeen, chunksTotal),
  ];
  maybeLogCall('metadata', metadataMessages);
  const t2 = now();
  const metaResp = chatModel.chat({
    messages: metadataMessages,
    response_model: { schema: Metadata, name: 'Metadata' },
    requestId,
    temperature: isGPT5 ? 1 : 0.1,
  });
  const t3 = now();
  const { data: { completed, progress }, usage: u2 } = metaResp;
  maybeLogResp('metadata', { completed, progress }, t3 - t2, u2);

  return {
    ...extractedData,
    metadata: { completed, progress },
    prompt_tokens: (u1?.prompt_tokens ?? 0) + (u2?.prompt_tokens ?? 0),
    completion_tokens: (u1?.completion_tokens ?? 0) + (u2?.completion_tokens ?? 0),
    inference_time_ms: (t1 - t0) + (t3 - t2),
  };
}
```

### 边界与注意事项
- Schema 驱动：务必使用响应模型 Schema 强约束输出并做解包校验。
- 温度策略：`gpt-5*` → 1；其他 → 0.1。
- 缺失用量：`usage` 可能为空，按 0 计入统计。
- 日志与追踪：`logInferenceToFile` 打开时写入 `<prefix>_summary` 目录，并追加 `<prefix>_summary.json`。
- DOM 规整：若传入 `List<String>`，请合并为单一 `String`（建议使用空行分隔）。

---

## 二、`observe` 流程

目的：给定用户指令与 DOM 片段，定位符合目标的可访问元素列表；可选地生成候选交互方法与参数（用于 `act` 流程的前置）。

### 输入/输出契约
- 输入
  - `instruction: String`
  - `domElements: String`（或上层先合并）
  - `returnAction?: Boolean` – 是否要求候选交互
  - `fromAct?: Boolean` – 从 `act` 链路触发时前缀使用 `act`（便于排查）
  - `requestId: String`，`userProvidedInstructions?: String`，`logInferenceToFile?: Boolean`
- 输出
  - `elements: Array<...>`（见下方 Schema）
  - `prompt_tokens: Int`，`completion_tokens: Int`（缺失视为 0）
  - `inference_time_ms: Long`

### 动态输出 Schema
```
Observation := {
  elements: Array<{
    elementId: string   // 必须为 'number-number' 格式，且禁止方括号
    description: string // 可访问元素与用途描述
    // 当 returnAction === true 时，还需：
    // method: string     // 候选 Playwright/驱动交互方法
    // arguments: string[]
  }>
}
```

### 执行步骤
1) 构造消息：
   - `buildObserveSystemPrompt(userProvidedInstructions)`
   - `buildObserveUserMessage(instruction, domElements)`
2) 一次结构化调用：`response_model = { schema: Observation, name: "Observation" }`
3) 日志（可选）：
   - 前缀：`fromAct ? 'act' : 'observe'`
   - 写入 call/response 文件并 `appendSummary(prefix, entry)`
4) 结果规范化：
   - 将 `description` 强制转换为字符串
   - 当 `returnAction` 为真时，补齐 `method` 与 `arguments`

### 伪代码
```
function observe(args) {
  const observeSchema = { /* 如上 */ };
  const messages = [
    buildObserveSystemPrompt(userProvidedInstructions),
    buildObserveUserMessage(instruction, domElements),
  ];
  const filePrefix = fromAct ? 'act' : 'observe';
  maybeLogCall(filePrefix, messages);

  const t0 = now();
  const resp = llm.chat({
    messages,
    response_model: { schema: observeSchema, name: 'Observation' },
    temperature: isGPT5 ? 1 : 0.1,
    requestId,
  });
  const t1 = now();
  const { data, usage } = resp;
  maybeLogResp(filePrefix, data, t1 - t0, usage);

  const elements = (data.elements ?? []).map(el => {
    const base = { elementId: el.elementId, description: String(el.description) };
    return returnAction ? { ...base, method: String(el.method), arguments: el.arguments } : base;
  });

  return {
    elements,
    prompt_tokens: usage?.prompt_tokens ?? 0,
    completion_tokens: usage?.completion_tokens ?? 0,
    inference_time_ms: t1 - t0,
  };
}
```

### 关键点与边界情况
- 动态 Schema：`returnAction` 决定是否强制输出 `method/arguments` 字段，确保与下游执行链路对齐。
- 元素 ID 约束：明确要求 `'number-number'` 格式且不包含方括号，模型对齐由描述与 Schema 双重提示完成。
- 安全规范化：将 `description` 转为字符串，避免模型偶发返回的非字符串类型。
- 日志与命名：当从 `act` 路径调用时，文件前缀切换为 `act`，便于离线排查。

---

## 🧪 最小实现指引（Kotlin）

- 输入整形
  - 若 `extract/observe` 收到 `List<String> domElements`，先 `domElements.joinToString("\n\n")` 再传入 `Prompt.kt` 构造的用户消息。
- ChatModel 调用
  - 统一封装一个 `callStructured(model, messages, schema, name, requestId, temperature)` 辅助，返回 `{ data, usage }`。
  - 暂无 Schema 库时，可先返回字符串 JSON，再用 Jackson 解析为目标类型，保持接口不变，后续替换为原生 Schema 支持。
- 日志/度量
  - 增加布尔开关 `logInferenceToFile`；若为真，调用 `writeTimestampedTxtFile` 写入 `*_call` 与 `*_response`。
  - `appendSummary(prefix, entry)` 追踪 tokens/耗时；summary 文件建议命名为 `extract_summary.json` / `observe_summary.json` / `act_summary.json`。
- 温度
  - 通过模型名判断 `gpt-5*`，其余使用低温。

---

## 📎 参考与文件约定回顾
- 调用/响应文件：`logs/<prefix>_summary/<kind>_<timestamp>.txt`
- 汇总文件：`logs/<prefix>_summary.json`
- `prefix`：`extract|metadata`（均归档到 `extract_summary` 目录）、`observe` 或 `act`
- 汇总条目字段：`<prefix>_inference_type`、`timestamp`、`LLM_input_file`、`LLM_output_file`、`prompt_tokens`、`completion_tokens`、`inference_time_ms`

---

## 🔬 质量门禁与测试建议

- 构建与快速测试（Windows CMD）：
  - `mvnw.cmd -v` 确认 Maven Wrapper 可用
  - `mvnw.cmd -q -DskipTests package` 快速构建
  - `mvnw.cmd -q test -pl pulsar-core/pulsar-skeleton -am` 仅构建/测试当前模块
- 单元测试建议：
  - Happy path：`extract` 在简单 Schema 下返回期望字段；`observe` 返回至少一个元素。
  - 边界：空 DOM、空 usage、`returnAction = true` 时 method/arguments 必填约束；元素 ID 格式校验。
  - 记录并断言 tokens/耗时按“缺失→0”逻辑归并。

---

## 比较与协作关系
- 调用次数：`extract` 采用“两段式”，先抽取再生成元数据；`observe` 为“一段式”。
- 结果角色：`observe` 强调元素发现与候选交互；`extract` 强调结构化业务数据与过程状态。
- Prompt 构造：两者均依赖 `build*Prompt` 系列。
- 统一度量：均统一记录 prompt/completion tokens 与耗时，利于成本与性能监控。

---

## 实践建议
- 优先定义稳定的 Schema，并通过 `describe` 提供明确约束与示例，有助于减少模型偏差。
- 针对 `gpt-5` 系列可适度提升温度获取更优召回；其他模型优先稳定性（低温度）。
- 在排障与评估阶段开启 `logInferenceToFile`，结合 `appendSummary` 进行离线分析；上线后可按需关闭以节约 IO。

---

## 🟢 实施进度（2025-10-14）

本次已按本文规范完成 `InferenceEngine.kt` 的落地实现，关键点如下：

- 提示词与消息
    - 使用 `Prompt.kt` 的 `buildExtractSystemPrompt`、`buildExtractUserPrompt`、`buildMetadataSystemPrompt`、`buildMetadataPrompt`、`buildObserveSystemPrompt`、`buildObserveUserMessage` 构造消息。
    - DOM 片段统一按 `List<String>` → `joinToString("\n\n")` 合并后传入 user 消消息。
- extract（双段式）
    - 新增 `ExtractParams`/`extract(params)`：执行两次模型调用（抽取 + 元数据），累加 tokens 与耗时，返回：
        - 展开的结构化数据（按 Schema）
        - `metadata: { progress, completed }`
        - `prompt_tokens`、`completion_tokens`、`inference_time_ms`
    - 由于当前 ChatModel 接口暂无“原生 schema 约束”参数，采用“在 user 消息中追加 JSON Schema 的强约束说明”的方式收敛输出；返回 JSON 用 Jackson 解析为 `ObjectNode`。
    - 可选落盘：在 `logs/` 下写入 `extract_summary/*` 与 `extract_summary.json`，条目含 tokens/耗时/文件路径。
- observe（单段式）
    - 新增 `ObserveParams`/`observe(params)`：构造动态 Schema 约束（是否输出 `method/arguments` 由 `returnAction` 决定），解析为元素列表：
        - `elements: [{ elementId, description, (method?), (arguments?) }]`
        - 规范化：`elementId` 去除包裹的方括号；`description` 强制转字符串；`method/arguments` 在 `returnAction=true` 时补齐。
        - 汇总 `prompt_tokens`、`completion_tokens`、`inference_time_ms`。
    - 可选落盘：前缀为 `observe`，若 `fromAct=true` 则前缀为 `act`，方便排查。
- 温度策略
    - `gpt-5*` 设定 1，其他 0.1；当前 ChatModel 未显式接入温度参数，保留检测逻辑，后续可在支持时透传。
- 兼容保留
    - 旧版 `extract(instruction)` / `extract(instruction, List<String>)` / `observe(instruction, List<String>)` 作为轻薄包装/占位，推荐使用新的参数化 API。

### 需求映射与覆盖度
- 输入合并与 Prompt 使用：已完成
- extract 二段式：已完成（抽取+元数据，合并 tokens/耗时）
- observe 一段式：已完成（动态 Schema，前缀 act/observe）
- 日志/度量：已完成（`writeTimestampedTxtFile` + `appendSummary`）
- Schema 约束：通过 prompt 强约束，后续可替换为原生 schema 响应模型（待接入）
- 边界处理：
    - usage 缺失按 0 计算（已完成）
    - JSON 解析失败回退为空对象（已完成）
    - 元素 ID 去方括号与字符串化描述（已完成）

---

## 🧪 构建与快速验证（Windows CMD）

受多模块依赖影响，需先本地安装 `pulsar-core` 相关子模块，再构建 `pulsar-skeleton`：

```bat
REM 1) 构建并安装 pulsar-core 聚合模块（跳过测试以提速）
"%CD%\mvnw.cmd" -T 1C -DskipTests -f pulsar-core\pom.xml clean install

REM 2) 仅打包 pulsar-skeleton 模块（可选）
"%CD%\mvnw.cmd" -DskipTests -f pulsar-core\pulsar-skeleton\pom.xml package
```

如需最小烟测，可在上层集成处以新的 `ExtractParams` / `ObserveParams` 调用：
- `ExtractParams(instruction, domElements, schema, chunksSeen, chunksTotal, requestId, userProvidedInstructions, logInferenceToFile)`
- `ObserveParams(instruction, domElements, requestId, userProvidedInstructions, returnAction, logInferenceToFile, fromAct)`

---

## 🔜 下一步计划

1) 接入/透传结构化响应模型与推理参数
    - 当 ChatModel 暴露 `response_model.schema`、`temperature`、`requestId` 等选项后，替换当前基于 prompt 的 Schema 收敛策略，显式传参以增强稳健性。
2) 单元测试与契约校验
    - 为 `extract`/`observe` 增加最小单测（happy path + 边界）：
        - 非法/空 JSON 回退
        - `returnAction = true` 时 method/arguments 断言
        - `elementId` 去方括号与 `number-number` 格式
        - tokens/耗时聚合与缺省为 0
3) 错误回退与日志增强
    - 对异常情况在结果中附加 `error` 字段（保持向后兼容），并在 `*_summary.json` 中追加 `error` 与 `stack`（仅在 `logInferenceToFile=true` 时）。
4) 参数表面化与上层对接
    - 在更高层的调用点暴露 `logInferenceToFile`、`userProvidedInstructions` 与 `fromAct` 等参数，完善外部 API。
5) 小改进
    - 在 `extract` 的 metadata 解析中，加入字段健壮性校验（缺失字段填默认值），并允许自定义进度描述阈值。
