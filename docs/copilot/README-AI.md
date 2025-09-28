# AI Copilot 使用与编写指南 (v2025-09)

> 目标：帮助 AI / 开发者在本仓库中高效、稳定、可控地完成各类任务（写代码、审查、文档、脚本、测试等），并保证产出质量与一致性。

## 1. 总览 (Overview)
- 仓库类型：多模块 Maven（`./mvnw` 统一构建）
- 主语言：Kotlin（兼容部分 Java 代码；公共 API 建议优先 Kotlin 实现）
- 运行环境：依据 `pom.xml` 判断 Java 版本（AI 生成代码前应扫描根 `pom.xml`）
- 主要领域：Browser Agent and Browser Automation
- AI 交互原则：最小必要修改、保留风格、显式说明、自动验证
- 优先级顺序：稳定性 > 可维护性 > 性能 > 功能扩展 > 美观

## 2. 使用前必读 (Prerequisites)
在执行任何操作前，快速阅读：
1. 根目录 `README-AI.md`（与本文件保持一致；若冲突，以根文件为准）
2. 根 `pom.xml`（父级依赖管理 & 模块划分 & 版本约束）
3. `docs/` 下相关专题：
   - `concepts.md`：核心概念
   - `config.md`：配置策略
   - `log-format.md`：日志结构

## 3. 适用任务类型 (Job Types)
- 代码：新增功能 / 修复缺陷 / 重构 / 性能调优 / 安全加固
- 审查：风格 / 逻辑风险 / 并发 / NPE / 复杂度 / 冗余 / 回归风险
- 文档：README、指南、API、运行说明、架构草图（文本）
- 注释：KDoc / 复杂算法说明 / 配置注释 / 约束声明
- 构建 / 运行：命令、脚本优化、构建失败诊断
- 测试：用例补充、边界设计、可靠性、性能回归

## 4. 交付标准 (Definition of Done)
一项任务完成需满足：
1. 变更最小且自洽，不破坏现有行为（除非明确声明“Breaking Change”）
2. 构建通过：`./mvnw -q -DskipTests` 无编译错误；涉及逻辑改动需跑相关测试
3. 新/改逻辑：新增或更新测试覆盖主路径 + 1 个边界（空 / 异常 / 极值）
4. 日志不记录敏感信息
5. 依赖：不漂移版本，不引入重复（遵循 BOM）
6. 安全：无硬编码凭证；外部输入已校验
7. 文档：公共 API / 配置 / 行为变化同步更新
8. 提交说明：动机 + 主要变更 + 风险点 + 回滚方式
9. 若影响性能（>5% 潜在开销）需写明评估/缓解

## 5. 工作流程 (AI Workflow)
| 阶段 | 动作 | 说明 |
| ---- | ---- | ---- |
| 1. 发现 | 明确需求边界 | 输入 / 期望输出 / 约束 |
| 2. 取证 | `grep` / `read_file` | 定位相关模块 / Kotlin 包路径 |
| 3. 设计 | 草案 | 接口 / 数据流 / 状态 / 错误策略 |
| 4. 实施 | 最小编辑 | 避免大范围格式化 / 重排 |
| 5. 验证 | 构建 + 测试 | 单测 / 集成 / 标签过滤 |
| 6. 总结 | 输出说明 | 覆盖点 / 风险 / 后续建议 |

## 6. Kotlin 代码规范 (Kotlin Code Guidelines)
- 数据模型：优先 `data class` + 不可变 (`val`)；衍生属性用计算属性
- 函数：单一职责；公共 API 明确返回类型（不要依赖类型推断隐藏意图）
- Null 安全：优先非空类型；必要时使用 `?` + `?:` + `require` / `check`
- 扩展函数：适合局部语义增强；避免把业务流程埋进扩展
- Sealed 层次：对受限状态建模时优先 `sealed interface / class`
- 异常：业务期望分支用类型建模或返回值，不滥用异常；不可恢复错误抛出 Runtime
- KDoc：公共类 / 接口 / 函数必须包含：一句话概述 + 参数 + 返回 + 异常（若有）
- 命名：简洁可读；布尔用语义 `is / has / can`
- 性能：热路径避免频繁装箱；字符串拼接用模板或日志占位符；延迟创建昂贵对象
- 日志：`logger.info("Task {} finished in {} ms", taskId, cost)`（保持与 Java Logger 统一模式）
- 不修改：与当前无关文件的 import 顺序 / 缩进 / 空行

## 7. Java 代码补充 (Java Interop)
- 若修改现有 Java：保持原风格，不强制 Kotlin 化
- 可重写的 util/常量类才逐步迁移 Kotlin（小粒度、可回滚）
- 共享常量优先放 Kotlin `object` 或 Java `final class` 内

## 8. 测试策略 (Testing Strategy – 与根 README 对齐)
根 `README-AI.md` 定义的测试分类需在此延伸执行：
- 测试类型配比：Unit ≈70% / Integration ≈25% / E2E ≈5%
- 目录：
  - 各模块：`src/test/kotlin/...`
  - 集成 / E2E：`pulsar-tests/`；公共工具：`pulsar-tests-common/`
- 命名：
  - 单元：`<ClassName>Test.kt`
  - 集成：`<ClassName>IT.kt` 或 `<ClassName>Tests.kt`
  - E2E：`<ClassName>E2ETest.kt`
  - 测试方法：使用反引号 + BDD：`Given X When Y Then Z`
- 标签（JUnit 5 @Tag）：`UnitTest` / `IntegrationTest` / `E2ETest` / `ExternalServiceTest` / `TimeConsumingTest` / `HeavyTest` / `SmokeTest` / `BenchmarkTest`
- 覆盖目标：
  - 全局 ≥70%；核心逻辑 ≥80%；工具 ≥90%；控制层 ≥85%
