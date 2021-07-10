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
package ai.platon.pulsar.common.distributed.lock.mongo.impl

import ai.platon.pulsar.common.distributed.lock.AbstractSimpleLock
import ai.platon.pulsar.common.distributed.lock.mongo.model.LockDocument
import com.mongodb.DuplicateKeyException
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.function.Supplier

class SimpleMongoLock(
        tokenSupplier: Supplier<String>,
        private val mongoTemplate: MongoTemplate
) : AbstractSimpleLock(tokenSupplier) {
    private val log = LoggerFactory.getLogger(SimpleMongoLock::class.java)

    override fun acquire(key: String, storeId: String, token: String, expiration: Long): String? {
        val query = Query.query(Criteria.where("_id").`is`(key))
        val update = Update()
                .setOnInsert("_id", key)
                .setOnInsert("expireAt", LocalDateTime.now().plus(expiration, ChronoUnit.MILLIS))
                .setOnInsert("token", token)
        val options = FindAndModifyOptions().upsert(true).returnNew(true)
        val document = try {
            mongoTemplate.findAndModify(query, update, options, LockDocument::class.java, storeId)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            // @see https://jira.mongodb.org/browse/JAVA-1821
            // caused by com.mongodb.MongoCommandException, Command failed with error 11000 (DuplicateKey)
            // the exception is wrapped and corrected by spring to be DuplicateKeyException
            null
        }
        val locked = document?.token == token
        log.debug("Tried to acquire lock for key {} with token {} in store {}. Locked: {}", key, token, storeId, locked)
        return if (locked) token else null
    }

    override fun release(key: String, storeId: String, token: String): Boolean {
        val deleted = mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(key).and("token").`is`(token)), storeId)
        val released = deleted.deletedCount == 1L
        when {
            released -> {
                log.debug("Remove query successfully affected 1 record for key {} with token {} in store {}",
                    key, token, storeId)
            }
            deleted.deletedCount > 0 -> {
                log.error("Unexpected result from release for key {} with token {} in store {}, released {}",
                    key, token, storeId, deleted)
            }
            else -> {
                log.error("Remove query did not affect any records for key {} with token {} in store {}",
                    key, token, storeId)
            }
        }
        return released
    }

    override fun refresh(key: String, storeId: String, token: String, expiration: Long): Boolean {
        val updated = mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`(key).and("token").`is`(token)),
                Update.update("expireAt", LocalDateTime.now().plus(expiration, ChronoUnit.MILLIS)),
                storeId)
        val refreshed = updated.modifiedCount == 1L
        when {
            refreshed -> {
                log.debug("Refresh query successfully affected 1 record for key {} with token {} in store {}",
                        key, token, storeId)
            }
            updated.modifiedCount > 0 -> {
                log.error("Unexpected result from refresh for key {} with token {} in store {}, released {}",
                        key, token, storeId, updated)
            }
            else -> {
                log.warn("Refresh query did not affect any records for key {} with token {} in store {}. " +
                        "This is possible when refresh interval fires for the final time after the lock has been released",
                        key, token, storeId)
            }
        }
        return refreshed
    }
}
