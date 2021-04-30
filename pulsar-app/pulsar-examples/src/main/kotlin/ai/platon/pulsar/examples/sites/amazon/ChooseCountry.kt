package ai.platon.pulsar.examples.sites.amazon

import ai.platon.pulsar.amazon.environment.ChooseCountry
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.context.withContext
import org.apache.commons.lang3.SystemUtils
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var portalUrl = "https://www.amazon.com/"
    var loadArguments = ""
    var gui = SystemUtils.IS_OS_WINDOWS

    var i = 0
    while (i++ < args.size - 1) {
        if (args[i] == "-url") portalUrl = args[i++]
        if (args[i] == "-args") loadArguments = args[i++]
        if (args[i] == "-gui") gui = true
    }

    withContext { cx ->
        val unmodifiedConfig = (cx as AbstractPulsarContext).unmodifiedConfig.unbox()

        if (gui) {
            System.clearProperty(CapabilityTypes.BROWSER_LAUNCH_SUPERVISOR_PROCESS)
            unmodifiedConfig.unset(CapabilityTypes.BROWSER_LAUNCH_SUPERVISOR_PROCESS)
            System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
        }
        System.setProperty(CapabilityTypes.PROXY_USE_PROXY, "false")
        System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyContextIdGenerator")

        ChooseCountry(portalUrl, loadArguments, cx.createSession()).choose()

        println("All done.")

        exitProcess(0)
    }
}
