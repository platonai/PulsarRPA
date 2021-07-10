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
import org.assertj.core.api.Assertions
import org.junit.Test
import org.springframework.core.convert.support.DefaultConversionService
import java.lang.reflect.Method

class SpelKeyGeneratorTest {
    private val keyGenerator: KeyGenerator = SpelKeyGenerator(DefaultConversionService())
    private val service = MessageService()
    private val sendMessageMethod: Method = MessageService::class.java.getMethod("sendMessage", String::class.java)

    @Test
    fun shouldGenerateExecutionPath() {
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", "#executionPath", service, sendMessageMethod, arrayOf<Any>("hello")))
                .containsExactly("lock_ai.platon.pulsar.common.distributed.lock.key.SpelKeyGeneratorTest.MessageService.sendMessage")
    }

    @Test
    fun shouldGenerateSingleKeyFromContextAndVariables() {
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", "#p0", service, sendMessageMethod, arrayOf<Any>("hello")))
                .containsExactly("lock_hello")
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", "#a0", service, sendMessageMethod, arrayOf<Any>("hello")))
                .containsExactly("lock_hello")
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", "#message", service, sendMessageMethod, arrayOf<Any>("hello")))
                .containsExactly("lock_hello")
    }

    @Test
    fun shouldGenerateMultipleKeysFromContextAndVariablesWithList() {
        val expression = "T(ai.platon.pulsar.common.distributed.lock.key.SpelKeyGeneratorTest).generateKeys(#message)"
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, arrayOf<Any>("p_")))
                .containsExactly("lock_p_first", "lock_p_second")
    }

    @Test
    fun shouldGenerateMultipleKeysFromContextAndVariablesWithArray() {
        val expression = "T(ai.platon.pulsar.common.distributed.lock.key.SpelKeyGeneratorTest).generateArrayKeys(#message)"
        Assertions.assertThat(keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, arrayOf<Any>("p_")))
                .containsExactly("lock_p_first", "lock_15")
    }

    @Test
    fun shouldGenerateMultipleKeysFromContextAndVariablesWithMixedTypeValues() {
        val expression = "T(ai.platon.pulsar.common.distributed.lock.key.SpelKeyGeneratorTest).generateMixedKeys(#message)"
        val keys = keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, arrayOf<Any>("p_"))
        Assertions.assertThat(keys).containsExactly("lock_p_first", "lock_15")
    }

    @Test
    fun shouldFailWithExpressionThatEvaluatesInNull() {
        Assertions.assertThatThrownBy {
            keyGenerator.resolveKeys("lock_", "null", service, sendMessageMethod, arrayOf<Any>("hello"))
        }.isInstanceOf(EvaluationConvertException::class.java)
    }

    @Test
    fun shouldFailWithExpressionThatEvaluatesInEmptyList() {
        Assertions.assertThatThrownBy {
            keyGenerator.resolveKeys("lock_", "T(java.util.Collections).emptyList()", service, sendMessageMethod, arrayOf<Any>("hello"))
        }.isInstanceOf(EvaluationConvertException::class.java)
    }

    @Test
    fun shouldFailWithExpressionThatEvaluatesInListWithNullValue() {
        Assertions.assertThatThrownBy {
            keyGenerator.resolveKeys("lock_", "T(java.util.Collections).singletonList(null)", service, sendMessageMethod, arrayOf<Any>("hello"))
        }.isInstanceOf(EvaluationConvertException::class.java)
    }

    private class MessageService {
        fun sendMessage(message: String?) {}
    }

    companion object {
        @JvmStatic
        fun generateKeys(prefix: String): List<String> {
            return listOf(prefix + "first", prefix + "second")
        }

        @JvmStatic
        fun generateArrayKeys(prefix: String): Array<Any> {
            return arrayOf(prefix + "first", 15)
        }

        @JvmStatic
        fun generateMixedKeys(prefix: String): Set<Any> {
            return setOf(prefix + "first", 15)
        }
    }
}
