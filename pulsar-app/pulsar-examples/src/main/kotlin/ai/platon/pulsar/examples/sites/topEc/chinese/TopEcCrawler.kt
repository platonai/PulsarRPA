package ai.platon.pulsar.examples.sites.topEc.chinese

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val urls = """
            http://category.dangdang.com/cid4010209.html -ol a[href~=product]
            https://list.gome.com.cn/cat10000092.html -ol a[href~=item]
            https://list.jd.com/list.html?cat=652,12345,12349 -ol a[href~=item]
            https://list.tmall.com/search_product.htm?q=大家电 -ol a[href~=item]
            https://search.suning.com/微单/&zw=0?safp=d488778a.shuma.44811515285.1 -ol a[href~=detail]
            https://s.taobao.com/search?spm=a21bo.jianhua.201867-main.24.5af911d9wFOWsc&q=收纳 -ol a[href~=item]
    """.trimIndent()
    val args = "-i 1s -ii 5d -parse -ignoreFailure"

    val session = PulsarContexts.createSession()
    val options = session.options(args)

    options.eventHandler.loadEventHandler.onAfterHtmlParse.addLast { page, document ->
        println(document.title + " | " + document.baseUri)
    }
    urls.split("\n").forEach { session.submitLoadOutPages(it, options) }

    PulsarContexts.await()
}
