package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class TaskSchedulers implements AutoCloseable {

  public static final Logger LOG = LoggerFactory.getLogger(TaskSchedulers.class);

  private final String name;
  private Map<Integer, TaskScheduler> fetchSchedulers = Maps.newTreeMap();
  private Queue<Integer> fetchSchedulerIds = Lists.newLinkedList();

  public TaskSchedulers(ImmutableConfig conf) {
    this.name = this.getClass().getSimpleName() + "-" + DateTimeUtil.now("d.Hms");

    LOG.info("Initialize " + name);
  }

  public TaskSchedulers(List<TaskScheduler> taskSchedulers, ImmutableConfig conf) {
    this(conf);
    taskSchedulers.forEach(t -> put(t.getId(), t));
  }

  public synchronized String name() {
    return this.name;
  }

  public synchronized void put(int id, TaskScheduler taskScheduler) {
    fetchSchedulers.put(id, taskScheduler);
    fetchSchedulerIds.add(id);

    LOG.info("Add task scheduler #" + id);
    LOG.info("status : " + __toString());
  }

  public synchronized List<Integer> schedulerIds() {
    return new ArrayList<>(fetchSchedulerIds);
  }

  public synchronized TaskScheduler get(int id) {
    return fetchSchedulers.get(id);
  }

  public synchronized TaskScheduler getFirst() {
    return fetchSchedulers.values().iterator().next();
  }

  public synchronized TaskScheduler peek() {
    Integer id = fetchSchedulerIds.peek();
    if (id == null) {
      return null;
    }

    return fetchSchedulers.get(id);
  }

  public synchronized void remove(int id) {
    fetchSchedulerIds.remove(id);
    fetchSchedulers.remove(id);

    LOG.info("Remove FetchScheduler #" + id + " from pool #" + name());
    LOG.info("status : " + __toString());
  }

  public synchronized void remove(TaskScheduler taskScheduler) {
    if (taskScheduler == null) {
      return;
    }
    remove(taskScheduler.getId());
  }

  public synchronized void clear() {
    fetchSchedulerIds.clear();
    fetchSchedulers.clear();
  }

  /**
   * Random get @param count fetch items from an iterative selected job
   * */
  public synchronized List<FetchTask.Key> randomFetchItems(int count) {
    List<FetchTask.Key> keys = Lists.newArrayList();

    Integer id = fetchSchedulerIds.poll();
    if (id == null) {
      // log.debug("No running fetcher job");
      return Lists.newArrayList();
    }

    try {
      TaskScheduler taskScheduler = fetchSchedulers.get(id);
      if (taskScheduler == null) {
        LOG.error("Failed to find out the fetch scheduler with id #" + id);

        remove(id);
        return Lists.newArrayList();
      }

      for (FetchTask item : taskScheduler.schedule(count)) {
        keys.add(item.getKey());
      }
    }
    catch (Throwable e) {
      LOG.error(e.toString());
    }

    fetchSchedulerIds.add(id); // put back to the queue

    return keys;
  }

  @Override
  public synchronized String toString() {
    return __toString();
  }

  private String __toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Job IDs : ")
        .append(StringUtils.join(fetchSchedulerIds, ", "))
        .append("\tQueue Size : ")
        .append(fetchSchedulers.size());

    return sb.toString();
  }

  @Override
  public void close() throws Exception {
    LOG.info("[Destruction] Closing TaskSchedulers");

    fetchSchedulerIds.clear();
    fetchSchedulers.clear();
  }
}
