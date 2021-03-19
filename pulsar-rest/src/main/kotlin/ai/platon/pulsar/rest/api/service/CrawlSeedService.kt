package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.rest.api.persist.CrawlSeedRepository
import org.springframework.stereotype.Service

@Service
class CrawlSeedService(
        private val crawlSeedRepository: CrawlSeedRepository
)
