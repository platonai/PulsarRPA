Pulsar README
===================
Pulsar is a full featured Web crawler as well as a data mining framework.

Pulsar has a Web SQL engine with which all tasks can be done using simple SQLs.

For example, turn a Web page into a table:

    SELECT
        DOM_TEXT(DOM) AS TITLE,
        DOM_ABS_HREF(DOM) AS LINK
    FROM 
    DOMT_LOAD_AND_SELECT('https://en.wikipedia.org/wiki/Topology', '.references a.external');

The SQL above downloads Web page from wikipedia, find out the references section and extract all external reference links.

To run the above SQL, download Pulsar, run

    bin/pulsar server

And then you can see a popup browser window, enter password 'sa', enter the SQL above and then run it.

All SQL functions can be found under fun/platonic/pulsar/ql/h2/udfs, every UDF have a namespace and a name.

TODO: document all SQL functions

Pulsar is also a production ready Web crawler, you can crawl large web sites from seeds, using Nutch style.

Pulsar is highly modularized so it also can be used as a library and be embedded within other projects.

Pulsar supports selenium so you can do web scraping using selenium's native api.

For Web scraping(in kotlin):

    fun main(args: Array<String>) {
        val pulsar = Pulsar()
        val page = pulsar.load("http://list.mogujie.com/book/jiadian/10059513")
        println(WebPageFormatter(page).withLinks())

        val document = pulsar.parse(page)
        val title = document.selectFirst(".goods_item .title")
        println(title)
    }

For batch Web scraping(in kotlin):

    val url = "http://www.sh.chinanews.com/jinrong/index.shtml"
    val portal = pulsar.load("$url --parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome")
    val pages = pulsar.parallelLoadAll(portal.simpleLiveLinks.filter { it.contains("jinrong") }, LoadOptions.parse("--parse"))
    pages.forEach { println("${it.url} ${it.contentTitle}") }

The examples can be found in fun/platonic/pulsar/examples/WebAccess.kt, and more examples will be added soon.

To build from source:

    git clone git@github.com:platonai/pulsar.git
    cd pulsar && mvn

Pulsar supports Nutch style large-scalar crawler in batches, for example,
to crawl Web pages and index using solr, run script:

    bin/crawl.sh default false information_tmp http://master:8983/solr/information_tmp/ 1

TODO: Scripts is NOT working if you can see this line. We are working on it ...

For more crawl task examples, see bin/samples.

The crawl workflow can be illustrated in kotlin as the following:

    val pages = urls
            .map { generate(batchId, it) }
            .filter { it.batchId == batchId }
            .filter { it.marks.contains(Mark.GENERATE) }
            .map { fetchComponent.fetchContent(it) }
            .filter { it.batchId == batchId }
            .filter { it.marks.contains(Mark.FETCH) }
            .onEach { parseComponent.parse(it) }
            .filter { it.batchId == batchId }
            .filter { it.marks.contains(Mark.PARSE) }
            .onEach { indexComponent.dryRunIndex(it) }
            .map { WebVertex(it) }
            .map { WebGraph(it, it) }
            .reduce { g, g2 ->  g.combine(g2)}
            .vertexSet()
            .map { it.webPage }
            .map { updateOutGraphMapper(it) }
            .map { updateOutGraphReducerBuildGraph(it) }
            .map { updateOutGraphReducer(it) }
            .filter { it.batchId == batchId }
            .filter { it.marks.contains(UPDATEOUTG) }
            .map { updateInGraphMapper(it) }
            .map { updateInGraphReducer(it) }

Note: the Web graph is updated both inward and outward, score filters can be applied to decide the importance of Web pages. 
Score filters may differ for different crawl tasks.

HBase is the primary choice as the storage, and any storage supported by Apache Gora will be fine.

TODO:

```
1. Add documents
2. Android support: http://selendroid.io/
3. Correct errors in startup scripts
```

============

Enterprise Edition:

Pulsar Enterprise Edition comes with lots of exciting features:

AI to do Web content mining:
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

Advanced DOM processing:
```
1. Tranditional CSS path support
2. Advanced expression in CSS path: element.select("div:expr(width >= 300 and height >= 400)")
3. Statistics based element locate
```

Enterprise Edition will be open sourced step by step.

Coming soon ...

============

Cloud Edition:

Write your own Web SQLs to create data products anywhere, anytime, to share, or for sale

Coming soon ...
