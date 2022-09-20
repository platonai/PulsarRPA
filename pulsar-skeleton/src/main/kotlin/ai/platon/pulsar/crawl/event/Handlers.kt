package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.lang.*
import java.util.concurrent.CopyOnWriteArrayList

interface PHandler

abstract class AbstractHandler: PHandler, PFunction {
    override val name: String = ""
    override val priority: Int = 0
    override val isRelevant: Boolean = true
}

abstract class AbstractHandler0<R>: AbstractHandler(), PFunction0<R>

abstract class AbstractHandler1<T, R>: AbstractHandler(), PFunction1<T, R>

abstract class AbstractHandler2<T, T2, R>: AbstractHandler(), PFunction2<T, T2, R>

abstract class AbstractHandler3<T, T2, T3, R>: AbstractHandler(), PFunction3<T, T2, T3, R>

abstract class AbstractDHandler0<R>: AbstractHandler(), PDFunction0<R>

abstract class AbstractDHandler1<T, R>: AbstractHandler(), PDFunction1<T, R>

abstract class AbstractDHandler2<T, T2, R>: AbstractHandler(), PDFunction2<T, T2, R>

abstract class AbstractDHandler3<T, T2, T3, R>: AbstractHandler(), PDFunction3<T, T2, T3, R>

interface ChainedHandler {
    val size: Int
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty

    fun remove(handler: Any): Boolean
    fun clear()
}

abstract class AbstractChainedHandler0<R>: AbstractHandler0<R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction0<R>): AbstractChainedHandler0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction0<R>): AbstractChainedHandler0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractHandler0<R>() {
        override fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractHandler0<R>() {
        override fun invoke() = handler()
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it() }
        return r
    }
}

abstract class AbstractChainedHandler1<T, R>: AbstractHandler1<T, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction1<T, R>): AbstractChainedHandler1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction1<T, R>): AbstractChainedHandler1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractHandler1<T, R>() {
        override fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractHandler1<T, R>() {
        override fun invoke(param: T) = handler(param)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(param: T): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param) }
        return r
    }
}

abstract class AbstractChainedHandler2<T, T2, R>: AbstractHandler2<T, T2, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction2<T, T2, R>): AbstractChainedHandler2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction2<T, T2, R>): AbstractChainedHandler2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T, T2) -> R) = addFirst(object: AbstractHandler2<T, T2, R>() {
        override fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: (T, T2) -> R) = addLast(object: AbstractHandler2<T, T2, R>() {
        override fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(param: T, param2: T2): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param, param2) }
        return r
    }
}

abstract class AbstractChainedDHandler0<R>: AbstractDHandler0<R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction0<R>): AbstractChainedDHandler0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction0<R>): AbstractChainedDHandler0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractDHandler0<R>() {
        override suspend fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractDHandler0<R>() {
        override suspend fun invoke() = handler()
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it() }
        return r
    }
}

abstract class AbstractChainedDHandler1<T, R>: AbstractDHandler1<T, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction1<T, R>): AbstractChainedDHandler1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction1<T, R>): AbstractChainedDHandler1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractDHandler1<T, R>() {
        override suspend fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractDHandler1<T, R>() {
        override suspend fun invoke(param: T) = handler(param)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(param: T): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param) }
        return r
    }
}

abstract class AbstractChainedDHandler2<T, T2, R>: AbstractDHandler2<T, T2, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction2<T, T2, R>): AbstractChainedDHandler2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction2<T, T2, R>): AbstractChainedDHandler2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: suspend (T, T2) -> R) = addFirst(object: AbstractDHandler2<T, T2, R>() {
        override suspend fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: suspend (T, T2) -> R) = addLast(object: AbstractDHandler2<T, T2, R>() {
        override suspend fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override suspend operator fun invoke(param: T, param2: T2): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it(param, param2) }
        return r
    }
}
