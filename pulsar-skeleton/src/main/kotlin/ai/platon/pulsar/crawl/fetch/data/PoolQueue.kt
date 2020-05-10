package ai.platon.pulsar.crawl.fetch.data

import ai.platon.pulsar.crawl.fetch.TaskPool
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by vincent on 16-9-22.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class PoolQueue: AbstractQueue<TaskPool>() {

    /** All fetch queues, indexed by priority, item with bigger priority comes first.  */
    private val priorityActiveQueues = PriorityQueue(Comparator.reverseOrder<TaskPool>())

    /** All fetch queues, indexed by queue id.  */
    private val activeQueues = HashMap<PoolId, TaskPool>()

    /** Retired queues do not serve any more, but the tasks can be find out by findExtend.  */
    private val inactiveQueues = HashMap<PoolId, TaskPool>()

    @get:Synchronized
    override val size get() = priorityActiveQueues.size

    @get:Synchronized
    val timeReport: String get() = activeQueues.values.sortedByDescending { it.averageTime }
            .take(50).joinToString("\n") { it.timeReport }

    @Synchronized
    override fun add(element: TaskPool): Boolean {
        priorityActiveQueues.add(element)
        activeQueues[element.id] = element

        if (priorityActiveQueues.size != activeQueues.size) {
            LOG.error("(Add)Inconsistent status : size of activeQueues (" + priorityActiveQueues.size + ") " +
                    "and priorityActiveQueues (" + activeQueues.size + ") do not match")
        }

        return true
    }

    @Synchronized
    override fun offer(taskPool: TaskPool): Boolean {
        return add(taskPool)
    }

    @Synchronized
    override fun poll(): TaskPool? {
        val queue = priorityActiveQueues.poll()
        if (queue != null) {
            activeQueues.remove(queue.id)
        }

        if (priorityActiveQueues.size != activeQueues.size) {
            LOG.error("(Poll)Inconsistent status : size of activeQueues (" + priorityActiveQueues.size + ") " +
                    "and priorityActiveQueues (" + activeQueues.size + ") do not match")
        }

        return queue
    }

    @Synchronized
    override fun peek(): TaskPool? {
        return priorityActiveQueues.peek()
    }

    @Synchronized
    override fun remove(element: TaskPool): Boolean {
        priorityActiveQueues.remove(element)
        activeQueues.remove(element.id)
        inactiveQueues.remove(element.id)

        return true
    }

    override fun iterator(): MutableIterator<TaskPool> {
        throw IllegalAccessException("Iteration is not supported")
    }

    @Synchronized
    override fun isEmpty(): Boolean {
        return priorityActiveQueues.isEmpty()
    }

    @Synchronized
    override fun clear() {
        priorityActiveQueues.clear()
        activeQueues.clear()
        inactiveQueues.clear()
    }

    @Synchronized
    fun enable(queue: TaskPool) {
        queue.enable()

        priorityActiveQueues.add(queue)
        activeQueues[queue.id] = queue
        inactiveQueues.remove(queue.id)
    }

    /**
     * Retired queues do not serve any more, but the tasks can be find out and finished.
     * The tasks in detached queues can be find out to finish.
     *
     * A queue should be detached if
     * 1. the queue is too slow, or
     * 2. all tasks are done
     */
    @Synchronized
    fun disable(pool: TaskPool) {
        priorityActiveQueues.remove(pool)
        activeQueues.remove(pool.id)

        pool.disable()
        inactiveQueues[pool.id] = pool
    }

    @Synchronized
    fun hasPriorPendingTasks(priority: Int): Boolean {
        var hasPrior = false
        for (queue in priorityActiveQueues) {
            if (queue.priority < priority) {
                break
            }

            hasPrior = queue.hasPendingTasks()
        }

        return hasPrior || inactiveQueues.values.any { it.priority >= priority && it.hasPendingTasks() }
    }

    @Synchronized
    fun find(id: PoolId): TaskPool? {
        return search(id, false)
    }

    @Synchronized
    fun findExtend(id: PoolId): TaskPool? {
        return search(id, true)
    }

    @Synchronized
    fun search(id: PoolId?, searchInactive: Boolean): TaskPool? {
        var queue: TaskPool? = null

        if (id != null) {
            queue = activeQueues[id]

            if (queue == null && searchInactive) {
                queue = inactiveQueues[id]
            }
        }

        return queue
    }

    @Synchronized
    fun dump(limit: Int, drop: Boolean) {
        LOG.info("Fetch queue status | active: {}, inactive: {}", activeQueues.size, inactiveQueues.size)

        activeQueues.values.take(limit).filter { it.hasTasks() }.forEach { it.dump(drop) }
        inactiveQueues.values.take(limit).filter { it.hasPendingTasks() }.forEach { it.dump(drop) }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(PoolQueue::class.java)
    }
}
