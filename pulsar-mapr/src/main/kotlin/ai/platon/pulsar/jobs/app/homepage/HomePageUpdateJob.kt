package ai.platon.pulsar.jobs.app.homepage

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.options.CommonOptions
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.apache.gora.filter.FilterOp
import org.apache.gora.filter.MapFieldValueFilter
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class HomePageUpdateJob : AppContextAwareJob() {
    companion object {
        val LOG = LoggerFactory.getLogger(HomePageUpdateJob::class.java)
        val FIELDS: MutableSet<GWebPage.Field> = HashSet()

        init {
            Collections.addAll(FIELDS, *GWebPage.Field.values())
            FIELDS.remove(GWebPage.Field.CONTENT)
            FIELDS.remove(GWebPage.Field.HEADERS)
            FIELDS.remove(GWebPage.Field.PAGE_TEXT)
            FIELDS.remove(GWebPage.Field.CONTENT_TEXT)
            FIELDS.remove(GWebPage.Field.LINKS)
            FIELDS.remove(GWebPage.Field.INLINKS)
            FIELDS.remove(GWebPage.Field.PAGE_MODEL)
        }
    }

    @Throws(Exception::class)
    override fun setup(params: Params) {
        super.setup(params)
        setIndexHomeUrl()
    }

    abstract fun setIndexHomeUrl()

    val queryFilter: MapFieldValueFilter<String, GWebPage>
        get() {
            val filter = MapFieldValueFilter<String, GWebPage>()
            filter.fieldName = GWebPage.Field.MARKERS.toString()
            filter.filterOp = FilterOp.NOT_EQUALS
            filter.isFilterIfMissing = false
            filter.mapKey = WebPage.wrapKey(Mark.INACTIVE)
            filter.operands.add(WebPage.u8(PulsarConstants.YES_STRING))
            return filter
        }

    override fun run(args: Array<String>): Int {
        val options = CommonOptions(args)
        options.parseOrExit()
        jobConf.setIfNotEmpty(CapabilityTypes.STORAGE_CRAWL_ID, options.crawlId)

        run()

        return 0
    }
}
