package ai.platon.pulsar.jobs.app.parse

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.PageParser.Companion.isTruncated
import ai.platon.pulsar.jobs.core.GoraMapper
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.apache.avro.util.Utf8
import java.io.IOException

class ParserMapper : GoraMapper<String, GWebPage, String, GWebPage>() {
    private var pageParser: PageParser? = null
    private var resume = false
    private var force = false
    private var reparse = false
    private var batchId: Utf8? = null
    private var limit = -1
    private var skipTruncated = false
    private var count = 0

    public override fun setup(context: Context) {
        batchId = Utf8(jobConf[CapabilityTypes.BATCH_ID, AppConstants.ALL_BATCHES])
        pageParser = PageParser(jobConf)
        resume = jobConf.getBoolean(CapabilityTypes.RESUME, false)
        reparse = jobConf.getBoolean(CapabilityTypes.PARSE_REPARSE, false)
        force = jobConf.getBoolean(CapabilityTypes.FORCE, false)
        limit = jobConf.getInt(CapabilityTypes.LIMIT, -1)
        skipTruncated = jobConf.getBoolean(CapabilityTypes.PARSE_SKIP_TRUNCATED, true)
        LOG.info(Params.format(
                "batchId", batchId,
                "resume", resume,
                "reparse", reparse,
                "force", force,
                "limit", limit,
                "skipTruncated", skipTruncated
        ))
    }

    @Throws(IOException::class, InterruptedException::class)
    override fun map(reversedUrl: String, row: GWebPage, context: Context) {
        val page = WebPage.box(reversedUrl, row, true)
        val url = page.url
        if (limit > -1 && count > limit) {
            stop("hit limit $limit, finish mapper.")
            return
        }
        if (!shouldProcess(page)) {
            return
        }
        val parseResult = pageParser!!.parse(page)
        context.write(reversedUrl, page.unbox())
        ++count
    }

    private fun shouldProcess(page: WebPage): Boolean {
        if (!reparse && !page.hasMark(Mark.FETCH)) {
            metricsCounters.increase(PageParser.Counter.notFetched)
            if (LOG.isDebugEnabled) { //        log.debug("Skipping " + TableUtil.unreverseUrl(key) + "; not fetched yet");
            }
            return false
        }
        if (!reparse && resume && page.hasMark(Mark.PARSE)) {
            metricsCounters.increase(PageParser.Counter.alreadyParsed)
            if (!force) {
                LOG.debug("Skipping " + page.url + "; already parsed")
                return false
            }
            LOG.debug("Forced parsing " + page.url + "; already parsed")
        } // if resume
        if (skipTruncated && isTruncated(page)) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Page truncated, ignore")
            }
            metricsCounters.increase(PageParser.Counter.truncated)
            return false
        }
        return true
    }

    companion object {
        val LOG = ParserJob.LOG
    }
}
