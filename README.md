Pulsar README
===================
Turn Web sites into tables and charts using just one simple SQL.

[中文文档](README.zh.md)

![product-screenshot](docs/images/pulsar-product-screenshot-1.png)
![product-screenshot](docs/images/pulsar-product-screenshot-2.png)

# Features
- X-SQL: eXtended SQL to do all data jobs: collection, extraction, preparation, processing, storage, BI, etc
- Web spider: browser rendering, Ajax, scheduling, page scoring, monitoring, distributed, high performance, indexing by solr/elastic
- BI Integration: turn Web sites into tables and charts using just one simple SQL
- Big data: large scale, various storage: HBase/MongoDB, etc

For more information check out [platonic.fun](http://platonic.fun)

## X-SQL
Extract data from a single page:

    SELECT
        DOM_TEXT(DOM) AS TITLE,
        DOM_ABS_HREF(DOM) AS LINK
    FROM
        LOAD_AND_SELECT('https://en.wikipedia.org/wiki/Topology', '.references a.external');

The SQL above downloads a Web page from wikipedia, find out the references section and extract all external reference links.

Extract data from a batch of pages, and turn them into a table:

    SELECT
      DOM_BASE_URI(DOM) AS BaseUri,
      DOM_FIRST_TEXT(DOM, '.brand') AS Title,
      DOM_FIRST_TEXT(DOM, '.titlecon') AS Memo,
      DOM_FIRST_TEXT(DOM, '.pbox_price') AS Price,
      DOM_FIRST_TEXT(DOM, '#wrap_con') AS Parameters
    FROM
      LOAD_OUT_PAGES_IGNORE_URL_QUERY('https://www.mia.com/formulas.html', '*:expr(width>=250 && width<=260 && height>=360 && height<=370 && sibling>30 ) a', 1, 20);

The SQL above visits an index page in mia.com, download detail pages and then extract data from them.

You can clone a copy of Pulsar code and run the SQLs yourself, or run them from our [online demo](http://bi.platonic.fun/question/65).

Check [sql-history.sql](https://github.com/platonai/pulsar/blob/master/sql-history.sql) to see more example SQLs.
All X-SQL functions can be found under [ai.platon.pulsar.ql.h2.udfs](https://github.com/platonai/pulsar/tree/master/pulsar-ql-server/src/main/kotlin/fun/platonic/pulsar/ql/h2/udfs),
Or run the x-sql below to show all X-SQL functions:

    SELECT * FROM XSQL_HELP();

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
    cd pulsar && mvn -Pthird -Pplugins
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
