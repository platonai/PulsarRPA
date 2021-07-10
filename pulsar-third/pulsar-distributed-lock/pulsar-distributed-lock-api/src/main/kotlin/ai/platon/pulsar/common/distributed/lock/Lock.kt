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

interface Lock {
    /**
     * Try to acquire the lock.
     *
     * @param keys       keys to try to lock
     * @param storeId    lock store id to save keys in (table, collection, ...)
     * @param expiration how long to wait before releasing the key automatically, in millis
     * @return token to use for releasing the lock or `null` if lock cannot be acquired at the moment
     */
    fun acquire(keys: List<String>, storeId: String, expiration: Long): String?

    /**
     * Try to release the lock if token held by the lock has not changed.
     *
     * @param keys    keys to try to unlock
     * @param storeId lock store id to release keys in (table, collection, ...)
     * @param token   token used to check if lock is still held by this lock
     * @return `true` if lock was successfully released, `false` otherwise
     */
    fun release(keys: List<String>, storeId: String, token: String): Boolean

    /**
     * Try to refresh the lock expiration.
     *
     * @param keys       keys to try to refresh
     * @param storeId    lock store id to refresh keys in (table, collection, ...)
     * @param expiration how long to wait before releasing the key automatically, in millis
     * @param token      token used to check if lock is still held by this lock
     * @return `true` if lock was successfully refreshed, `false` otherwise
     */
    fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean
}
