数据提取
=

PulsarR 使用 [jsoup](https://jsoup.org/) 从 HTML 文档中提取数据。 Jsoup 将 HTML 解析为与现代浏览器相同的 DOM。查看 [selector-syntax](https://jsoup.org/cookbook/extracting-data/selector-syntax) 以查阅所有受支持的 CSS 选择器，[这里](https://www.w3school.com.cn/cssref/css_selectors.asp) 也有一份中文版标准 CSS 选择器的详细表格。

现代网页源代码变化非常频繁，但是网页“看上去”不会变太多，以保证一致的用户体验。这时候，从视觉特征去审视网页元素就特别有效。为了更好地从视觉特征和数字特征来看待网页，PulsarR 扩展了 CSS，来解决最复杂的现实问题。

首先准备一个文档：

```kotlin
// 加载一个页面，如果该页面已过期，或者该页面为首次加载，则从互联网上下载该页面
val page = session.load(url, "-expires 1d")
// 将一个网页内容解析为Jsoup文档
val document = session.parse(page)
```

最简单的数据提取：

```kotlin
// 使用该文档做一些事情
val title = document.selectFirst('.title').text()
val price = document.selectFirst('.price').text()
```

稍稍复杂一点的例子：

```kotlin
// a with href
val links = document.select("a[href]")

// img with src ending .png
val pngs = document.select("img[src$=.png]")

// div with class=masthead
val masthead = document.select("div.masthead").first()

// direct a after h3
val resultLinks = document.select("h3.r > a")

// finds sibling td element preceded by th who contains text "Best Sellers Rank"
val bsr = document.select("th:contains(Best Sellers Rank) ~ td")
```

PulsarR 扩展了 CSS 和 Jsoup，来解决复杂的现实问题：

1. PulsarR 为 DOM 的每个 Node 计算了数值特征
2. PulsarR 扩展了 CSS 语法，来支持在 CSS 查询中使用数学运算
3. PulsarR 提供了一批实用方法来简化和增强 DOM 操作

为了弥补现有 CSS 表达式的不足，PulsarR 扩展了 CSS 表达式，引入了 **:expr(expresson)** 伪选择器，使得我们可以在 CSS 中做数学运算。

譬如：

```
/* 选择左侧栏宽 500 的链接 */
a:expr(left < 100 && width == 500)

/* 选择面积超过 1600 的图片 */
img:expr(width * height > 1600)

/* 选择文档中所有 div 元素，这些元素需要包含一张图片，宽和高都在 400 ~ 500 之间 */
div:expr(img == 1 && width > 400 && width < 500 && height > 400 && height < 500)
```

选择文档中所有 400 x 400 的图片：

```kotlin
val imgs400x400 = document.select("img:expr(width == 400 && height == 400)")
```

选择文档中所有 div 元素，这些元素需要包含一张图片，宽和高都在 400 ~ 500 之间：

```kotlin
val expr = "img == 1 && width > 400 && width < 500 && height > 400 && height < 500"
val elements = doc.select("div:expr($expr)")
```

目前，PulsarR 支持以下数值特征：

```
top,       // the top coordinate in pixel of the element
left,      // the left coordinate in pixel of the element
width,     // the width in pixel of the element
height,    // the height in pixel of the element
char,      // number of chars inside this node
txt_nd,    // number of descend text nodes
img,       // number of descend images
a,         // number of descend anchors
sibling,   // number of sibling nodes
child,     // number of children
dep,       // node depth
seq,       // node sequence in the document
txt_dns    // text node density
```

下面的表格列出了标准运算符。

### 算术运算符

| Name | Description                             |
| ---- | --------------------------------------- |
| -    | The prefix minus operator, like in “-2” |
| +    | The prefix minus operator, like in “+2” |
| -    | The infix minus operator, like in “5-2” |
| +    | The infix plus operator, like in “5+2”  |
| *    | The multiplication operator             |
| /    | The division operator                   |
| ^    | The power-of operator                   |
| %    | The modulo operator (remainder)         |

### 布尔运算符

| Name   | Description                         |
| ------ | ----------------------------------- |
| =, ==  | The equals operator                 |
| !=, <> | The not equals operator             |
| !      | The prefix not operator, like in !a |
| >      | The greater than operator           |
| >=     | The greater equals operator         |
| <      | The less than operator              |
| <=     | The less equals operator            |
| &&     | The and operator                    |
| \|\|   | The or operator                     |

在 PulsarR 专业版中，我们将会引入更多有趣的数值特征来支持机器学习和[人工智能](https://zhuanlan.zhihu.com/p/576098111)，譬如拓扑结构相关的特征。

 在 [X-SQL](13X-SQL.md) 章节，我们将会详细介绍如何在 X-SQL 中使用 CSS 选择器来选择元素及其属性。一个综合性较强的真实案例是 [x-asin.sql](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)（[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon/blob/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)），它使用了各种各样的 CSS 选择器来解决最复杂的电商网页数据提取问题，下面是一些片段：

```sql
dom_first_attr(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)', 'data-old-hires') as `imgsrc`,
dom_first_attr(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)', 'data-a-dynamic-image') as `dynamicimgsrcs`,
dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)') as `img`,
dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
dom_first_text(dom, '#price #priceblock_dealprice, #price tr td:contains(Deal of the Day) ~ td') as `withdeal`,
dom_first_text(dom, '#price #dealprice_savings .priceBlockSavingsString, #price tr td:contains(You Save) ~ td') as `yousave`,
```
------

[Prev](3load-options.md) [Home](1home.md) [Next](5URL.md)
