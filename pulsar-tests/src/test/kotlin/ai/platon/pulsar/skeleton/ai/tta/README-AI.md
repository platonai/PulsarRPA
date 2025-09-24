# 🚦 AI Coder Agent Guideline for Text-To-Action Testing

这个文件指导AI代理如何为Text-To-Action功能生成、执行和改进测试。

## 📋 前置条件

在开始之前，请阅读以下文档以了解项目全貌：

1. **项目根目录** `README-AI.md` - 全局开发规范和项目结构
2. **核心功能文档** `pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/README-AI.md` - TTA核心实现指南
3. 所有测试都必须使用 Mock Server 的网页进行测试，这些网页资源位于`pulsar-tests-common/src/main/resources/static/generated/tta`目录下

## 🎯 测试目标

测试 **Text-To-Action (TTA)** 功能，确保AI能够正确地将用户的自然语言指令转换为可执行的WebDriver操作。

**核心测试对象：**
- `ai.platon.pulsar.skeleton.ai.tta.TextToAction#generateWebDriverAction` 方法
- 仅测试 generateWebDriverAction 方法，忽略其他方法
- 自然语言 → WebDriver API 转换的准确性
- 交互元素识别和选择的可靠性
- DOM变化下元素引用的稳定性

## 🏗 测试环境配置

### 目录结构
```
pulsar-tests/src/test/kotlin/ai/platon/pulsar/tta/    # Test code directory
├── TextToActionTestBase.kt                          # Test base class
├── TextToActionTest.kt                              # Basic functionality tests
├── TextToActionComprehensiveTests.kt                # Comprehensive tests
├── InteractiveElementExtractionTests.kt             # Element extraction tests
└── README-AI.md                                     # This file

# Note: The actual interactive test web pages are stored in a shared module (used by multiple test modules)
pulsar-tests-common/src/main/resources/static/generated/tta  # Actual test web page directory
├── interactive-1.html                               # Basic interactions
├── interactive-2.html                               # Complex forms
├── interactive-3.html                               # Animation/basic dynamics
├── interactive-4.html                               # Dark mode + drag-and-drop
└── interactive-screens.html                         # (Currently still a single-page placeholder)
```

### 环境要求
- **Java版本**: 根据根目录 `pom.xml` 确定
- **构建工具**: 使用 `./mvnw` (Maven wrapper)
- **LLM配置**: 需要配置AI模型API密钥
- **网页服务器**: 继承 `WebDriverTestBase` 自动启动
- **WebDriver 对象**: 继承 `WebDriverTestBase`，使用 `runWebDriverTest` 获得

## 🔧 测试基础设施

### 测试基类继承关系
```text
WebDriverTestBase              # 提供网页服务器和WebDriver支持
    ↓
TextToActionTestBase          # TTA专用测试基础设施
    ↓
具体测试类                     # 实际测试实现
```

### LLM配置检查
测试基类会自动检查LLM配置：
- 如果未配置API密钥，测试将被跳过并显示配置提示
- 配置文件位置
  - `${project.baseDir}/application[-private].properties`
  - `AppPaths.CONFIG_ENABLED_DIR/application[-private].properties`
- 支持环境变量配置

## 📝 测试编写规范

### 1. 测试文件命名
- 功能测试: `<Feature>Test.kt` (如 `TextToActionTest.kt`)
- 综合测试: `<Feature>ComprehensiveTests.kt`
- 集成测试: `<Feature>IT.kt`

### 2. 测试方法命名
使用反引号描述性命名：
```kotlin
@Test
fun `When ask to click a button then generate correct WebDriver action code`() = runWebDriverTest(browser) { driver ->
    // 测试实现
}

@Test
fun `Given complex form when ask to fill specific field then select correct element`() = runWebDriverTest(browser) { driver ->
    // 测试实现
}
```

### 3. 测试注解使用
```kotlin
@Tag("ExternalServiceTest")    // 需要外部服务（LLM API）
@Tag("TimeConsumingTest")      // 耗时测试
@SpringBootTest(classes = [Application::class], 
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
```

