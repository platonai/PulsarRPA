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
package ai.platon.pulsar.common.distributed.lock

import org.springframework.util.Assert
import java.util.function.Supplier

/**
 * An abstract lock used as a base for all locks that operate with only 1 key instead of multiple keys.
 */
abstract class AbstractSimpleLock(
    val tokenSupplier: Supplier<String>
) : Lock {

    override fun acquire(keys: List<String>, storeId: String, expiration: Long): String? {
        Assert.isTrue(keys.size == 1, "Cannot acquire lock for multiple keys with this lock")
        val token = tokenSupplier.get()
        check(token.isNotEmpty()) { "Cannot lock with empty token" }
        return acquire(keys[0], storeId, token, expiration)
    }

    override fun release(keys: List<String>, storeId: String, token: String): Boolean {
        Assert.isTrue(keys.size == 1, "Cannot release lock for multiple keys with this lock")
        return release(keys[0], storeId, token)
    }

    override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
        Assert.isTrue(keys.size == 1, "Cannot refresh lock for multiple keys with this lock")
        return refresh(keys[0], storeId, token, expiration)
    }

    protected abstract fun acquire(key: String, storeId: String, token: String, expiration: Long): String?

    protected abstract fun release(key: String, storeId: String, token: String): Boolean

    protected abstract fun refresh(key: String, storeId: String, token: String, expiration: Long): Boolean
}
