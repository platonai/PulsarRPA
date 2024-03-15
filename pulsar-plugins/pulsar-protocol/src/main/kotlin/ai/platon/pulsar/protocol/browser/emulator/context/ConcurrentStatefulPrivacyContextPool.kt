package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.privacy.CloseStrategy
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class ConcurrentStatefulPrivacyContextPool(
    val proxyPoolManager: ProxyPoolManager,
    val driverPoolManager: WebDriverPoolManager,
    val coreMetrics: CoreMetrics,
    val conf: ImmutableConfig,
    val allowedPrivacyContextCount: Int
) {
    private val logger = getLogger(this)
    
    /**
     * life cycle of the permanent context is relatively long. The system will never delete the permanent contexts.
     *
     * The predefined privacy agents for permanent contexts are:
     *
     * 1. PrivacyAgent.USER_DEFAULT
     * 2. PrivacyAgent.PROTOTYPE
     * 2. PrivacyAgent.DEFAULT
     * */
    private val _permanentContexts = ConcurrentHashMap<PrivacyAgent, PrivacyContext>()
    
    /**
     * The life cycle of the temporary context is very short. Whenever the system detects that the
     * privacy context is leaked, the system discards the leaked context and creates a new one.
     *
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    private val _temporaryContexts = ConcurrentHashMap<PrivacyAgent, PrivacyContext>()
    
    private val _zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    
    private val _deadContexts = ConcurrentLinkedDeque<PrivacyContext>()
    
    val permanentContexts: Map<PrivacyAgent, PrivacyContext> get() = _permanentContexts
    
    val temporaryContexts: Map<PrivacyAgent, PrivacyContext> get() = _temporaryContexts
    
    val zombieContexts: Deque<PrivacyContext> get() = _zombieContexts
    
    val deadContexts: Deque<PrivacyContext> get() = _deadContexts
    
    @get:Synchronized
    val activeContexts get() = permanentContexts + temporaryContexts
    
    @get:Synchronized
    val activeContextCount get() = permanentContexts.size + temporaryContexts.size

    @Throws(ProxyException::class)
    fun computeIfAbsent(privacyAgent: PrivacyAgent): PrivacyContext {
        return if (privacyAgent.isPermanent) {
            _permanentContexts.computeIfAbsent(privacyAgent) { createUnmanagedContext(privacyAgent) }
        } else {
            _temporaryContexts.computeIfAbsent(privacyAgent) { createUnmanagedContext(privacyAgent) }
        }
    }
    
    /**
     * Create a privacy context who is not added to the context list yet.
     * */
    @Throws(ProxyException::class)
    fun createUnmanagedContext(privacyAgent: PrivacyAgent): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, privacyAgent)
        if (privacyAgent.isPermanent) {
            logger.info("Permanent privacy context is created #{} | {}", context.display, context.baseDir)
        } else {
            logger.info(
                "Temporary privacy context is created #{}, active: {}, allowed: {} | {}",
                context.display, temporaryContexts.size, allowedPrivacyContextCount, context.baseDir
            )
        }
        return context
    }
    
    /**
     * Close the zombie contexts, and the resources release immediately.
     * */
    @Synchronized
    fun closeDyingContexts() {
        logger.debug("Closing zombie contexts ...")
        
        val dyingContexts = zombieContexts.filter { !it.isClosed }
        if (dyingContexts.isEmpty()) {
            return
        }
        
        logger.debug("Closing {} zombie contexts ...", dyingContexts.size)
        
        dyingContexts.forEach { privacyContext ->
            privacyContext.runCatching { close() }.onFailure { warnForClose(this, it) }
            
            zombieContexts.remove(privacyContext)
            deadContexts.add(privacyContext)
        }
    }
    
    @Synchronized
    fun close(privacyContext: PrivacyContext) {
        val privacyAgent = privacyContext.privacyAgent
        
        _permanentContexts.remove(privacyAgent)
        _temporaryContexts.remove(privacyAgent)
        
        if (!_zombieContexts.contains(privacyContext)) {
            // every time we add the item to the head,
            // so when we report the deque, the latest contexts are reported.
            _zombieContexts.addFirst(privacyContext)
        }
        
        closeDyingContexts()
    }
}
