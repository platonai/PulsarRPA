package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.Wildchar
import com.github.kklisura.cdt.protocol.types.network.ResourceType

/**
 * The block rules of urls and resources
 * */
open class BlockRule {

    open val blockingResourceTypes: MutableList<ResourceType>
        get() = listOf(ResourceType.IMAGE, ResourceType.MEDIA, ResourceType.FONT).toMutableList()

    /**
     * amazon.com note:
     * The following have to pass, or the site refuses to serve:
     * .woff,
     * .mp4
     * */
    open val mustPassUrls: MutableList<String>
        get() = mutableListOf()

    /**
     * Blocking urls patten using widcards
     * */
    open val blockingUrls: MutableList<String>
        get() = listOf(
            "*.png", "*.jpg", "*.jpeg", "*.gif", "*.ico", "*.webp",
            "*.woff", "*.woff2",
            "*.mp4", "*.svg",
            "*.png?*", "*.jpg?*", "*.gif?*", "*.ico?*", "*.webp?*",
            "https://img*"
        ).filterNot { it in mustPassUrls }.toMutableList()

    open val mustPassUrlPatterns: MutableList<Regex>
        get() = listOf(
            "about:blank",
            "data:.+",
        ).map { it.toRegex() }.union(mustPassUrls.map { Wildchar(it).toRegex() }).toMutableList()

    open val blockingUrlPatterns: MutableList<Regex>
        get() = blockingUrls.map { Wildchar(it).toRegex() }.toMutableList()
}
