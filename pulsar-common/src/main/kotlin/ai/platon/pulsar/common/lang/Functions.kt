package ai.platon.pulsar.common.lang

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
