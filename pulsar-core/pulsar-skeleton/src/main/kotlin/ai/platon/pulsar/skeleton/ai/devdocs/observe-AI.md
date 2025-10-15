# PerceptiveAgent.extract extract / observe 源码逻辑梳理与伪代码

## 📋 Prerequisites

Before starting development, ensure you understand:

1. Root `README-AI.md` – Global development guidelines and project structure
2. Project architecture – Multi-module Maven project with Kotlin as primary language

## 🎯 Overview

[PerceptiveAgent.kt](../PerceptiveAgent.kt)`InferenceEngine.kt` 实现三个方法：`act`, `extract` 与 `observe`。

本文介绍 `extract` 与 `observe` 两个核心方法的执行流程、输入输出契约、日志/度量采集点，以及典型边界情况，并给出伪代码与实现要点。

你需要根据本文介绍来实现这两个方法。

- 代码位置：`pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/PerceptiveAgent.kt`
- 相关类型：`InferenceEngine`

难点

设计一个描述数据格式 schema 的机制，用来描述extract的提取结果，当前 schema 简单使用了一个 Map<String, String>，你需要设计一个更完善的机制。

—

## extract 方法

作用：按指令从页面结构中抽取结构化数据，支持自定义 schema；无参调用时采用默认抽取。

输入：
- 可选 `string | ExtractOptions`；
- 字符串会被转换为 `{ instruction, schema: defaultExtractSchema }`；
- 若传入的 `ExtractOptions` 未显式提供 `schema`，会自动填充默认 schema。

伪代码：

```
function extract(instructionOrOptions?: string | ExtractOptions): ExtractResult {
  if instructionOrOptions is undefined:
    result = inference.extract()
    addToHistory('extract', instructionOrOptions, result)
    return result

  // 归一化 options（字符串 -> 默认 schema；缺省 schema -> 填充默认）
  options = normalizeExtractOptions(instructionOrOptions, defaultExtractSchema)

  requestId = randomId()

  log('extract', { instruction: options.instruction, requestId })

  try:
    result = inference.extract({
      instruction: options.instruction,
      schema: options.schema,
      requestId,
      domSettleTimeoutMs: options.domSettleTimeoutMs,
      useTextExtract: options.useTextExtract,
      selector: options.selector,
      iframes: options.iframes,
    })
  catch (e):
    logError('extract', e, requestId)
    throw e

  addToHistory('extract', instructionOrOptions, result)
  return result
}
```

—

## observe 方法

作用：根据自然语言指令理解页面元素与可执行动作，返回一组可操作/可定位的观察结果，可选直接给出“下一步动作”。

输入：
- 可选 `string | ObserveOptions`；字符串会被转换为 `{ instruction }`；默认 `returnAction = true`。


伪代码：

```
function observe(instructionOrOptions?: string | ObserveOptions): ObserveResult[] {
  options = normalizeObserveOptions(instructionOrOptions)  // string -> { instruction }, default returnAction=true

  requestId = randomId()

  log('observe', {
    instruction: options.instruction,
    requestId,
  })

  try:
    result = inference.observe({
      instruction: options.instruction,
      requestId,
      domSettleTimeoutMs: options.domSettleTimeoutMs,
      returnAction: options.returnAction ?? true,
      drawOverlay: options.drawOverlay,
      iframes: options.iframes,
    })
  catch (e):
    logError('observe', e, { requestId, instruction: options.instruction })
    throw e

  addToHistory('observe', instructionOrOptions, result)
  return result
}
```
