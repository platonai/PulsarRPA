package fun.platonic.pulsar.common.proxy;

import fun.platonic.pulsar.common.NetUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_HOST;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_PORT;

public class ProxyUpdateThread extends Thread {

    protected static final Logger LOG = LoggerFactory.getLogger(ProxyUpdateThread.class);
    private final ImmutableConfig conf;
    protected long updatePeriod = 10 * 1000;
    private ProxyPool proxyPool;

    public ProxyUpdateThread(ImmutableConfig conf) {
        this.conf = conf;
        this.proxyPool = ProxyPool.getInstance(conf);
        this.updatePeriod = conf.getInt("http.proxy.pool.update.period", 10 * 1000);
        this.setDaemon(true);
    }

    public static void main(String[] args) {
        ImmutableConfig conf = new ImmutableConfig();

        ProxyUpdateThread updateThread = new ProxyUpdateThread(conf);
        updateThread.start();

        ProxyPool proxyPool = ProxyPool.getInstance(conf);

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
        String host = conf.get(PULSAR_MASTER_HOST, "localhost");
        int port = conf.getInt(PULSAR_MASTER_PORT, 8081);
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
}
