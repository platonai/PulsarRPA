package ai.platon.pulsar.protocol.browser.conf

import ai.platon.pulsar.common.Wildchar
import com.github.kklisura.cdt.protocol.types.network.ResourceType

object UrlBlockRule {
    /**
     * amazon.com note:
     * The following have to pass, or the site refuses to serve:
     * .woff,
     * .mp4
     * */
    val mustPassUrls = listOf("*.woff", "*.mp4").toMutableList()

    /**
     * Blocking urls patten using widcards
     * */
    val blockingUrls = listOf(
        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.ico", "*.webp",
        "*.woff", "*.woff2",
        "*.mp4", "*.svg",
        "*.png?*", "*.jpg?*", "*.gif?*", "*.ico?*", "*.webp?*",
        "https://img*"
    ).filterNot { it in mustPassUrls }.toMutableList()

    val mustPassUrlPatterns = listOf(
        "about:blank",
        "data:.+",
        ".+/gp/.+",
        ".+/dp/.+",
        "https://fls-na.amazon.com/1/batch/1/OP/ATVPDKIKX0DER.+",
        "https://fls-na.amazon.com/1/remote-weblab-triggers.+",
        "https://fls-na.amazon.com/1/action-impressions.+",
        "https://aax-us-east.amazon-adsystem.com.+",
        "https://assoc-na.associates-amazon.com.+"
    ).map { it.toRegex() }.union(mustPassUrls.map { Wildchar(it).toRegex() }).toMutableList()

    val blockingUrlPatterns = blockingUrls.map { Wildchar(it).toRegex() }.toMutableList()

    val blockingResourceTypes = listOf(ResourceType.IMAGE, ResourceType.MEDIA, ResourceType.FONT).toMutableList()
}
