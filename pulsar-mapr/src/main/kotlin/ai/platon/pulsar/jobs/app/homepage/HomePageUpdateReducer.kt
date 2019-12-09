package ai.platon.pulsar.jobs.app.homepage

import ai.platon.pulsar.common.CommonCounter
import ai.platon.pulsar.common.WeakPageIndexer
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer
import ai.platon.pulsar.jobs.core.Reducer
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.generated.GWebPage
import java.io.IOException
import java.util.*

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class HomePageUpdateReducer : AppContextAwareGoraReducer<SelectorEntry, GWebPage, String, GWebPage>() {
    private var webDb: WebDb? = null
    private var count = 0
    private val pageSize = 10000
    private var pageNo = 0
    private val indexUrls: MutableList<CharSequence> = ArrayList(pageSize)
    private lateinit var indexHomeUrl: String
    private lateinit var weakIndexer: WeakPageIndexer

    @Throws(IOException::class)
    override fun setup(context: Context) {
        webDb = applicationContext.getBean(WebDb::class.java)
        indexHomeUrl = jobConf[CapabilityTypes.STAT_INDEX_HOME_URL, "http://nebula.platonic.fun/tmp_index/"]
        weakIndexer = WeakPageIndexer(indexHomeUrl, webDb)
    }

    protected override fun reduce(key: SelectorEntry, rows: Iterable<GWebPage>, context: Context) {
        metricsCounters.increase(CommonCounter.rRows)
        ++count
        indexUrls.add(key.url)
        if (indexUrls.size >= pageSize) {
            commit()
        }
    }

    override fun cleanup(context: Context) {
        commit()
        val message = "Total $count index pages, indexed in $pageNo pages"
        LOG.info(message)
    }

    private fun commit() {
        weakIndexer.indexAll(++pageNo, indexUrls)
        weakIndexer.commit()
        indexUrls.clear()
    }
}