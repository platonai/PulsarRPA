package fun.platonic.pulsar.common;

import fun.platonic.pulsar.persist.WebPage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static fun.platonic.pulsar.common.config.PulsarConstants.*;
import static java.util.stream.Collectors.joining;

/**
 * Created by vincent on 17-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class PulsarFiles {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarFiles.class);

    public static Path writeLastGeneratedRows(long rows) throws IOException {
        Path path = PATH_LAST_GENERATED_ROWS;
        Files.write(path, (rows + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        return path;
    }

    public static int readLastGeneratedRows() {
        try {
            String line = Files.readAllLines(PATH_LAST_GENERATED_ROWS).get(0);
            return NumberUtils.toInt(line, -1);
        } catch (Throwable ignored) {
        }

        return -1;
    }

    public static Path writeBatchId(String batchId) throws IOException {
        if (batchId != null && !batchId.isEmpty()) {
            Path path = PATH_LAST_BATCH_ID;
            Files.write(path, (batchId + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return path;
        }

        return null;
    }

    public static String readBatchIdOrDefault(String defaultValue) {
        try {
            return Files.readAllLines(PATH_LAST_BATCH_ID).get(0);
        } catch (Throwable ignored) {
        }

        return defaultValue;
    }

    /**
     * TODO: we need a better name
     */
    public void createSharedFileTask(String url) {
        try {
            ScentPaths paths = ScentPaths.INSTANCE;
            Path path = paths.get(paths.getWebCacheDir().toString(), paths.fromUri(url, ".task"));
            Files.write(path, url.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public String getCachedWebPage(String url) {
        ScentPaths paths = ScentPaths.INSTANCE;
        Path path = paths.get(paths.getWebCacheDir().toString(), paths.fromUri(url, ".htm"));
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

    public void logUnreachableHosts(Collection<String> unreachableHosts) {
        String report = unreachableHosts.stream()
                .map(StringUtil::reverse).sorted().map(StringUtil::reverse)
                .collect(joining("\n"));

        try {
            Files.write(PATH_UNREACHABLE_HOSTS, report.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }
}
