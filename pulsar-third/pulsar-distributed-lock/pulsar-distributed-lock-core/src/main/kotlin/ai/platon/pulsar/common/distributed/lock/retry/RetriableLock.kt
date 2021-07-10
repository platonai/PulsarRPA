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
package ai.platon.pulsar.common.distributed.lock.retry

import ai.platon.pulsar.common.distributed.lock.Lock
import ai.platon.pulsar.common.distributed.lock.exception.LockNotAvailableException
import org.springframework.retry.RetryContext
import org.springframework.retry.support.RetryTemplate

/**
 * A [Lock] wrapper for retrying [.acquire] method calls. This wrapper will retry the acquire method
 * only as specified by the provided [RetryTemplate].
 */
class RetriableLock(
    var lock: Lock,
    var retryTemplate: RetryTemplate
) : Lock {

    override fun acquire(keys: List<String>, storeId: String, expiration: Long): String {
        return retryTemplate.execute<String, RuntimeException> { ctx: RetryContext ->
            // catch exceptions?
            val token = lock.acquire(keys, storeId, expiration)
            if (token.isNullOrBlank()) {
                throw LockNotAvailableException(String.format("Lock not available for keys: %s in store %s", keys, storeId))
            }
            token
        }
    }

    override fun release(keys: List<String>, storeId: String, token: String): Boolean {
        return lock.release(keys, storeId, token)
    }

    override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
        return lock.refresh(keys, storeId, token, expiration)
    }
}
