package fun.platonic.pulsar.common;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.platonic.pulsar.persist.WebPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Collection;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static java.util.stream.Collectors.joining;

/**
 * Created by vincent on 17-3-23.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class PulsarFileSystem {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarFileSystem.class);

    public static final Path CONFIG_DIR = Paths.get(System.getProperty("java.io.tmpdir"),
            "pulsar-" + System.getenv("USER"), "conf");

    private final ImmutableConfig conf;
    private final DecimalFormat df = new DecimalFormat("0.0");
    private String reportSuffix;
    private Path configDir;
    private Path tmpDir;
    private Path cacheDir;
    private Path webCacheDir;
    private Path reportDir;
    private Path unreachableHostsPath;

    public PulsarFileSystem(ImmutableConfig conf) {
        this.conf = conf;
        this.configDir = conf.getPath(PULSAR_CONFIG_DIR, CONFIG_DIR);

        try {
            reportSuffix = conf.get(PULSAR_JOB_NAME, "job-unknown-" + DateTimeUtil.now("MMdd.HHmm"));

            tmpDir = conf.getPath(PULSAR_TMP_DIR, Paths.get(PulsarConstants.PATH_PULSAR_TMP_DIR));
            if (!Files.exists(tmpDir)) {
                Files.createDirectories(tmpDir);
            }

            cacheDir = Paths.get(tmpDir.toString(), "cache");
            if (!Files.exists(cacheDir)) {
                Files.createDirectory(cacheDir);
            }

            webCacheDir = Paths.get(cacheDir.toString(), "web");
            if (!Files.exists(webCacheDir)) {
                Files.createDirectory(webCacheDir);
            }

            reportDir = conf.getPath(PULSAR_REPORT_DIR, Paths.get(PulsarConstants.PATH_PULSAR_REPORT_DIR));
            reportDir = Paths.get(reportDir.toAbsolutePath().toString(), DateTimeUtil.format(System.currentTimeMillis(), "yyyyMMdd"));
            Files.createDirectories(reportDir);

            unreachableHostsPath = Paths.get(reportDir.toAbsolutePath().toString(), PulsarConstants.FILE_UNREACHABLE_HOSTS);
            Files.createDirectories(unreachableHostsPath.getParent());

            if (!Files.exists(unreachableHostsPath)) {
                Files.createFile(unreachableHostsPath);
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public static Path writeLastGeneratedRows(long rows) throws IOException {
        Path path = Paths.get(PulsarConstants.PATH_LAST_GENERATED_ROWS);
        Files.write(path, (rows + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        return path;
    }

    public static int readLastGeneratedRows() {
        try {
            String line = Files.readAllLines(Paths.get(PulsarConstants.PATH_LAST_GENERATED_ROWS)).get(0);
            return NumberUtils.toInt(line, -1);
        } catch (Throwable ignored) {
        }

        return -1;
    }

    public static Path writeBatchId(String batchId) throws IOException {
        if (batchId != null && !batchId.isEmpty()) {
            Path path = Paths.get(PulsarConstants.PATH_LAST_BATCH_ID);
            Files.write(path, (batchId + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return path;
        }

        return null;
    }

    public static String readBatchIdOrDefault(String defaultValue) {
        try {
            return Files.readAllLines(Paths.get(PulsarConstants.PATH_LAST_BATCH_ID)).get(0);
        } catch (Throwable ignored) {
        }

        return defaultValue;
    }

    public Path getTmpDir() {
        return tmpDir;
    }

    public Path getTmpPath(String... paths) {
        return Paths.get(tmpDir.toString(), paths);
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public Path getWebCacheDir() {
        return webCacheDir;
    }

    public Path getUnreachableHostsPath() {
        return unreachableHostsPath;
    }

    /**
     * TODO: we need a better name
     */
    public void createSharedFileTask(String url) {
        try {
            Path path = Paths.get(webCacheDir.toString(), encode(url) + ".task");
            Files.write(path, url.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public String save(WebPage page) {
        return save(page.getUrl(), page.getContentAsString());
    }

    public String save(WebPage page, String postfix) {
        return save(page.getUrl(), postfix, page.getContentAsString());
    }

    public String save(String url, String postfix, byte[] content) {
        Path path = Paths.get(webCacheDir.toString(), encode(url) + postfix);
        return saveAs(content, path);
    }

    public String save(String url, String postfix, String content) {
        Path path = Paths.get(webCacheDir.toString(), encode(url) + postfix);
        return saveAs(content, path);
    }

    public String save(String url, String content) {
        Path path = Paths.get(webCacheDir.toString(), encode(url) + ".html");
        return saveAs(content, path);
    }

    public String saveAs(String content, Path path) {
        return saveAs(content.getBytes(), path);
    }

    public String saveAs(byte[] content, Path path) {
        try {
            Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return path.toString();
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return "/dev/null";
    }

    public String getCachedWebPage(String url) {
        Path path = Paths.get(webCacheDir.toString(), encode(url) + ".html");
        if (Files.notExists(path)) {
            return null;
        }

        try {
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return null;
    }

    public String encode(String url) {
        return DigestUtils.md5Hex(url);
    }

    public void logUnreachableHosts(Collection<String> unreachableHosts) {
        String report = unreachableHosts.stream()
                .map(StringUtil::reverse).sorted().map(StringUtil::reverse)
                .collect(joining("\n"));

        try {
            Files.write(unreachableHostsPath, report.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }
}
