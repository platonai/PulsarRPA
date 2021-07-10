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
package ai.platon.pulsar.common.distributed.lock.advice.support

import ai.platon.pulsar.common.distributed.lock.Lock
import org.slf4j.LoggerFactory
import java.util.*

class SimpleLock : Lock {
    private val log = LoggerFactory.getLogger(SimpleLock::class.java)

    val lockMap: MutableMap<String, MutableList<LockedKey>> = HashMap()

    override fun acquire(keys: List<String>, storeId: String, expiration: Long): String? {
        log.debug("Acquiring lock for keys {} in store {} with expiration: {}", keys, storeId, expiration)
        val lockedKeysWithExpiration = lockMap[storeId]
        if (lockedKeysWithExpiration != null) {
            val locksForStore = lockedKeysWithExpiration.map { it.key }
            if (keys.any { locksForStore.contains(it) }) {
                return null
            }
        }

        val lockedKeys = lockMap.computeIfAbsent(storeId) { ArrayList() }
        keys.forEach {
            val key = LockedKey(it, expiration, System.currentTimeMillis(), System.currentTimeMillis(), 0, false)
            lockedKeys.add(key)
        }

        return UUID.randomUUID().toString()
    }

    override fun release(keys: List<String>, storeId: String, token: String): Boolean {
        log.debug("Releasing keys {} in store {} with token {}", keys, storeId, token)
        val lockedKeys: List<LockedKey> = lockMap[storeId]!!
        if (lockedKeys.isEmpty()) {
            return false
        }

        lockedKeys.forEach {
            if (keys.contains(it.key)) {
                it.released = true
            }
        }

        return true
    }

    override fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean {
        log.debug("Refreshing keys {} in store {} with token {} to expiration: {}", keys, storeId, token, expiration)
        val lockedKeys = lockMap[storeId] ?: return false
        lockedKeys.forEach {
            if (!it.released) {
                it.expiration = expiration
                it.newUpdate()
            }
        }

        return true
    }

    fun getLockedKeys(storeId: String): List<String> {
        return lockMap[storeId]?.map { it.key }?.toList() ?: listOf()
    }

    data class LockedKey(
            val key: String,
            var expiration: Long = 0,
            var updatedAt: Long = 0,
            var createdAt: Long = 0,
            var updateCounter: Long = 0,
            var released: Boolean = false,
    ) {
        fun newUpdate() {
            updatedAt = System.currentTimeMillis()
            updateCounter++
        }
    }
}
