/*
 * MIT License
 *
 * Copyright (c) 2020 Alen Turkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ai.platon.pulsar.common.distributed.lock.key

import ai.platon.pulsar.common.distributed.lock.exception.EvaluationConvertException
import org.springframework.context.expression.AnnotatedElementKey
import org.springframework.context.expression.CachedExpressionEvaluator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.expression.Expression
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class SpelKeyGenerator(
        val conversionService: ConversionService,
) : CachedExpressionEvaluator(), KeyGenerator {
    private val conditionCache: Map<ExpressionKey, Expression> = ConcurrentHashMap()

    override fun resolveKeys(prefix: String, expression: String, obj: Any, method: Method, args: Array<Any>): List<String> {
        val expressionValue = evaluateExpression(expression, obj, method, args)
        val keys = convertResultToList(expressionValue)

        if (keys.any { it == null }) {
            throw EvaluationConvertException("null keys are not supported: $keys")
        }

        return if (prefix.isBlank()) keys.mapNotNull { it } else keys.mapNotNull { prefix + it }
    }

    protected fun convertResultToList(expressionValue: Any): List<String?> {
        val list = (expressionValue as? Iterable<*>)?.let { iterableToList(it) }
                ?: if (expressionValue.javaClass.isArray) arrayToList(expressionValue) else listOf(expressionValue.toString())

        if (list.isEmpty()) {
            throw EvaluationConvertException("Expression evaluated in an empty list")
        }

        return list
    }

    private fun evaluateExpression(expression: String, any: Any, method: Method, args: Array<Any>): Any {
        val context = MethodBasedEvaluationContext(any, method, args, super.getParameterNameDiscoverer())
        context.setVariable("executionPath", any.javaClass.canonicalName + "." + method.name)
        val evaluatedExpression = getExpression(conditionCache, AnnotatedElementKey(method, any.javaClass), expression)
        return evaluatedExpression.getValue(context) ?: throw EvaluationConvertException("Expression evaluated in a null")
    }

    private fun iterableToList(expressionValue: Any): List<String?> {
        val genericCollection = TypeDescriptor.collection(MutableCollection::class.java, TypeDescriptor.valueOf(Any::class.java))
        return toList(expressionValue, genericCollection)
    }

    private fun arrayToList(expressionValue: Any): List<String?> {
        val genericArray = TypeDescriptor.array(TypeDescriptor.valueOf(Any::class.java))
        return toList(expressionValue, genericArray!!)
    }

    private fun toList(expressionValue: Any, from: TypeDescriptor): List<String?> {
        val listTypeDescriptor = TypeDescriptor.collection(List::class.java, TypeDescriptor.valueOf(String::class.java))
        return conversionService.convert(expressionValue, from, listTypeDescriptor) as List<String?>
    }

    override fun equals(other: Any?): Boolean {
        return other is SpelKeyGenerator
                && other.conversionService == conversionService
                && other.conditionCache == conditionCache
    }

    override fun hashCode(): Int {
        val p = 59
        var r = -1
        conditionCache.entries.forEach { (key, exp) -> r += p * r + key.hashCode() + exp.hashCode() }
        return r
    }
}
