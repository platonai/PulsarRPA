package ai.platon.pulsar.common.proxy;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.ObjectCache;
import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manager all proxy servers, for every request, we choose a proxy server from a proxy server list
 */
public class ProxyPool extends AbstractQueue<ProxyEntry> implements AutoCloseable {

    public static final Path CONFIG_DIR = Paths.get(System.getProperty("java.io.tmpdir"),
            "pulsar-" + System.getenv("USER"), "conf");
    public static final Duration RELOAD_PERIOD = Duration.ofMinutes(2);
    protected static final Logger LOG = LoggerFactory.getLogger(ProxyPool.class);
    private static ProxyPool INSTANCE;
    private final ImmutableConfig conf;
    private final Duration pollingWait;
    private final int maxPoolSize;
    private final Map<Path, Long> lastModifiedTimes = new HashMap<>();
    private final Set<ProxyEntry> proxyEntries = new HashSet<>();
    private final BlockingQueue<ProxyEntry> freeProxies = new LinkedBlockingDeque<>();
    private final Set<ProxyEntry> workingProxies = Collections.synchronizedSet(new HashSet<>());
    private final Set<ProxyEntry> unavailableProxies = Collections.synchronizedSet(new HashSet<>());
    private Path availableDir;
    private Path enabledDir;
    private Path archiveDir;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public ProxyPool(ImmutableConfig conf) {
        this.conf = conf;
        this.maxPoolSize = conf.getInt(CapabilityTypes.PROXY_POOL_SIZE, 10000);
        this.pollingWait = conf.getDuration(CapabilityTypes.PROXY_POOL_POLLING_WAIT, Duration.ofSeconds(1));

        String configDir = conf.get(CapabilityTypes.PULSAR_CONFIG_DIR, CONFIG_DIR.toString());
        availableDir = Paths.get(configDir, "proxy", "available-proxies");
        enabledDir = Paths.get(configDir, "proxy", "enabled-proxies");
        archiveDir = Paths.get(configDir, "proxy", "archive");

        loadAll();
    }

    public static ProxyPool getInstance(ImmutableConfig conf) {
        return ObjectCache.get(conf).computeIfAbsent(ProxyPool.class, c -> new ProxyPool(conf));
    }

    @Override
    public boolean contains(Object proxy) {
        return freeProxies.contains(proxy);
    }

    @Override
    public Iterator<ProxyEntry> iterator() {
        return freeProxies.iterator();
    }

    @Override
    public int size() {
        return freeProxies.size();
    }

    @Override
    public boolean offer(ProxyEntry proxyEntry) {
        proxyEntry.refresh();
        return freeProxies.offer(proxyEntry);
    }

    @Override
    public ProxyEntry poll() {
        ProxyEntry proxy = null;

        while (proxy == null && !freeProxies.isEmpty()) {
            proxy = pollOne();
        }

        return proxy;
    }

    @Override
    public ProxyEntry peek() {
        return freeProxies.peek();
    }

    public Path getAvailableDir() {
        return availableDir;
    }

    public Path getEnabledDir() {
        return enabledDir;
    }

    public Path getArchiveDir() {
        return archiveDir;
    }

    /**
     * The proxy may be recovered later
     */
    public void retire(ProxyEntry proxyEntry) {
        unavailableProxies.add(proxyEntry);
    }

    /**
     * Check all unavailable proxies, recover them if possible.
     * This might take a long time, so it should be run in a separate thread
     */
    public int recover() {
        return recover(Integer.MAX_VALUE);
    }

    /**
     * Check n unavailable proxies, recover them if possible.
     * This might take a long time, so it should be run in a separate thread
     */
    public int recover(int n) {
        int recovered = 0;

        Iterator<ProxyEntry> it = unavailableProxies.iterator();
        while (n-- > 0 && it.hasNext()) {
            ProxyEntry proxy = it.next();

            if (proxy.isGone()) {
                it.remove();
            } else if (proxy.testNetwork()) {
                it.remove();
                proxy.refresh();
                offer(proxy);
                ++recovered;
            }
        }

        return recovered;
    }

    // Block until timeout or an available proxy entry returns
    private ProxyEntry pollOne() {
        ProxyEntry proxy = null;
        try {
            proxy = freeProxies.poll(pollingWait.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        if (proxy == null) {
            return null;
        }

        if (proxy.isExpired()) {
            if (proxy.testNetwork()) {
                proxy.refresh();
            }
        }

        if (proxy.isExpired()) {
            unavailableProxies.add(proxy);
            proxy = null;
        } else {
            workingProxies.add(proxy);
        }

        return proxy;
    }

    public void reloadIfModified() {
        try {
            Files.list(enabledDir).filter(path -> Files.isRegularFile(path)).forEach(path -> {
                reloadIfModified(path, lastModifiedTimes.getOrDefault(path, 0L));
            });
        } catch (IOException e) {
            LOG.info(toString());
        }
    }

    private void reloadIfModified(Path path, long lastModified) {
        long modified = path.toFile().lastModified();
        long elapsed = modified - lastModified;

        if (elapsed > RELOAD_PERIOD.toMillis()) {
            LOG.debug("Reload from file, last modified : {}, elapsed : {}s", lastModified, elapsed / 1000.0);
            load(path);
        }

        lastModifiedTimes.put(path, lastModified);
    }

    @Override
    public String toString() {
        return String.format("Total %d, free: %d, working: %d, gone: %d",
                proxyEntries.size(), freeProxies.size(), workingProxies.size(), unavailableProxies.size());
    }

    private void loadAll() {
        try {
            if (!Files.exists(enabledDir)) {
                Files.createDirectories(enabledDir);
            }

            Files.list(enabledDir).filter(path -> Files.isRegularFile(path)).forEach(this::load);

            LOG.info(toString());
        } catch (IOException e) {
            LOG.warn(e.toString());
        }
    }

    private void load(Path path) {
        try {
            List<String> lines = Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#"))
                    .distinct()
                    .collect(Collectors.toList());

            Collections.shuffle(lines);

            lines.stream()
                    .map(ProxyEntry::parse)
                    .filter(Objects::nonNull)
                    .forEach(proxyEntry -> {
                        proxyEntries.add(proxyEntry);
                        freeProxies.add(proxyEntry);
                    });
        } catch (IOException e) {
            LOG.info(toString());
        }
    }

    private void archive(Path path, Collection<ProxyEntry> proxyEntries) {
        String content = proxyEntries.stream().map(ProxyEntry::toString).collect(Collectors.joining("\n"));
        try {
            Files.deleteIfExists(path);
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            LOG.warn(e.toString());
        }
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        String now = DateTimeUtil.now("MMdd.HHmm");

        try {
            final Path currentArchiveDir = Paths.get(archiveDir.toString(), now);
            if (!Files.exists(currentArchiveDir)) {
                Files.createDirectories(currentArchiveDir);
            }

            Map<Collection<ProxyEntry>, String> archiveDestinations = new HashMap<>();
            archiveDestinations.put(proxyEntries, "proxies.all.txt");
            archiveDestinations.put(workingProxies, "proxies.working.txt");
            archiveDestinations.put(freeProxies, "proxies.free.txt");
            archiveDestinations.put(unavailableProxies, "proxies.unavailable.txt");

            archiveDestinations.forEach((container, destination) -> {
                Path path = Paths.get(currentArchiveDir.toString(), destination);
                archive(path, container);
            });
        } catch (IOException e) {
            LOG.warn(e.toString());
        }
    }
}
