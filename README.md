Pulsar README
===================

Pulsar is an open source solution to scrape web data at scale.

Extracting web data at scale is extremely hard. Websites change frequently and are becoming more complex, meaning web data collected is often inaccurate or incomplete, pulsar is an open source solution to address such issues.

Pulsar supports the Network As A Database paradigm, so we can turn the Web into tables and charts using simple SQLs, and we can query the web using SQL directly.

We also have a plan to release an advanced AI to automatically extract every field in webpages with notable accuracy.

![product-screenshot](docs/images/pulsar-product-screenshot-1.png)

# Features
- Web spider: browser rendering, ajax data crawling
- Performance: highly optimized, rendering hundreds of pages in parallel on a single machine
- Data quantity assurance: smart retry, accurate scheduling, web data lifetime management
- Large scale: fully distributed, designed for large scale crawling
- Simple API: single line of code to scrape, or single SQL to turn a website into a table
- X-SQL: extend SQL to manage web data: Web crawling, scraping, Web content mining, Web BI
- Bot stealth: IP rotation, web driver stealth, never get banned
- RPA: imitating human behavior, SPA crawling, or do something else awesome
- Big data: various backend storage support: MongoDB/HBase/Gora
- Logs & metrics: monitored closely and every event is recorded

For more information check [platon.ai](http://platon.ai)
# Use pulsar as a library
Maven:
```
<dependency>
  <groupId>ai.platon.pulsar</groupId>
  <artifactId>pulsar-all</artifactId>
  <version>1.8.4</version>
</dependency>
```
Scrape a massive url collection:
```kotlin
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document

val parseHandler = { _: WebPage, document: Document ->
    // do something wonderful with the document
    println(document.title() + "\t|\t" + document.baseUri())
}
val urls = LinkExtractors.fromResource("seeds.txt").map { ParsableHyperlink(it, parseHandler) }
val context = PulsarContexts.create().asyncLoadAll(urls)
// feel free to add a huge number of urls to the crawl queue here using async loading
// ...
context.await()
```
## X-SQL

Scrape a single page:

```sql
select
    dom_first_text(dom, '#productTitle') as `title`,
    dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
    array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
    dom_base_uri(dom) as `baseUri`
from
    load_and_select('https://www.amazon.com/dp/B00BTX5926', ':root')
```

Execute the X-SQL:

```kotlin
val context = SQLContexts.create()
context.executeQuery(sql)
```

The result is as follows:

    TITLE                                                            | LISTPRICE | PRICE  | CATEGORIES                                    | BASEURI
    Tara Toys Ariel Necklace Activity Set - Amazon Exclusive (51394) | $19.99    | $12.99 | Toys & Games|Arts & Crafts|Craft Kits|Jewelry | https://www.amazon.com/dp/B00BTX5926

# Use pulsar as a server

## Requirements

- Memory 4G+
- Maven 3.2+
- The latest version of the Java 11 OpenJDK
- java and jar on the PATH
- Google Chrome 90+

## Build from source

    git clone https://github.com/platonai/pulsar.git
    cd pulsar && bin/build.sh

## Start the pulsar server if not started

```shell
bin/pulsar
```

## Scrape a webpage in another terminal window

```shell
bin/scrape.sh
```

The response is as follows:

```json
{
    "uuid": "cc611841-1f2b-4b6b-bcdd-ce822d97a2ad",
    "statusCode": 200,
    "pageStatusCode": 200,
    "pageContentBytes": 1607636,
    "resultSet": [
        {
            "title": "Tara Toys Ariel Necklace Activity Set - Amazon Exclusive (51394)",
            "listprice": "$19.99",
            "price": "$12.99",
            "categories": "Toys & Games|Arts & Crafts|Craft Kits|Jewelry",
            "baseuri": "https://www.amazon.com/dp/B00BTX5926"
        }
    ],
    "pageStatus": "OK",
    "status": "OK"
}
```

Here are X-SQLs for [The Complete Amazon Data Model](pulsar-app/pulsar-sites-support/pulsar-site-amazon/src/main/resources/config/sites/amazon/crawl/parse/sql).