## 🧪 测试策略

### 1. 测试网页选择策略
- **优先使用现有测试网页**: 检查 `interactive-*.html` 是否满足测试需求
- **创建新网页条件**: 当现有网页无法覆盖特定测试场景时（动态加载、歧义解析、Shadow DOM 等）
- **命名规范**: `interactive-<number>.html` 或 `<feature>-test.html`

### 2. 测试层次划分

#### 单元测试 (Unit Tests)
- 测试单个方法的正确性
- 不依赖外部LLM服务（使用mock）
- 快速执行，覆盖边界条件

#### 集成测试 (Integration Tests) 
- 测试TTA与LLM的真实交互
- 使用真实API调用
- 验证端到端的转换流程

#### E2E测试 (End-to-End Tests)
- 完整的用户场景测试
- 从自然语言输入到浏览器操作执行
- 验证最终的自动化效果

### 3. 测试用例设计原则

#### 正向测试
```kotlin
@Test
fun `When given clear action command then generate precise WebDriver code`() = runWebDriverTest(browser) { driver ->
    val command = "点击登录按钮"
    val result = textToAction.generateWebDriverAction(command, driver)
    assertThat(result).contains("click")
}
```

#### 边界测试
```kotlin
@Test
fun `When no matching element exists then generate empty suspend function`() = runWebDriverTest(browser) { driver ->
    val result = textToAction.generateWebDriverAction("点击不存在的按钮", driver)
    assertThat(result).doesNotContain("click")
}
```

#### 歧义/恢复测试
```kotlin
@Test
fun `When ambiguous command then choose best match or ask clarify`() = runWebDriverTest(browser) { driver ->
    val result = textToAction.generateWebDriverAction("点击按钮", driver)
    // 验证策略
}
```

## 🎯 重点测试场景

### 1. 基础操作转换
- 点击操作: "点击登录按钮" → `driver.click()`
- 输入操作: "在搜索框输入AI工具" → `driver.type("AI工具")`
- 滚动操作: "滚动到页面中间" → `driver.evaluate("window.scrollTo...")`
- 导航操作: "返回上一页" → `driver.back()`

### 2. 元素选择准确性
- 通过文本匹配: "点击提交按钮"
- 通过位置描述: "点击右上角的菜单"
- 通过功能描述: "选择搜索框"
- 处理相似元素: 多个按钮时的精确选择

### 3. 复杂场景处理
- 动态加载内容的元素识别
- 表单填写的字段匹配
- 多步骤操作的序列生成
- 条件判断逻辑的处理

### 4. 错误和边界情况
- 元素不存在时的fallback策略
- 模糊指令的处理
- DOM结构变化的适应性
- 超时和异常的处理

## 📊 测试覆盖率要求

- **指令覆盖率**: 90%+ 常见用户指令类型
- **元素类型覆盖率**: 85%+ HTML交互元素类型
- **代码覆盖率**: 70%+ (JaCoCo配置)
- **场景覆盖率**: 100% 核心用户场景

---
## ✅ 当前测试网页能力与差距摘要
| 页面 | 已含能力 | 主要缺失 |
|------|----------|----------|
| interactive-1 | 基础输入/选择/按钮/显隐/简单计算 | 多按钮歧义/错误态/滚动长内容 |
| interactive-2 | 多控件表单/滑块/订阅开关/动态字体 | 表单验证/多步骤/条件显示/file/radio |
| interactive-3 | IntersectionObserver动画/范围控制/显隐切换 | 真异步加载/列表增删/懒加载/分页 |
| interactive-4 | 暗色模式/拖拽排序 | 跨列表拖拽/撤销/Shadow DOM/多拖拽类型 |
| interactive-screens | 与 1 类似（占位） | 真正多屏/Tab/iframe/分栏/路由感知 |

