Pulsar README
===================

Pulsar is a production ready web crawler.

Pulsar is highly modularized so it also can be used as a library or embedded within other projects.

The major architecture of Pulsar comes from Apache Nutch and reused a lot of code from Nutch.

Pulsar supports selenium so one can do web scraping using selenium's native api or custom parse plugins.

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

For web crawling and index using solr, run script:

    bin/crawl.sh default false information_tmp http://master:8983/solr/information_tmp/ 1

For more crawl task examples, see bin/samples.

TODO: Scripts may not working if you can see this line. We are working on it ...

The crawling workflow can be illustrated in kotlin as the following:

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

Notice: the Web graph is updated both inward and outward, score filters can be applied to decide the importance of Web pages. Score filters may differs for different crawl tasks.

HBase is the primary choice as the storage, and any storage supported by Apache Gora will be fine.

Plugins are configured as spring beans.

TODO:

```
1. Integrate with srping 5: https://spring.io/
2. Test scripts and correct errors
3. A simple learner to learn boilerpipe arguments: https://github.com/kohlschutter/boilerpipe
```

============

Enterprise Edition:

Pulsar Enterprise Edition comes with lots of exciting features:

Web SQL:
```
1. Any source, any format, any volume, ETL the data and turn it into a table by just one simple SQL
2. Monitor a Web site and turn it into a table by just one simple SQL
3. Integrated argorithms for Web extraction, data mining, NLP, Knowldge Graph, maching learning, etc
4. Do business intelligence on unstructured data
```

Machine learning for Web content mining:
```
1. Learn and generate SQL for one site
2. Extract Web pages from many web sites using just one model
```

Advanced DOM processing:
```
1. Tranditional CSS path support
2. Advanced expression in CSS path: element.select("div:expr(_width >= 300 and _height >= 400)")
3. Statistics based element locate
```

Coming soon ...

Enterprise Edition will be open sourced step by step.

============

Cloud Edition:

Write your own Web SQL to create data products anywhere, anytime, for sale

Coming soon ...
