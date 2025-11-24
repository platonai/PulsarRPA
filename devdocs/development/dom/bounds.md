下面按类别系统地整理 DOM 中 **所有与元素位置 / 尺寸（布局、滚动、可见区域、绝对位置等）相关的属性与 API**，涵盖你在 web automation / CDP / layout 分析时最常用的所有信息。

---

# 🧱 一览：DOM 中与位置 / 尺寸相关的属性

## 🌕 1. **元素自身尺寸（Element Size）**

### **1.1 offset 系列（border-box）**

| 属性             | 含义                   | 包含内容                         |
| -------------- | -------------------- | ---------------------------- |
| `offsetWidth`  | 元素的可见宽度              | content + padding + border   |
| `offsetHeight` | 元素的可见高度              | content + padding + border   |
| `offsetTop`    | 与 offsetParent 顶部的距离 | border-box 外缘                |
| `offsetLeft`   | 与 offsetParent 左侧的距离 | border-box 外缘                |
| `offsetParent` | 定位参考的父元素             | 通常是最近 position != static 的元素 |

---

### **1.2 client 系列（padding-box）**

| 属性             | 含义                                      |
| -------------- | --------------------------------------- |
| `clientWidth`  | content + padding，不含 border & scrollbar |
| `clientHeight` | content + padding                       |
| `clientTop`    | border-top 宽度                           |
| `clientLeft`   | border-left 宽度                          |

> 注意：clientWidth 常用于**可见内容区域（不含滚动条）**。

---

### **1.3 scroll 系列（content-box + scroll）**

| 属性             | 含义            |
| -------------- | ------------- |
| `scrollWidth`  | 内容总体宽度（可滚动内容） |
| `scrollHeight` | 内容总体高度        |
| `scrollTop`    | 内容向上滚动的距离     |
| `scrollLeft`   | 内容向左滚动的距离     |

---

## 🌕 2. **窗口 / 屏幕相关尺寸**

### window 级别

| 属性                   | 含义                     |
| -------------------- | ---------------------- |
| `window.innerWidth`  | 浏览器 viewport 宽度（包括滚动条） |
| `window.innerHeight` | 浏览器 viewport 高度        |
| `window.outerWidth`  | 包含浏览器 UI 的窗口宽度         |
| `window.outerHeight` | 包含浏览器 UI 的窗口高度         |

### document 级别

| 属性                                     | 含义                 |
| -------------------------------------- | ------------------ |
| `document.documentElement.clientWidth` | viewport 宽度（不含滚动条） |
| `document.documentElement.scrollTop`   | 页面滚动距离（整个文档）       |

---

## 🌕 3. **绝对位置（相对屏幕 / viewport）**

### **3.1 getBoundingClientRect()（最重要）**

返回一个 DOMRect：

* `x`, `y`
* `top`, `left`, `right`, `bottom`
* `width`, `height`

特点：

* **相对 viewport**
* 会随页面滚动实时变化
* 包含 CSS transform 的影响（非 matrix）

例：

```js
const rect = element.getBoundingClientRect();
rect.top;
rect.bottom;
rect.width;
rect.height;
```

---

### **3.2 CSS transform 后的位置**

使用：

* `element.getClientRects()`（分片盒子）
* `getBoundingClientRect()`（整个覆盖边界）

用于处理：

* inline 元素
* 多行文本
* transform 位移后的 bounding box

---

## 🌕 4. **布局（Computed Style）相关**

通过：

```js
window.getComputedStyle(elem)
```

可以取得：

* `width`（CSS width，不一定等于 layout width）
* `height`
* `margin`, `padding`, `border-width`
* `transform`, `translate`, `scale`
* `box-sizing`

特别重要：

| 属性           | 含义                               |
| ------------ | -------------------------------- |
| `box-sizing` | 内容计算模型（content-box / border-box） |
| `transform`  | 位移、旋转、缩放会影响视觉位置                  |

---

## 🌕 5. **元素位置 API**

### **element.offsetParent**

返回：

* 最近的定位容器（position: relative/absolute/fixed/sticky）

### **element.closest(selector)**

用于查找特定布局上下文。

---

## 🌕 6. **Ranges / Caret / 文本布局（更高级）**

用于文本节点的实际位置：

| API                             | 用途            |
| ------------------------------- | ------------- |
| `Range.getClientRects()`        | 文本碎片在屏幕上的所有矩形 |
| `Range.getBoundingClientRect()` | 文本区域边界        |
| `Selection.getRangeAt()`        | 获取当前光标区域      |

这在 CDP 自动化中非常有用（处理文本点击、hover）。

---

## 🌕 7. **Fullscreen / Visual Viewport（移动设备）**

特别用于移动端：

| 属性                                | 含义                         |
| --------------------------------- | -------------------------- |
| `window.visualViewport.width`     | 实际可见区域宽度（键盘弹出时会变化）         |
| `window.visualViewport.offsetTop` | 视觉区域相对 layout viewport 的偏移 |

---

# 🧭 总结：所有位置/尺寸属性分类图

```
尺寸（size）
 ├─ offsetWidth / offsetHeight  ← border-box
 ├─ clientWidth / clientHeight  ← padding-box
 └─ scrollWidth / scrollHeight  ← content (scrollable)

位置（position）
 ├─ offsetTop / offsetLeft      ← relative to offsetParent
 ├─ scrollTop / scrollLeft      ← internal scroll
 └─ getBoundingClientRect()     ← relative to viewport

元素边界（box）
 ├─ clientTop / clientLeft      ← border width
 ├─ margin/padding/border       ← computed style
 └─ box-sizing                  ← content-box / border-box

窗口（window/document）
 ├─ window.innerWidth/innerHeight
 ├─ documentElement.clientWidth
 └─ documentElement.scrollTop

可视区域（移动）
 └─ visualViewport.*
```

---

如果你愿意，我还可以给你：

* **所有属性的可视化示意图（ASCII）**
* **一个表格列出所有属性的包含关系**
* **用一个元素变化演示这些属性如何变化（JS demo）**
* **浏览器自动化中（CDP/Playwright）推荐如何精确计算元素绝对位置**

你需要继续哪一种？
