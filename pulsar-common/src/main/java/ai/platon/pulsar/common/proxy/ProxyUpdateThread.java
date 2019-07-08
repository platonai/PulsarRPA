package ai.platon.pulsar.common.proxy;

import ai.platon.pulsar.common.NetUtil;
import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ProxyUpdateThread extends Thread {

    protected static final Logger LOG = LoggerFactory.getLogger(ProxyUpdateThread.class);
    private final ImmutableConfig conf;
    protected long updatePeriod;
    private ProxyPool proxyPool;

    public ProxyUpdateThread(ProxyPool proxyPool, ImmutableConfig conf) {
        this.conf = conf;
        this.proxyPool = proxyPool;
        this.updatePeriod = conf.getInt("http.proxy.pool.update.period", 10 * 1000);
        this.setDaemon(true);
    }

    @Override
    public void run() {
        if (proxyPool == null) {
            LOG.error("proxy manager must not be null");
            return;
        }

        try {
            int tick = 0;
            while (true) {
                if (tick % 20 == 0) {
                    LOG.debug("updating proxy pool...");
                }

                if (tick % 20 == 0) {
                    updateProxyFromMaster();
                }

                long start = System.currentTimeMillis();
                proxyPool.recover(100);
                long elapsed = System.currentTimeMillis() - start;

                // too often, enlarge review period
                if (elapsed > updatePeriod) {
                    LOG.info("it costs {} millis to check all retired proxy servers, enlarge the check interval", elapsed);
                    updatePeriod = elapsed * 10;
                }

                proxyPool.reloadIfModified();
                Thread.sleep(updatePeriod);

                ++tick;
            }
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
    }

    private void updateProxyFromMaster() {
        String host = conf.get(CapabilityTypes.PULSAR_MASTER_HOST, "localhost");
        int port = conf.getInt(CapabilityTypes.PULSAR_MASTER_PORT, 8081);
        String url = "http://" + host + ":" + port + "/proxy/download";

        try {
            String hostname = NetUtil.getHostname();

            // Update only if this is not the master
            if (!host.equals(hostname)) {
                String file = "synced.proxies.txt";
                Path target = Paths.get(proxyPool.getArchiveDir().toString(), file);
                String cmd = "wget " + url + " -O " + target.toString() + " > /dev/null 2>&1";
                Process p = Runtime.getRuntime().exec(cmd);

                Path link = Paths.get(proxyPool.getEnabledDir().toString(), file);
                Files.createSymbolicLink(link, target);
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public static void main(String[] args) {
        ImmutableConfig conf = new ImmutableConfig();

        ProxyPool proxyPool = new ProxyPool(conf);
        ProxyUpdateThread updateThread = new ProxyUpdateThread(proxyPool, conf);
        updateThread.start();

        while (!proxyPool.isEmpty()) {
            ProxyEntry proxy = proxyPool.poll();

            if (proxy.testNetwork()) {
                LOG.debug("proxy : {} is available", proxy);
            } else {
                LOG.debug("proxy : {} is not available", proxy);
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