> 结论：需要新增专用页面覆盖：动态异步、歧义冲突、Shadow DOM、可访问性、媒体/富文本、多屏结构。

## 🧩 元素类型覆盖进度（概览）
- 已覆盖: text/email/number/range/textarea/select/checkbox/button/a/draggable list/toggle(自制)/slider
- 未覆盖（优先）: password/search/date/time/file/radio/progress/meter/dialog/modal/contenteditable/iframe/video/audio/canvas/disabled/readonly/aria-live/Shadow DOM

## 🗺 改进路线（分阶段）
1. Phase 1（结构修复）
   - 测试网页实际目录修改为 pulsar-tests-common/src/main/resources/static/generated/tta
   - 重命名interactive-<number>.html，使用可读性强的名字
   - 修正文档路径说明（已完成）
   - 修复暗色模式
   - 重写 interactive-screens 为真正多屏：Tab + iframe + anchor + 长滚动区
2. Phase 2（动态与歧义）
   - 新增 `interactive-dynamic.html`：异步加载(setTimeout)、列表增删、懒加载图片、虚拟滚动占位
   - 新增 `interactive-ambiguity.html`：重复按钮/同文本不同区域/data-testid 策略
3. Phase 3（高级控件）
   - `forms-advanced-test.html`: radio/file/date/time/password/验证错误态/disabled/readonly
   - `modal-dialog-test.html`: 自定义 dialog + focus trap + ESC 关闭
4. Phase 4（平台/可访问性）
   - `shadow-components-test.html`: open/closed shadow + slot
   - `a11y-test.html`: landmarks/nav/main/aria-label/aria-live/aria-expanded
   - `media-rich-test.html`: video/audio/canvas/contenteditable
5. Phase 5（策略验证）
   - 编写元素定位优先级测试：data-testid > aria-label > role+name > 文本 > 相对位置
   - 加入 dom 置换 / stale element 重试测试

## 🏷 定位与命名规范补充
- 为歧义消解引入: `data-testid="tta-<domain>-<seq>"`
- Shadow DOM 元素：外层再加 wrapper `data-scope="shadow-demo"`
- 动态插入元素：添加 `data-dynamic="true"` 便于过滤

## 🔁 推荐测试辅助方法（后续可在基类中补充）
- `waitFor(selector, timeout)` 条件等待
- `retrying(action)` 处理暂时性 stale
- `byTestId(id)` 简化选择器

## 📌 新增/更新页面的验收清单
- 是否引入新元素类型
- 是否提供至少 1 个歧义选择场景
- 是否包含动态/延迟/可失败交互
- 是否添加 data-testid / aria 元数据
- 是否在 README 能力表中登记

## 🧪 质量度量改进建议
- 脚本统计元素种类：扫描 `static/generated/*.html` 输出覆盖率
- 统计测试指令语料类型分布（动作/目标/修饰）
- 失败分类：解析失败/定位失败/执行失败/超时

---
## 🚀 测试执行命令

```bash
# 运行所有TTA测试
./mvnw test -Dtest="ai.platon.pulsar.tta.**"

# 运行特定测试类
./mvnw test -Dtest="TextToActionTest"

# 跳过需要LLM的测试
./mvnw test -Dtest="**" -DexcludedGroups="ExternalServiceTest"

# 运行覆盖率报告
./mvnw clean test jacoco:report
```

## 📈 持续改进指导

### 1. 测试维护
- 定期更新测试网页以覆盖新的UI模式
- 根据用户反馈添加真实场景测试
- 监控测试执行时间，优化慢速测试

### 2. 质量监控
- 跟踪TTA转换的准确率
- 监控LLM API调用的成功率和延迟
- 分析失败用例，改进指令理解

### 3. 测试数据管理
- 维护标准测试指令集
- 收集和分类边界用例
- 建立回归测试基准

---
> 💡 **提示**: 本文档应随功能演进更新。发现覆盖盲区请优先：登记差距 → 设计页面 → 编写用例 → 更新能力表。
