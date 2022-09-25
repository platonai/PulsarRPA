package ai.platon.pulsar.common.lang

import java.util.concurrent.CopyOnWriteArrayList

interface PFunction {
    val name: String
    val priority: Int
    val isRelevant: Boolean
}

interface PFunction0<R>: PFunction {
    operator fun invoke(): R?
}

interface PFunction1<T, R>: PFunction {
    operator fun invoke(param: T): R?
}

interface PFunction2<T, T2, R>: PFunction {
    operator fun invoke(param: T, param2: T2): R?
}

interface PFunction3<T, T2, T3, R>: PFunction {
    operator fun invoke(param: T, param2: T2, param3: T3): R?
}

interface PDFunction0<R>: PFunction {
    suspend operator fun invoke(): R?
}

interface PDFunction1<T, R>: PFunction {
    suspend operator fun invoke(param: T): R?
}

interface PDFunction2<T, T2, R>: PFunction {
    suspend operator fun invoke(param: T, param2: T2): R?
}

interface PDFunction3<T, T2, T3, R>: PFunction {
    suspend operator fun invoke(param: T, param2: T2, param3: T3): R?
}

interface PHandler {
    val name: String
    val priority: Int
    val isRelevant: Boolean
}

interface PHandler0: PHandler {
    operator fun invoke()
}

interface PHandler1<T>: PHandler {
    operator fun invoke(param: T)
}

interface PHandler2<T, T2>: PHandler {
    operator fun invoke(param: T, param2: T2)
}

interface PHandler3<T, T2, T3>: PHandler {
    operator fun invoke(param: T, param2: T2, param3: T3)
}

interface PDHandler0: PHandler {
    suspend operator fun invoke()
}

interface PDHandler1<T>: PHandler {
    suspend operator fun invoke(param: T)
}

interface PDHandler2<T, T2>: PHandler {
    suspend operator fun invoke(param: T, param2: T2)
}

interface PDHandler3<T, T2, T3>: PHandler {
    suspend operator fun invoke(param: T, param2: T2, param3: T3)
}

abstract class AbstractPFunction: PHandler, PFunction {
    override val name: String = ""
    override val priority: Int = 0
    override val isRelevant: Boolean = true
}

abstract class AbstractPHandler: PHandler {
    override val name: String = ""
    override val priority: Int = 0
    override val isRelevant: Boolean = true
}

abstract class AbstractPFunction0<R>: AbstractPFunction(), PFunction0<R>

abstract class AbstractPFunction1<T, R>: AbstractPFunction(), PFunction1<T, R>

abstract class AbstractPFunction2<T, T2, R>: AbstractPFunction(), PFunction2<T, T2, R>

abstract class AbstractPFunction3<T, T2, T3, R>: AbstractPFunction(), PFunction3<T, T2, T3, R>

abstract class AbstractPDFunction0<R>: AbstractPFunction(), PDFunction0<R>

abstract class AbstractPDFunction1<T, R>: AbstractPFunction(), PDFunction1<T, R>

abstract class AbstractPDFunction2<T, T2, R>: AbstractPFunction(), PDFunction2<T, T2, R>

abstract class AbstractPDFunction3<T, T2, T3, R>: AbstractPFunction(), PDFunction3<T, T2, T3, R>

abstract class AbstractPHandler0: AbstractPHandler(), PHandler0

abstract class AbstractPHandler1<T>: AbstractPHandler(), PHandler1<T>

abstract class AbstractPHandler2<T, T2>: AbstractPHandler(), PHandler2<T, T2>

abstract class AbstractPHandler3<T, T2, T3>: AbstractPHandler(), PHandler3<T, T2, T3>

abstract class AbstractPDHandler0: AbstractPHandler(), PDHandler0

abstract class AbstractPDHandler1<T>: AbstractPHandler(), PDHandler1<T>

abstract class AbstractPDHandler2<T, T2>: AbstractPHandler(), PDHandler2<T, T2>

abstract class AbstractPDHandler3<T, T2, T3>: AbstractPHandler(), PDHandler3<T, T2, T3>

interface ChainedHandler {
    val size: Int
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty

    fun remove(handler: Any): Boolean
    fun clear()
}

abstract class AbstractChainedFunction0<R>: AbstractPFunction0<R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction0<R>): AbstractChainedFunction0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction0<R>): AbstractChainedFunction0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractPFunction0<R>() {
        override fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractPFunction0<R>() {
        override fun invoke() = handler()
    })

    /**
     * TODO: remove does not work if the handler is added as a lambda function
     * */
    override fun remove(handler: Any) = registeredHandlers.remove(handler)

    override fun clear() = registeredHandlers.clear()

    override operator fun invoke(): R? {
        var r: R? = null
        registeredHandlers.asSequence().filter { it.isRelevant }.forEach { r = it() }
        return r
    }
}

abstract class AbstractChainedFunction1<T, R>: AbstractPFunction1<T, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction1<T, R>): AbstractChainedFunction1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction1<T, R>): AbstractChainedFunction1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractPFunction1<T, R>() {
        override fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractPFunction1<T, R>() {
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

abstract class AbstractChainedFunction2<T, T2, R>: AbstractPFunction2<T, T2, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PFunction2<T, T2, R>): AbstractChainedFunction2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PFunction2<T, T2, R>): AbstractChainedFunction2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T, T2) -> R) = addFirst(object: AbstractPFunction2<T, T2, R>() {
        override fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: (T, T2) -> R) = addLast(object: AbstractPFunction2<T, T2, R>() {
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

abstract class AbstractChainedPDFunction0<R>: AbstractPDFunction0<R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction0<R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction0<R>): AbstractChainedPDFunction0<R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction0<R>): AbstractChainedPDFunction0<R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: () -> R) = addFirst(object: AbstractPDFunction0<R>() {
        override suspend fun invoke() = handler()
    })

    fun addLast(handler: () -> R) = addLast(object: AbstractPDFunction0<R>() {
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

abstract class AbstractChainedPDFunction1<T, R>: AbstractPDFunction1<T, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction1<T, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction1<T, R>): AbstractChainedPDFunction1<T, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction1<T, R>): AbstractChainedPDFunction1<T, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: (T) -> R) = addFirst(object: AbstractPDFunction1<T, R>() {
        override suspend fun invoke(param: T) = handler(param)
    })

    fun addLast(handler: (T) -> R) = addLast(object: AbstractPDFunction1<T, R>() {
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

abstract class AbstractChainedPDFunction2<T, T2, R>: AbstractPDFunction2<T, T2, R>(), ChainedHandler {
    private val registeredHandlers = CopyOnWriteArrayList<PDFunction2<T, T2, R>>()

    override val size: Int
        get() = registeredHandlers.size

    fun addFirst(handler: PDFunction2<T, T2, R>): AbstractChainedPDFunction2<T, T2, R> {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addLast(handler: PDFunction2<T, T2, R>): AbstractChainedPDFunction2<T, T2, R> {
        registeredHandlers.add(handler)
        return this
    }

    fun addFirst(handler: suspend (T, T2) -> R) = addFirst(object: AbstractPDFunction2<T, T2, R>() {
        override suspend fun invoke(param: T, param2: T2) = handler(param, param2)
    })

    fun addLast(handler: suspend (T, T2) -> R) = addLast(object: AbstractPDFunction2<T, T2, R>() {
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
