# 🚦 AI Coder Agent Guideline for Text-To-Action Testing

这个文件指导AI代理如何为Text-To-Action功能生成、执行和改进测试。

## 📋 前置条件

在开始之前，请阅读以下文档以了解项目全貌：

1. **项目根目录** `README-AI.md` - 全局开发规范和项目结构
2. **核心功能文档** `pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/README-AI.md` - TTA核心实现指南

## 🎯 测试目标

测试 **Text-To-Action (TTA)** 功能，确保AI能够正确地将用户的自然语言指令转换为可执行的WebDriver操作。

**核心测试对象：**
- `ai.platon.pulsar.skeleton.ai.tta.TextToAction` 类
- 自然语言 → WebDriver API 转换的准确性
- 交互元素识别和选择的可靠性
- DOM变化下元素引用的稳定性

## 🏗 测试环境配置

### 目录结构
```
pulsar-tests/src/test/kotlin/ai/platon/pulsar/tta/    # 测试代码目录
├── TextToActionTestBase.kt                          # 测试基类
├── TextToActionTest.kt                              # 基础功能测试
├── TextToActionComprehensiveTests.kt                # 综合测试
├── InteractiveElementExtractionTests.kt             # 元素提取测试
└── README-AI.md                                     # 本文件

pulsar-tests/src/main/resources/static/generated/    # 测试网页目录
├── interactive-1.html                               # 基础交互测试页面
├── interactive-2.html                               # 复杂表单测试页面
├── interactive-3.html                               # 动态内容测试页面
├── interactive-4.html                               # 高级交互测试页面
└── interactive-screens.html                         # 多屏幕布局测试页面
```

### 环境要求
- **Java版本**: 根据根目录 `pom.xml` 确定
- **构建工具**: 使用 `./mvnw` (Maven wrapper)
- **LLM配置**: 需要配置AI模型API密钥
- **网页服务器**: 继承 `WebDriverTestBase` 自动启动

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
  - `${project.baseDir}/application*.properties`
  - `AppPaths.CONFIG_ENABLED_DIR/application*.properties`
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
fun `When ask to click a button then generate correct WebDriver action code`() {
    // 测试实现
}

@Test
fun `Given complex form when ask to fill specific field then select correct element`() {
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
- **创建新网页条件**: 当现有网页无法覆盖特定测试场景时
- **网页命名规范**: `interactive-<number>.html` 或 `<feature>-test.html`

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
fun `When given clear action command then generate precise WebDriver code`() {
    val command = "点击登录按钮"
    val result = textToAction.generateWebDriverActions(command, interactiveElements)
    
    assertThat(result).contains("click")
    assertThat(result).contains("登录")
}
```

#### 边界测试
```kotlin
@Test
fun `When no matching element exists then generate empty suspend function`() {
    val command = "点击不存在的按钮"
    val result = textToAction.generateWebDriverActions(command, emptyList())
    
    assertThat(result).contains("suspend")
    assertThat(result).doesNotContain("click")
}
```

#### 错误处理测试
```kotlin
@Test
fun `When given ambiguous command then request clarification or select best match`() {
    val command = "点击按钮"  // 模糊指令
    val result = textToAction.generateWebDriverActions(command, multipleButtons)
    
    // 验证处理策略
}
```

## 🎯 重点测试场景

### 1. 基础操作转换
- 点击操作: "点击登录按钮" → `driver.findElement().click()`
- 输入操作: "在搜索框输入AI工具" → `driver.findElement().sendKeys("AI工具")`
- 滚动操作: "滚动到页面中间" → `driver.executeScript("window.scrollTo...")`
- 导航操作: "返回上一页" → `driver.navigate().back()`

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

> 💡 **提示**: 本文档应该随着项目功能演进而定期更新。如发现测试覆盖盲区或新的测试需求，及时补充相应的测试指导。
