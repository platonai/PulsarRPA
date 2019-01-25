Pulsar README
===================
Pulsar is a full featured Web crawler as well as a Web mining framework.

# Features
- Web SQL: Do all Web mining jobs using SQL
- BI Integration: Turn Web sites into tables and charts using just one simple SQL
- Ajax support: Access the Web automatically, behave like humans
- Web site monitoring: monitor news sites, e-commerce sites out-of-box
- Highly extensible and scalable: runs on Hadoop/Spark, and other big data infrastructure
- Various database support: Store data in your favourite database, MongoDB/HBase, etc

## Web SQL
Turn a Web page into a table:

    SELECT
        DOM_TEXT(DOM) AS TITLE,
        DOM_ABS_HREF(DOM) AS LINK
    FROM
        LOAD_AND_SELECT('https://en.wikipedia.org/wiki/Topology', '.references a.external');

The SQL above downloads a Web page from wikipedia, find out the references section and extract all external reference links.

Check [sql-history.sql](https://github.com/platonai/pulsar/blob/master/sql-history.sql) to see more example SQLs. All SQL functions can be found under [fun.platonic.pulsar.ql.h2.udfs](https://github.com/platonai/pulsar/tree/master/pulsar-ql-server/src/main/kotlin/fun/platonic/pulsar/ql/h2/udfs).

## BI Integration
Use the exiting customized BI tool [Metabase](https://github.com/platonai/metabase) to write Web SQLs and turn 
Web sites into tables and charts immediately.
Everyone in your company can ask questions and learn from WEB DATA now, for the first time.

# Build & Run
## Build from source
    git clone git@github.com:platonai/pulsar.git
    cd pulsar && mvn -Pthird -Pplugins
## Install dependencies
    bin/tools/install-depends.sh
## Install mongodb
You can skip this step, in such case, all data will lose after pulsar shutdown.
Ubuntu/Debian:

    sudo apt-get install mongodb
## Start the pulsar server
    bin/pulsar
## Use Web console
Web console [http://localhost:8082](http://localhost:8082) is already open in your browser now, enjoy playing with Web SQL.
## Execute a single Web SQL
    bin/pulsar sql -sql "SELECT DOM_TEXT(DOM) AS TITLE, DOM_ABS_HREF(DOM) AS LINK FROM LOAD_AND_SELECT('https://en.wikipedia.org/wiki/Topology', '.references a.external')"
## Use GUI-free console
    bin/pulsar sql

## Use advanced BI tool
Download [Metabase](https://github.com/platonai/metabase) Web SQL edition, and run:

    -- coming soon ..
    java -jar metabase.jar

# Large scale Web spider
Crawl the open Web from seeds, and index text content using solr, run script:

    -- coming soon ..
    bin/crawl.sh default false awesome_crawl_task http://master:8983/solr/awesome_crawl_task/ 1

# Enterprise Edition:

Pulsar Enterprise Edition comes with lots of exciting features:

Advanced AI to do Web content mining:
```
1. Extract large scale Web pages with above human-level accuracy using advanced AI
2. Learn and generate SQLs for sites
```

Full featured Web SQL:
```
1. Any source, any format, any volume, ETL the data and turn it into a table by just one simple SQL
2. Monitor a Web site and turn it into a table by just one simple SQL
3. Integrated argorithms for Web extraction, data mining, NLP, Knowldge Graph, maching learning, etc
4. Do business intelligence on unstructured data
```

Enterprise Edition will be open sourced step by step.

Coming soon ...

============

# Cloud Edition:

Write your own Web SQLs to create data products anywhere, anytime, to share, or for sale

Coming soon ...
