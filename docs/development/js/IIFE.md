## **IIFE**: Immediately Invoked Function Expression。

本文介绍立即执行函数表达式。

---

### ✅ 解释

`(() => { ... })()` 是一个 **箭头函数** 的 IIFE，完整结构如下：

```js
(() => {
  // 这里是函数体
  return something;
})()
```

或者使用普通函数也可以：

```js
(function () {
  // 函数体
  return something;
})()
```

---

### 🔍 特点

* **定义后立即执行**：函数声明完毕后立刻调用。
* **作用域隔离**：常用于创建一个临时的作用域，避免变量污染全局环境。
* **常见用途**：初始化逻辑、模块封装、避免全局变量冲突等。

---

要不要我给你展示一个更复杂的 IIFE 应用示例？

要将任意 JavaScript 函数转换为 **IIFE（Immediately Invoked Function Expression，立即执行函数表达式）**，关键是两步：

1. **将函数变成表达式（expression）**：用括号包裹函数定义。
2. **紧随其后添加一对括号 `()` 调用它**。

---

### ✅ 一般函数转换示例：

#### 普通函数：

```js
function sayHello() {
  console.log('Hello!');
}
```

#### 转换为 IIFE：

```js
(function sayHello() {
  console.log('Hello!');
})();
```

或者使用箭头函数：

```js
(() => {
  console.log('Hello!');
})();
```

---

### ✅ 带参数的函数：

#### 普通函数：

```js
function greet(name) {
  console.log(`Hello, ${name}!`);
}
```

#### 转换为 IIFE：

```js
(function greet(name) {
  console.log(`Hello, ${name}!`);
})('Alice');
```

或者箭头函数写法：

```js
((name) => {
  console.log(`Hello, ${name}!`);
})('Alice');
```

---

### ✅ 小贴士：

* IIFE 最常用于**封装作用域、避免变量污染全局环境**。
* 可以省略函数名（匿名函数）如果不需要递归调用。

是否需要我写一个工具函数，把任意函数字符串自动转成 IIFE？

下面是一个简单的工具函数，可以将任意的 JavaScript 函数字符串转换成 IIFE（立即执行函数表达式）形式：

---

### ✅ 工具函数（适用于函数声明或函数表达式）：

```js
function toIIFE(fnString, args = '') {
  // 清理前后空白
  const trimmed = fnString.trim();

  // 判断是否是函数声明（以 "function" 开头）
  if (/^function\s*\w*\s*\(.*\)\s*\{/.test(trimmed)) {
    return `(${trimmed})(${args});`;
  }

  // 箭头函数或其他表达式
  return `(${trimmed})(${args});`;
}
```

---

### ✅ 示例用法：

```js
const func1 = `
function add(a, b) {
  return a + b;
}
`;

console.log(toIIFE(func1, '3, 5'));
// 输出： (function add(a, b) { return a + b; })(3, 5);

const func2 = `
(a, b) => a * b
`;

console.log(toIIFE(func2, '4, 6'));
// 输出： ((a, b) => a * b)(4, 6);
```

---

需要我扩展它来解析实际代码 AST 或支持多种语法边界情况吗？
下面是一个 **Kotlin 版本的“函数转 IIFE（立即执行函数表达式）”工具**，用于将任意传入的 JavaScript 函数字符串包装成 IIFE 形式：

---

### ✅ Kotlin 示例：将 JS 函数代码转换成 IIFE

```kotlin
fun toIIFE(jsFunctionCode: String, args: String = ""): String {
    val trimmed = jsFunctionCode.trim()

    return if (trimmed.startsWith("function") || trimmed.startsWith("(") || trimmed.startsWith("async") || trimmed.startsWith("x") || trimmed.startsWith("{")) {
        "(${trimmed})($args);"
    } else if (trimmed.contains("=>")) {
        "(${trimmed})($args);"
    } else {
        "// ❌ Unsupported format: not a valid JS function"
    }
}
```

---

### ✅ 示例用法：

```kotlin
fun main() {
    val fn1 = "function greet(name) { return 'Hello, ' + name; }"
    val fn2 = "(x, y) => x * y"
    val fn3 = "async function fetchData() { return await fetch('/api'); }"

    println(toIIFE(fn1, "'Vincent'"))
    println(toIIFE(fn2, "3, 4"))
    println(toIIFE(fn3))
}
```

---

### ✅ 输出结果：

```js
(function greet(name) { return 'Hello, ' + name; })('Vincent');
((x, y) => x * y)(3, 4);
(async function fetchData() { return await fetch('/api'); })();
```

---

这个 Kotlin 函数足够应对大多数 JS 函数声明、箭头函数、async 函数等使用场景。如果你希望引入完整 JS 解析（比如使用 [GraalVM](https://www.graalvm.org/) 或 Nashorn），也可以告诉我，我可以帮你写一个支持完整 AST 处理的 Kotlin 实现。

是否需要更强的 AST 解析版本？
