Pulsar README
===================
Pulsar focus on web data processing, it extends SQL to handle the entire life cycle of web data processing:
crawling, web scraping, data mining, BI, etc.

[中文文档](README.zh.md)

![product-screenshot](docs/images/pulsar-product-screenshot-1.png)
![product-screenshot](docs/images/pulsar-product-screenshot-2.png)

# Features
- X-SQL: eXtended SQL to manage web data: crawling, web scraping, data mining, BI, etc.
- Web spider: browser rendering, ajax, scheduling, page scoring, monitoring, distributed, high performance, indexing by solr/elastic
- BI Integration: turn Web sites into tables and charts using just one simple SQL
- Big data: large scale, various storage: HBase/MongoDB

For more information check out [platonic.fun](http://platonic.fun)

## Native API
Create a pulsar session:

    val session = PulsarContext.createSession()
    # specify a test url
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

Crawl a single page:

    session.load(url, "-expires 1d")

Crawl out pages from a portal:

    session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")

Crawl and scrape a single page:

        val page = session.load(url, "-expires 1d")
        val document = session.parse(page)
        val products = document.select("li[data-sku]").map {
            it.selectFirstOrNull(".p-name em")?.text() to it.selectFirstOrNull(".p-price")?.text()
        }
        products.forEach { (name, price) -> println("$price $name") }

Crawl out pages from a portal and scrape each out page:

        val pages = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
        val documents = pages.map { session.parse(it) }
        val products = documents.mapNotNull { it.selectFirstOrNull(".product-intro") }.map {
            it.selectFirstOrNull(".sku-name")?.text() to it.selectFirstOrNull(".p-price")?.text()
        }
        products.forEach { (name, price) -> println("$price $name") }

The above examples can be found in [manual](pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/Manual.kt)

## X-SQL
Crawl and scrape data from a single page:

    SELECT
        DOM_TEXT(DOM) AS TITLE,
        DOM_ABS_HREF(DOM) AS LINK
    FROM
        LOAD_AND_SELECT('https://en.wikipedia.org/wiki/Topology', '.references a.external');

The SQL above downloads a Web page from wikipedia, find out the references section and extract all external reference links.

Crawl and scrape data from a batch of pages, and turn them into a table:

    SELECT
      DOM_FIRST_TEXT(DOM, '.sku-name') AS NAME,
      DOM_FIRST_TEXT(DOM, '.summary-price') AS PRICE,
      DOM_BASE_URI(DOM) AS URI,
      DOM_FIRST_IMG(DOM, '.main-img') AS MAIN_IMAGE,
      DOM_FIRST_IMG(DOM, '460x460') AS MAIN_IMAGE2,
      DOM_FIRST_TEXT(DOM, '.parameter2') AS PARAMETERS,
      DOM_FIRST_TEXT(DOM, '.comment-item') AS COMMENT1
    FROM LOAD_OUT_PAGES('https://list.jd.com/list.html?cat=652,12345,12349', 'a[href~=item]', 1, 20)
    WHERE LOCATE('item', DOM_BASE_URI(DOM)) > 0;

The SQL above visits a portal page in jd.com, download detail pages and then extract data from them.

You can clone a copy of Pulsar code and run the SQLs yourself, or run them from our [online demo](http://bi.platonic.fun/question/65).

Check [sql-history.sql](sql-history.sql) to see more example SQLs. All SQL functions can be found under [ai.platon.pulsar.ql.h2.udfs](pulsar-ql-server/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs).

## BI Integration
Use the customized [Metabase](https://github.com/platonai/metabase) to write X-SQLs and turn 
Web sites into tables and charts immediately.
Everyone in your company can ask questions and learn from WEB DATA now, for the first time.

# Build & Run
## Install dependencies
    
    bin/tools/install-depends.sh
    
## Install mongodb
You can skip this step, in such case, all data will lose after pulsar shutdown.
Ubuntu/Debian:

    sudo apt-get install mongodb
    
## Build from source
    
    git clone https://github.com/platonai/pulsar.git
    cd pulsar && mvn
    
## Start pulsar server
    
    bin/pulsar
    
## Execute X-SQLs
Web console [http://localhost:8082](http://localhost:8082) is already open in your browser now, enjoy playing with X-SQL.

## Use Metabase
[Metabase](https://github.com/platonai/metabase) is the easy, open source way for everyone in your company to ask questions and learn from data.
With X-SQL support, everyone can organize knowledge not just from the company's internal data, but also
from the WWW.

    git clone https://github.com/platonai/metabase.git
    cd metabase
    bin/build && bin/start

# Enterprise Edition:

Pulsar Enterprise Edition supports Auto Web Mining: unsupervised machine learning, no rules or training required, 
turn Web sites into tables automatically. Here are some examples: [Auto Web Mining Examples](http://bi.platonic.fun/dashboard/20)