- 质量约束：
  - 单测 <100ms；集成 <5s；E2E <30s
  - 零容忍 flaky（复现后必须修复或暂时隔离并标明原因）
- 性能 / 基准：使用 `@Tag("BenchmarkTest")` 归类；必要时后续引入 JMH 模块
- 外部依赖：有限 Mock；E2E 尽量真实环境
- 典型断言：AssertJ / Kotlin test DSL（语义化）

## 9. 测试执行建议 (Test Execution Tips)
示例：
```
./mvnw -q -DskipTests            # 跳过测试构建
./mvnw test -Dtest="*CoreTest"   # 选择性运行
./mvnw test -Dgroups=UnitTest    # 运行指定 Tag（若配置 Surefire 分组）
./mvnw -pl pulsar-core -am test  # 仅构建依赖并测试指定模块
```

## 10. 文档与注释 (Docs & Comments)
- KDoc 解释“做什么 + 为什么 + 约束”
- 复杂流程（状态机 / DSL / Parser）：顶部块注释：意图 + 数据流 + 复杂度 + 边界
- 配置：示例 + 默认值 + 影响说明（性能/安全）
- 更新策略：公共接口 / 行为 / 配置改动 → 同步 README / Changelog（若存在）

## 11. 安全与合规 (Security & Compliance)
- 不提交密钥 / Token / 私有域名；使用占位 `${ENV_VAR}` 或 `<REDACTED>`
- 输入校验：长度 / 格式 / 枚举 / 范围；避免未限制集合增长
- 序列化：复用项目现有（Jackson/Gson 等）配置；禁止动态反射执行外部输入
- 外部调用：增加超时 / 重试（幂等场景）/ 限速视上下文
- 依赖升级：必须写理由（漏洞 / 性能 / 兼容）+ 回归验证方法

## 12. 性能注意事项 (Performance)
- 热路径：避免多层装饰器链；必要时内联或缓存策略
- I/O：批量化 / 连接复用 / 降低阻塞（可评估协程 / Reactor）
- 内存：避免大集合一次性加载，采用流式处理
- 指标：采集异步化，不阻塞主流程

## 13. 常见检查清单 (Review Checklist)
- [ ] 最小必要修改
- [ ] 构建通过 & 无新增编译告警
- [ ] 测试新增 / 覆盖边界
- [ ] Kotlin / Java 风格一致
- [ ] Null 安全 & 并发安全
- [ ] 日志无敏感数据
- [ ] 无未使用代码 / 依赖
- [ ] 性能影响已评估 / 说明
- [ ] 文档同步更新
- [ ] 提交信息规范

## 14. 构建与运行 (Build & Run)
Windows (cmd)：
```
mvnw.cmd -v
mvnw.cmd -q -DskipTests
mvnw.cmd test -Dtest="*CoreTest"
```
Linux / macOS：
```
./mvnw -q -DskipTests
./mvnw test -Dtest="*CoreTest"
```
多模块：
```
./mvnw -pl pulsar-core -am test
```

## 15. 基准测试 (Benchmarks)
- 模块：`pulsar-benchmarks`（不参与 deploy/release profile 发布）
- 目的：微基准监测热路径性能回归（字符串/解析/DOM/评分等）
- 运行示例：
```
./mvnw -pl pulsar-benchmarks -am package -DskipTests
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar -f 1 -wi 3 -i 5
```
- 规范：
  - JMH 基准类命名：`<Domain><Operation>Benchmark`
  - 避免 I/O；数据在 `@Setup` 中准备
  - 需含简短注释：目的 + 指标 + 回归触发因素
  - 不在循环里做日志 / I/O

## 16. 模板与资源 (Templates & Resources)
目录：`docs/copilot/templates/`
- `response-template.md`：标准 AI 响应格式
- `pr-description-template.md`：PR 描述模板
- `test-tag-usage.md`：测试标签使用示例

使用建议：
- 新增/修改 PR：必须填 Summary / Motivation / Risk / Rollback
- 性能相关变更：附临时基准或说明“无需基准（理由）”
- 标记潜在 flaky：附失败堆栈摘要 + 标签

## 17. 约定速查 (Quick Reference)
| 项 | 约定 |
|----|------|
| 构建 | `./mvnw` 统一入口 |
| 语言 | Kotlin 主导，渐进式兼容 Java |
| 测试命名 | `XxxTest` / `XxxIT` / `XxxE2ETest` |
| 测试标签 | `UnitTest` / `IntegrationTest` / ... |
| 覆盖率 | 全局≥70% / 核心≥80% |
| 日志 | 占位符 + 低噪声 + 无敏感信息 |
| 依赖管理 | BOM 统一；禁止重复版本 |
| 配置层次 | application*.properties 分层覆盖 |
| 文档 | 公共 API 必有 KDoc / README 条目 |
| 基准 | JMH：`pulsar-benchmarks` 模块 |
| 质量脚本 | `tools/quality/quality-check.sh` |

## 18. 后续可改进 (Future Enhancements)
- 基准覆盖扩展：DOM Diff / Selector Matching / QL 解析
- 基准阈值集成：CI 中可选性能守卫（允许软失败）
- 覆盖率增量报告：对比上次主分支
- 自动生成“缺失测试骨架”报告
- Flaky 统计面板（频率 / 首次出现时间）

---
若 AI 执行任务时需偏离本规范：必须显式列出 “偏离项 + 原因 + 风险 + 回滚方式”。
