Pulsar README
===================
** Network As A Database **

Turn the Web into tables and charts using simple SQLs.

## Other language
[Chinese](README.zh.md)

![product-screenshot](docs/images/pulsar-product-screenshot-1.png)

# Features
- X-SQL: extend SQL to manage web data: Web crawling, scraping, Web content mining, BI on Web.
- Web spider: browser rendering, ajax, scheduling, page scoring, monitoring, distributed, high performance, indexing by solr/elastic
- BI Integration: turn large websites into tables and charts using just one simple SQL
- Big data: large scale, various storage: HBase/MongoDB

For more information check out [platon.ai](http://platon.ai)

## X-SQL

Scrape a product page:

    select
        dom_first_text(dom, '#productTitle') as `title`,
        dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
        dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
        array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
        dom_base_uri(dom) as `baseUri`
    from
        load_and_select('https://www.amazon.com/dp/B00BTX5926', ':root')

Scrape pages from a portal:

    select
        dom_first_text(dom, '#productTitle') as `title`,
        dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
        dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
        array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
        dom_base_uri(dom) as `baseUri`
    from
        load_out_pages('https://www.amazon.com/Best-Sellers/zgbs', 'a[href~=/dp/]')

# Build & Run
## Check & install dependencies

    bin/tools/install-depends.sh

## Build from source

    git clone https://github.com/platonai/pulsar.git
    cd pulsar && mvn

## Start pulsar server

    bin/pulsar

or if you are using an IDE, run main() in

    ai.platon.pulsar.common.master.PulsarMaster

## Issue a request to scrape

    # CURL
    curl -X POST --location "http://localhost:8182/x/e" \
    -H "Content-Type: text/plain" \
    -d "select
            dom_first_text(dom, '#productTitle') as `title`,
            dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
            dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
            array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
            dom_base_uri(dom) as `baseUri`
        from
            load_and_select('https://www.amazon.com/dp/B00BTX5926', ':root')"
