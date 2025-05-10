Data Extraction
=

[Prev](3load-options.md) [Home](1home.md) [Next](5URL.md)

PulsarRPA uses [jsoup](https://jsoup.org/) to extract data from HTML documents. Jsoup parses HTML into a DOM that mirrors modern browsers. Check out [selector-syntax](https://jsoup.org/cookbook/extracting-data/selector-syntax) for all supported CSS selectors, and [here](https://www.w3school.com.cn/cssref/css_selectors.asp) for a detailed table of standard CSS selectors in Chinese.

Modern web page source codes change very frequently, but the web page "looks" not much different, to ensure a consistent user experience. At this time, examining web page elements from visual features is particularly effective. To better view web pages from visual and numerical features, PulsarRPA extends CSS to solve the most complex real-world problems.

First, prepare a document:

```kotlin
// Load a page, or download it from the internet if the page has expired or is being loaded for the first time
val page = session.load(url, "-expires 1d")
// Parse the web page content into a Jsoup document
val document = session.parse(page)
```

The simplest data extraction:

```kotlin
// Do something with the document
val title = document.selectFirstTextOrNull('.title')
val price = document.selectFirstTextOrNull('.price')
```

A slightly more complex example:

```kotlin
// a with href
val links = document.select("a[href]")

// img with src ending .png
val pngs = document.select("img[src$=.png]")

// div with class=masthead
val masthead = document.select("div.masthead")

// direct a after h3
val resultLinks = document.select("h3.r > a")

// finds sibling td element preceded by th who contains text "Best Sellers Rank"
val bsr = document.select("th:contains(Best Sellers Rank) ~ td")
```

PulsarRPA extends CSS and Jsoup to solve complex real-world problems:

1. PulsarRPA calculates numerical features for each Node in the DOM.
2. PulsarRPA extends CSS syntax to support mathematical operations in CSS queries.
3. PulsarRPA provides a set of practical methods to simplify and enhance DOM operations.

To make up for the shortcomings of existing CSS expressions, PulsarRPA extends CSS expressions by introducing the **:expr(expresson)** pseudo-selector, allowing us to perform mathematical operations in CSS.

For example:

```css
/* Select links in the left column with a width of 500 */
a:expr(left < 100 && width == 500)

/* Select images with an area greater than 1600 */
img:expr(width * height > 1600)

/* Select all div elements in the document that contain an image, with a width and height between 400 and 500 */
div:expr(img == 1 && width > 400 && width < 500 && height > 400 && height < 500)
```

Select all 400x400 images in the document:

```kotlin
val imgs400x400 = document.select("img:expr(width == 400 && height == 400)")
```

Select all div elements in the document that contain an image, with a width and height between 400 and 500:

```kotlin
val expr = "img == 1 && width > 400 && width < 500 && height > 400 && height < 500"
val elements = doc.select("div:expr($expr)")
```

Currently, PulsarRPA supports the following numerical features:

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

The table below lists the standard operators.

### Arithmetic Operators

| Name | Description                             |
| ---- | --------------------------------------- |
| -    | The prefix minus operator, like in “-2”  |
| +    | The prefix plus operator, like in “+2”   |
| -    | The infix minus operator, like in “5-2” |
| +    | The infix plus operator, like in “5+2”  |
| *    | The multiplication operator             |
| /    | The division operator                    |
| ^    | The power-of operator                    |
| %    | The modulo operator (remainder)           |

### Boolean Operators

| Name    | Description                          |
| ------- | ------------------------------------ |
| =, ==   | The equals operator                   |
| !=, <>  | The not equals operator               |
| !       | The prefix not operator, like in !a      |
| >       | The greater than operator             |
| >=      | The greater equals operator           |
| <       | The less than operator                |
| <=      | The less equals operator              |
| &&      | The and operator                      |
| \|\|    | The or operator                       |

In the professional version of PulsarRPA, we will introduce more interesting numerical features to support machine learning and [artificial intelligence](https://zhuanlan.zhihu.com/p/576098111), such as features related to topological structures.

In the [X-SQL](13X-SQL.md) chapter, we will详细介绍如何在 X-SQL 中使用 CSS 选择器来选择元素及其属性。一个综合性较强的真实案例是 [x-asin.sql](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)（[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon/blob/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)），它使用了各种各样的 CSS 选择器来解决最复杂的电商网页数据提取问题，下面是一些片段：

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
