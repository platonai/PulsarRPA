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

    override val size get() = priorityActiveQueues.size

    val costReport: String
        get() {
            return activeQueues.values
                    .sortedByDescending { it.averageTimeCost }
                    .take(50)
                    .joinToString("\n") { it.costReport }
        }

    override fun add(taskPool: TaskPool?): Boolean {
        if (taskPool == null) {
            return false
        }

        priorityActiveQueues.add(taskPool)
        activeQueues[taskPool.id] = taskPool

        if (priorityActiveQueues.size != activeQueues.size) {
            LOG.error("(Add)Inconsistent status : size of activeQueues (" + priorityActiveQueues.size + ") " +
                    "and priorityActiveQueues (" + activeQueues.size + ") do not match")
        }

        return true
    }

    override fun offer(taskPool: TaskPool): Boolean {
        return add(taskPool)
    }

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

    override fun peek(): TaskPool? {
        return priorityActiveQueues.peek()
    }

    override fun remove(taskPool: TaskPool): Boolean {
        priorityActiveQueues.remove(taskPool)
        activeQueues.remove(taskPool.id)
        inactiveQueues.remove(taskPool.id)

        return true
    }

    override fun iterator(): MutableIterator<TaskPool> {
        return priorityActiveQueues.iterator()
    }

    override fun isEmpty(): Boolean {
        return priorityActiveQueues.isEmpty()
    }

    override fun clear() {
        priorityActiveQueues.clear()
        activeQueues.clear()
        inactiveQueues.clear()
    }

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
    fun disable(pool: TaskPool) {
        priorityActiveQueues.remove(pool)
        activeQueues.remove(pool.id)

        pool.disable()
        inactiveQueues[pool.id] = pool
    }

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

    fun find(id: PoolId): TaskPool? {
        return search(id, false)
    }

    fun findExtend(id: PoolId): TaskPool? {
        return search(id, true)
    }

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

    fun dump(limit: Int, drop: Boolean) {
        LOG.info("Report fetch queue status. "
                + "Active : " + activeQueues.size + ", inactive : " + inactiveQueues.size + " ...")
        activeQueues.values.take(limit).filter { it.hasTasks() }.forEach { it.dump(drop) }
        inactiveQueues.values.take(limit).filter { it.hasPendingTasks() }.forEach { it.dump(drop) }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(PoolQueue::class.java)
    }
}
