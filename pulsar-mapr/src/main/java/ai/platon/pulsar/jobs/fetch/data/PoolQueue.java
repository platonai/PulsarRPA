package ai.platon.pulsar.jobs.fetch.data;

import ai.platon.pulsar.jobs.fetch.FetchMonitor;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

/**
 * Created by vincent on 16-9-22.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class PoolQueue extends AbstractQueue<TaskPool> {

  public final Logger LOG = FetchMonitor.LOG;

  /** All fetch queues, indexed by priority, item with bigger priority comes first. */
  private final PriorityQueue<TaskPool> priorityActiveQueues = new PriorityQueue<>(Comparator.reverseOrder());

  /** All fetch queues, indexed by queue id. */
  private final Map<PoolId, TaskPool> activeQueues = new HashMap<>();

  /** Retired queues do not serve any more, but the tasks can be find out by findExtend. */
  private final Map<PoolId, TaskPool> inactiveQueues = new HashMap<>();

  @Override
  public boolean add(TaskPool fetchQueue) {
    if (fetchQueue == null) {
      return false;
    }

    priorityActiveQueues.add(fetchQueue);
    activeQueues.put(fetchQueue.getId(), fetchQueue);

    if(priorityActiveQueues.size() != activeQueues.size()) {
      LOG.error("(Add)Inconsistent status : size of activeQueues (" + priorityActiveQueues.size() + ") " +
          "and priorityActiveQueues (" + activeQueues.size() + ") do not match");
    }

    return true;
  }

  @Override
  public boolean offer(TaskPool fetchQueue) {
    return add(fetchQueue);
  }

  @Override
  public TaskPool poll() {
    TaskPool queue = priorityActiveQueues.poll();
    if (queue != null) {
      activeQueues.remove(queue.getId());
    }

    if(priorityActiveQueues.size() != activeQueues.size()) {
      LOG.error("(Poll)Inconsistent status : size of activeQueues (" + priorityActiveQueues.size() + ") " +
          "and priorityActiveQueues (" + activeQueues.size() + ") do not match");
    }

    return queue;
  }

  @Override
  public TaskPool peek() {
    return priorityActiveQueues.peek();
  }

  @Override
  public boolean remove(Object fetchQueue) {
    if (fetchQueue == null || !(fetchQueue instanceof TaskPool)) {
      return false;
    }

    TaskPool queue = (TaskPool)fetchQueue;
    priorityActiveQueues.remove(queue);
    activeQueues.remove(queue.getId());
    inactiveQueues.remove(queue.getId());

    return true;
  }

  @Override
  public Iterator<TaskPool> iterator() { return priorityActiveQueues.iterator(); }

  @Override
  public int size() { return priorityActiveQueues.size(); }

  @Override
  public boolean isEmpty() { return priorityActiveQueues.isEmpty(); }

  @Override
  public void clear() {
    priorityActiveQueues.clear();
    activeQueues.clear();
    inactiveQueues.clear();
  }

  public void enable(TaskPool queue) {
    queue.enable();

    priorityActiveQueues.add(queue);
    activeQueues.put(queue.getId(), queue);
    inactiveQueues.remove(queue.getId());
  }

  /**
   * Retired queues do not serve any more, but the tasks can be find out and finished.
   * The tasks in detached queues can be find out to finish.
   *
   * A queue should be detached if
   * 1. the queue is too slow, or
   * 2. all tasks are done
   * */
  public void disable(TaskPool queue) {
    priorityActiveQueues.remove(queue);
    activeQueues.remove(queue.getId());

    queue.disable();
    inactiveQueues.put(queue.getId(), queue);
  }

  public boolean hasPriorPendingTasks(int priority) {
    boolean hasPrior = false;
    for (TaskPool queue : priorityActiveQueues) {
      if (queue.getPriority() < priority) {
        break;
      }

      hasPrior = queue.hasPendingTasks();
    }

    final Predicate<TaskPool> p = queue -> queue.getPriority() >= priority && queue.hasPendingTasks();
    return hasPrior || inactiveQueues.values().stream().anyMatch(p);
  }

  public TaskPool find(PoolId id) { return search(id, false); }

  public TaskPool findExtend(PoolId id) { return search(id, true); }

  public TaskPool search(PoolId id, boolean searchInactive) {
    TaskPool queue = null;

    if (id != null) {
      queue = activeQueues.get(id);

      if (queue == null && searchInactive) {
        queue = inactiveQueues.get(id);
      }
    }

    return queue;
  }

  public String getCostReport() {
    StringBuilder sb = new StringBuilder();
    activeQueues.values().stream()
        .sorted(Comparator.comparing(TaskPool::averageTimeCost).reversed())
        .limit(50)
        .forEach(queue -> sb.append(queue.getCostReport()).append('\n'));
    return sb.toString();
  }

  public void dump(int limit) {
    LOG.info("Report fetch queue status. "
        + "Active : " + activeQueues.size() + ", inactive : " + inactiveQueues.size()+ " ...");
    activeQueues.values().stream().limit(limit).filter(TaskPool::hasTasks).forEach(TaskPool::dump);
    inactiveQueues.values().stream().limit(limit).filter(TaskPool::hasPendingTasks).forEach(TaskPool::dump);
  }
}
