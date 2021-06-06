package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.metrics.AppMetrics

class AmazonMetrics(val ident: String) {
    companion object {
        val tasks = AmazonDiagnosis("tasks")
        val finishes = AmazonDiagnosis("finishes")
        val successes = AmazonDiagnosis("successes")
        val fetchSuccesses = AmazonDiagnosis("fetchSuccesses")
    }

    private val registry = AppMetrics.defaultMetricRegistry
    private fun multiMetric(name: String) = registry.multiMetric(this, ident, name)

    val zgbs = multiMetric("zgbs")
    val szgbs = multiMetric("szgbs")
    val mWishedF = multiMetric("mWishedF")
    val smWishedF = multiMetric("smWishedF")
    val nRelease = multiMetric("nRelease")
    val snRelease = multiMetric("snRelease")
    val mas = multiMetric("mas")
    val smas = multiMetric("smas")
}

class AmazonDiagnosis(val ident: String) {
    companion object {
        /**
         * The labeled portals:
         * https://www.amazon.com/Best-Sellers/zgbs
         * https://www.amazon.com/gp/new-releases
         * https://www.amazon.com/gp/movers-and-shakers
         * https://www.amazon.com/gp/most-wished-for
         * */
        val portalLabels = arrayOf(
            "zgbs", "most-wished-for", "new-releases", "movers-and-shakers",
        )



        fun isAmazon(url: String): Boolean {
            return url.contains(".amazon.")
        }

        fun getLabelOfPortalOrNull(url: String): String? {
            return portalLabels.firstOrNull { url.contains("/$it/", ignoreCase = true) }
        }

        fun getLabelOfPortal(url: String): String {
            return portalLabels.first { url.contains("/$it/", ignoreCase = true) }
        }

        fun isLabeledPortalPage(url: String): Boolean {
            return portalLabels.any { url.contains("/$it/", ignoreCase = true) }
        }

        fun isPrimaryLabeledPortalPage(url: String): Boolean {
            return isLabeledPortalPage(url) && !url.contains("&pg=")
        }

        fun isSecondaryLabeledPortalPage(url: String): Boolean {
            return isLabeledPortalPage(url) && url.contains("&pg=")
        }
    }

    private val metrics = AmazonMetrics(ident)

    fun mark(url: String) {
        if (isLabeledPortalPage(url)) {
            val label = getLabelOfPortal(url)
            when (label) {
                "zgbs" -> metrics.zgbs.mark()
                "most-wished-for" -> metrics.mWishedF.mark()
                "new-releases" -> metrics.nRelease.mark()
                "movers-and-shakers" -> metrics.mas.mark()
            }

            if (isSecondaryLabeledPortalPage(url)) {
                when (label) {
                    "zgbs" -> metrics.szgbs.mark()
                    "most-wished-for" -> metrics.smWishedF.mark()
                    "new-releases" -> metrics.snRelease.mark()
                    "movers-and-shakers" -> metrics.smas.mark()
                }
            }
        }
    }
}
