package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.warnInterruptible
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class ConcurrentStatefulPrivacyContextPool(

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
    
    /**
     * Close the zombie contexts, and the resources release immediately.
     * */
    @Synchronized
    fun closeHistoricalContexts() {
        logger.debug("Closing zombie contexts ...")
        
        val dyingContexts = zombieContexts.filter { !it.isClosed }
        if (dyingContexts.isEmpty()) {
            return
        }
        
        logger.debug("Closing {} zombie contexts ...", dyingContexts.size)
        
        dyingContexts.forEach { privacyContext ->
            privacyContext.runCatching { close() }.onFailure { warnInterruptible(this, it) }
            
            zombieContexts.remove(privacyContext)
            deadContexts.add(privacyContext)
        }
    }
    
    @Synchronized
    fun close() {
        _permanentContexts.clear()
        _temporaryContexts.clear()
        _zombieContexts.clear()
        _deadContexts.clear()
    }
}
