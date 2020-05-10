package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.options.InjectOptions
import ai.platon.pulsar.crawl.inject.SeedBuilder
import ai.platon.pulsar.persist.WebDb
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val context: ApplicationContext = ClassPathXmlApplicationContext(
            System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.APP_CONTEXT_CONFIG_LOCATION))
    val conf = context.getBean(MutableConfig::class.java)

    val opts = InjectOptions(args, conf)
    opts.parseOrExit()
    conf.setIfNotEmpty(CapabilityTypes.STORAGE_CRAWL_ID, opts.crawlId)
    var seeds = opts.seeds[0]
    if (seeds.startsWith("@")) {
        seeds = String(Files.readAllBytes(Paths.get(seeds.substring(1))))
    }
    val configuredUrls = Strings.getUnslashedLines(seeds)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .sorted().distinct()
    context.getBean(WebDb::class.java).use { webDb ->
        val injectComponent = InjectComponent(SeedBuilder(conf), webDb, conf)
        injectComponent.injectAll(*configuredUrls.toTypedArray())
        injectComponent.commit()
    }
}
