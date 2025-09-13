# X-SQL

[英文](/docs/x-sql.md) | 简体中文

### 介绍

我们开发了X-SQL，它可以直接查询网络，并将网页转换成表格和图表。

X-SQL是建立在H2数据库之上的，因此它与H2兼容，SQL方言也是H2。

下面展示了一个典型的X-SQL查询。

```sql
select
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as category,
  dom_first_slim_html(dom, '#bylineInfo') as brand,
  dom_all_slim_htmls(dom, '#imageBlock img') as gallery,
  dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N   -i 1d -njr 3', 'body');
```

PulsarRPA主要通过注册UDF来扩展H2数据库，但它也进行了其他扩展。

每个X-SQL函数都有一个命名空间，例如：

```
dom_base_uri() -> dom
str_substring_after() -> str
```

在上面的例子中，`dom`和`str`是命名空间。如果一个X-SQL函数被声明为`hasShortcut`，那么可以忽略命名空间。

X-SQL函数不区分大小写，并且忽略所有的下划线(`_`)。

下面的X-SQL函数是相同的：

```sql
DOM_LOAD_AND_SELECT(url, 'body');
dom_loadAndSelect(url, 'body');
Dom_Load_And_Select(url, 'body');
dom_load_and_select(url, 'body');
dOm_____lo_AdaNd_S___elEct_____(url, 'body');
```

由于`LOAD_AND_SELECT`被声明为`hasShortcut`，可以忽略命名空间，下面的函数仍然是相同的：

```sql
LOAD_AND_SELECT(url, 'body');
loadAndSelect(url, 'body');
Load_And_Select(url, 'body');
load_and_select(url, 'body');
_____lo_AdaNd_S___elEct_____(url, 'body');
```

## 表格函数

每个表格函数返回一个ResultSet，并且可以出现在`from`子句中。

### LOAD_AND_SELECT

```sql
LOAD_AND_SELECT(url [, cssSelector [, offset [, limit]]])
```

加载一个页面并选择元素，返回一个ResultSet。结果集包含两列：`DOM`和`DOC`，两者都是`ValueDom`类型。

示例：

```sql
select
  dom_base_uri(dom)
from
  load_and_select('https://www.amazon.com/dp/B0FFTT2J6N',  'body', 1, 10)
```

## DOM函数

DOM函数旨在查询DOM属性。每个DOM函数接受一个名为`ValueDom`的参数，它是一个Jsoup Element的包装器。

DOM函数定义在以下文件中：[DomFunctions](https://github.com/apache/pulsar/blob/master/pulsar-ql/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs/DomFunctions.kt)。

所有DOM函数都在命名空间`DOM`中。

### DOM_BASE_URI

```sql
DOM_BASE_URI(dom)
```

返回HTML文档的URI。

示例：

```sql
select dom_base_uri(dom) from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N',  'body')
```

## DOM选择函数

DOM选择函数旨在从DOM中查询元素及其属性。

每个DOM函数接受一个名为`DOM`的参数（不区分大小写），类型为`ValueDom`，它是一个Jsoup `Element`的包装器。

DOM选择函数通常也接受一个名为`cssSelector`的参数，用于选择`DOM`的子元素。

最重要的DOM选择函数定义在以下文件中：[DomSelectFunctions](https://github.com/apache/pulsar/blob/master/pulsar-ql/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs/DomSelectFunctions.kt)。

所有DOM选择函数都在命名空间`DOM`中。

### DOM_FIRST_TEXT

```sql
DOM_FIRST_TEXT(dom, cssSelector)
```

返回`cssSelector`在`dom`内选择的第一个元素的文本内容，类似于以下JavaScript代码。

```javascript
dom.querySelector(cssSelector).textContent
```

示例：

```sql
select
  dom_first_text(dom, '#productName') as Name,
  dom_first_text(dom, '#price') as Price,
  dom_first_text(dom, '#star') as StarNum
from
  load_and_select('https://www.example.com/zgbs/appliances',  'ul.item-collection li.item')
```

### DOM_ALL_TEXTS

```sql
DOM_ALL_TEXTS(dom, cssSelector)
```

返回`cssSelector`在`dom`内选择的所有元素的文本内容数组，类似于以下JavaScript伪代码。

```javascript
dom.querySelectorAll(cssSelector).map(e => e.textContent)
```

示例：

```sql
select
  dom_all_texts(dom, 'ul li.item a.name') as ProductNames,
  dom_all_texts(dom, 'ul li.item span.price') as ProductPrices,
  dom_all_texts(dom, 'ul li.item span.star') as ProductStars
from
  load_and_select('https://www.example.com/zgbs/appliances',  'div.products')
```

## 字符串函数

大多数字符串函数是通过编程自动从`org.apache.commons.lang3.StringUtils`转换而来。您可以在以下文件中找到UDF定义：[StringFunctions](https://github.com/apache/pulsar/blob/master/pulsar-ql/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs/StringFunctions.kt)。

所有字符串函数都在命名空间`STR`中。

### STR_SUBSTRING_AFTER

```sql
STR_SUBSTRING_AFTER(str, separator)
```

获取分隔符第一次出现后的子字符串。

示例：

```sql
select
  str_substring_after(dom_first_text(dom, '#price'), '$') as Price
from
  load_and_select('https://www.amazon.com/dp/B0FFTT2J6N',  'body');
```

