package fun.platonic.pulsar.common;

import com.google.common.collect.Lists;
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
import java.util.Collection;

import static fun.platonic.pulsar.common.PulsarConstants.*;
import static java.util.stream.Collectors.joining;

/**
 * Created by vincent on 17-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class PulsarPaths {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarPaths.class);

    private Path tmpDir = PATH_PULSAR_TMP_DIR;
    private Path cacheDir = PATH_PULSAR_CACHE_DIR;
    private Path webCacheDir = Paths.get(PATH_PULSAR_CACHE_DIR.toString(), "web");

    private static PulsarPaths INSTANCE;

    public static PulsarPaths getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PulsarPaths();
        }
        return INSTANCE;
    }

    public PulsarPaths() {
        try {
            if (!Files.exists(tmpDir)) Files.createDirectories(tmpDir);
            if (!Files.exists(cacheDir)) Files.createDirectory(cacheDir);
            if (!Files.exists(webCacheDir)) Files.createDirectory(webCacheDir);
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public Path getTmpDir() {
        return tmpDir;
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public Path getWebCacheDir() {
        return webCacheDir;
    }

    public Path get(Path first, String... more) {
        String[] paths = Lists.asList(first.toString(), more).toArray(new String[0]);
        return Paths.get(tmpDir.toString(), paths);
    }

    public Path get(String first, String... more) {
        String[] paths = Lists.asList(first, more).toArray(new String[0]);
        return Paths.get(tmpDir.toString(), paths);
    }

    public String fromUri(String url) {
        return DigestUtils.md5Hex(url);
    }

    public String fromUri(String url, String suffix) {
        return DigestUtils.md5Hex(url) + suffix;
    }
}
