package ai.platon.pulsar.jobs.app.inject

import ai.platon.pulsar.common.CommonCounter
import ai.platon.pulsar.common.Urls.splitUrlArgs
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.jobs.core.AppContextAwareMapper
import ai.platon.pulsar.jobs.core.Mapper
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text

/**
 * Created by vincent on 17-4-13.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class InjectMapper : AppContextAwareMapper<LongWritable, Text, String, GWebPage>() {
    private lateinit var injectComponent: InjectComponent

    override fun setup(context: Context) {
        injectComponent = applicationContext.getBean(InjectComponent::class.java)
        Params.of("className", this.javaClass.simpleName)
                .merge(injectComponent.params).withLogger(Mapper.LOG).info()
    }

    override fun map(key: LongWritable, line: Text, context: Context) {
        metricsCounters.increase(CommonCounter.mRows)
        val configuredUrl = StringUtils.stripToEmpty(line.toString())
        if (configuredUrl.isEmpty() || configuredUrl.startsWith("#")) {
            return
        }
        injectComponent.inject(splitUrlArgs(configuredUrl))
        metricsCounters.increase(CommonCounter.mPersist)
    }
}
