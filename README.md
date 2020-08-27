Pulsar README
===================
Pulsar focus on web data processing, it extends SQL to handle the entire life cycle of web data processing:
crawling, web scraping, data mining, BI, etc.

## Other lauguage
[Chinese](README.zh.md)

![product-screenshot](docs/images/pulsar-product-screenshot-1.png)
![product-screenshot](docs/images/pulsar-product-screenshot-2.png)

# Features
- X-SQL: eXtended SQL to manage web data: crawling, web scraping, data mining, BI, etc.
- Web spider: browser rendering, ajax, scheduling, page scoring, monitoring, distributed, high performance, indexing by solr/elastic
- BI Integration: turn Web sites into tables and charts using just one simple SQL
- Big data: large scale, various storage: HBase/MongoDB

For more information check out [platon.ai](https://platon.ai)

## X-SQL
Crawl and scrape a single page:

    select
        dom_text(dom) as title,
        dom_abs_href(dom) as link
    from
        load_and_select('https://en.wikipedia.org/wiki/topology', '.references a.external');

The SQL above downloads a Web page from wikipedia, find out the references section and extract all external reference links.

Crawl out pages from a portal and scrape each:

    select
        dom_first_text(dom, '.sku-name') as name,
        dom_first_number(dom, '.p-price .price', 0.00) as price,
        dom_first_number(dom, '#page_opprice', 0.00) as tag_price,
        dom_first_text(dom, '#comment-count .count') as comments,
        dom_first_text(dom, '#summary-service') as logistics,
        dom_base_uri(dom) as baseuri
    from load_out_pages('https://list.jd.com/list.html?cat=652,12345,12349 -i 1s -ii 100d', 'a[href~=item]', 1, 100)
    where dom_first_number(dom, '.p-price .price', 0.00) > 0
    order by dom_first_number(dom, '.p-price .price', 0.00);

The SQL above visits a portal page in jd.com, downloads detail pages and then scrape data from them.

You can clone a copy of Pulsar code and run the SQLs yourself, or run them from our [online demo](http://bi.platonic.fun/).

Check [sql-history.sql](sql-history.sql) to see more example SQLs. All SQL functions can be found under [ai.platon.pulsar.ql.h2.udfs](pulsar-ql-server/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs).

## Use pulsar as a library
See [Tutorials](https://github.com/platonai/pulsar-tutorials).

## Use pulsar as an X-SQL server
Once pulsar runs in X-SQL server mode, the web can be used just like the normal database.
You can use our customized [Metabase](https://github.com/platonai/metabase) to write X-SQLs and turn web sites into tables and 
charts immediately. Everyone in your company can ask questions and learn from WEB DATA now, for the first time.

# Build & Run
## Check & install dependencies

    bin/tools/install-depends.sh

## Install mongodb
MongoDB is optional but is recommended. You can skip this step, in such case, all data will lose after pulsar shutdown.
Ubuntu/Debian:

    sudo apt install mongodb

## Build from source

    git clone https://github.com/platonai/pulsar.git
    cd pulsar && mvn

## Run the native api demo

    bin/pulsar example ManualKt

## Start pulsar server

    bin/pulsar

## Use web console
Open web console [http://localhost:8082](http://localhost:8082) using your favourite browser now, enjoy playing with X-SQL.

## Use Metabase
[Metabase](https://github.com/platonai/metabase) is the easy, open source way for everyone in your company to ask questions and learn from data.
With X-SQL support, everyone can organize knowledge not just from the company's internal data, but also
from the web.

    git clone https://github.com/platonai/pulsar-metabase.git
    cd pulsar-metabase
    bin/build && bin/start

# Enterprise Edition:

Pulsar Enterprise Edition supports Auto Web Mining: advanced machine learning, no rules or training required, 
turn web sites into tables automatically. Here are some examples: [Auto Web Mining Examples](http://bi.platonic.fun/)
