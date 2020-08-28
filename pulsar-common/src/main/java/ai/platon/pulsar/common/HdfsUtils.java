/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * <p>HdfsUtils class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class HdfsUtils {

    /** Constant <code>LOG</code> */
    public static final Logger LOG = LoggerFactory.getLogger(HdfsUtils.class);

    /**
     * Check if running under distributed file system
     *
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean isDistributedFS(ImmutableConfig conf) {
        String fsName = conf.get("fs.defaultFS");
        return fsName != null && fsName.startsWith("hdfs");
    }

    /**
     * Copy from local file to HDFS, overwrite the dest file if exsits
     * The src file is on the local disk.
     *
     * @param file a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void copyFromLocalFile(String file, ImmutableConfig conf) throws IOException {
        if (Files.exists(Paths.get(file))) {
            FileSystem fs = FileSystem.get(conf.unbox());
            org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(file);
            fs.copyFromLocalFile(path, path);
        }
    }

    /**
     * <p>moveFromLocalFile.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void moveFromLocalFile(String file, ImmutableConfig conf) throws IOException {
        if (Files.exists(Paths.get(file))) {
            FileSystem fs = FileSystem.get(conf.unbox());
            org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(file);
            fs.moveFromLocalFile(path, path);
        }
    }

    /**
     * Returns PathFilter that passes all paths through.
     *
     * @return a {@link org.apache.hadoop.fs.PathFilter} object.
     */
    public static PathFilter getPassAllFilter() {
        return arg0 -> true;
    }

    /**
     * Returns PathFilter that passes directories through.
     *
     * @param fs a {@link org.apache.hadoop.fs.FileSystem} object.
     * @return a {@link org.apache.hadoop.fs.PathFilter} object.
     */
    public static PathFilter getPassDirectoriesFilter(FileSystem fs) {
        return path -> {
            try {
                return fs.getFileStatus(path).isDirectory();
            } catch (IOException ioe) {
                return false;
            }
        };
    }

    /**
     * Turns an array of FileStatus into an array of Paths.
     *
     * @param stats an array of {@link org.apache.hadoop.fs.FileStatus} objects.
     * @return an array of {@link org.apache.hadoop.fs.Path} objects.
     */
    public static Path[] getPaths(FileStatus[] stats) {
        if (stats == null) {
            return null;
        }
        if (stats.length == 0) {
            return new Path[0];
        }
        Path[] res = new Path[stats.length];
        for (int i = 0; i < stats.length; i++) {
            res[i] = stats[i].getPath();
        }
        return res;
    }

    /**
     * <p>createIfNotExits.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean createIfNotExits(Path path, ImmutableConfig conf) {
        try {
            FileSystem fs = FileSystem.get(conf.unbox());

            if (fs.exists(path)) {
                // The file exists, someone else have already read it
                return false;
            } else {
                // The lock file does not exist, i will read it
                FSDataOutputStream out = fs.create(path);
                out.close();
                return true;
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return false;
    }

    /**
     * <p>exits.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean exits(String path, ImmutableConfig conf) {
        try {
            return FileSystem.get(conf.unbox()).exists(new Path(path));
        } catch (IOException e) {
            LOG.warn(e.toString());
        }

        return false;
    }

    /**
     * <p>getModifiedTime.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.time.Instant} object.
     */
    public static Instant getModifiedTime(String path, ImmutableConfig conf) {
        try {
            FileSystem fs = FileSystem.get(conf.unbox());
            FileStatus status = fs.getFileStatus(new Path(path));
            return Instant.ofEpochMilli(status.getModificationTime());
        } catch (IOException e) {
            LOG.warn(e.toString());
        }

        return Instant.MIN;
    }

    /**
     * <p>deleteIfExits.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean deleteIfExits(Path path, ImmutableConfig conf) {
        try {
            FileSystem fs = FileSystem.get(conf.unbox());

            if (fs.exists(path)) {
                fs.delete(path, false);
            }
        } catch (IOException e) {
            LOG.warn(e.toString());
        }

        return false;
    }

    /**
     * <p>isLocked.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean isLocked(Path path, String jobName, ImmutableConfig conf) {
        Path lockPath = new Path(path.toString() + "." + jobName + ".lock");

        try (FileSystem fs = FileSystem.get(conf.unbox())) {
            return fs.exists(lockPath);
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return false;
    }

    /**
     * <p>lock.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean lock(String path, String jobName, ImmutableConfig conf) {
        return lock(new Path(path), jobName, conf);
    }

    /**
     * <p>lock.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean lock(Path path, String jobName, ImmutableConfig conf) {
        Path lockPath = new Path(path.toString() + "." + jobName + ".lock");
        createIfNotExits(lockPath, conf);
        return true;
    }

    /**
     * <p>unlock.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean unlock(String path, String jobName, ImmutableConfig conf) {
        return unlock(new Path(path), jobName, conf);
    }

    /**
     * <p>unlock.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a boolean.
     */
    public static boolean unlock(Path path, String jobName, ImmutableConfig conf) {
        Path lockPath = new Path(path.toString() + "." + jobName + ".lock");
        deleteIfExits(lockPath, conf);
        return true;
    }

    /**
     * <p>readAllLines.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param lock a boolean.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLines(String path, String jobName, boolean lock, ImmutableConfig conf) throws IOException {
        return readAllLines(new Path(path), jobName, lock, conf);
    }

    /**
     * <p>readAllLines.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param jobName a {@link java.lang.String} object.
     * @param lock a boolean.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLines(Path path, String jobName, boolean lock, ImmutableConfig conf) throws IOException {
        List<String> lines = Collections.emptyList();

        try {
            if (lock) {
                if (isLocked(path, jobName, conf)) {
                    return lines;
                }

                lock(path, jobName, conf);
            }

            lines = readAllLines(path, jobName, lock, conf);
        } finally {
            if (lock) {
                unlock(path, jobName, conf);
            }
        }

        return lines;
    }

    /**
     * <p>readAllLines.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLines(String path, ImmutableConfig conf) throws IOException {
        return readAllLines(new Path(path), conf);
    }

    /**
     * <p>readAllLines.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLines(Path path, ImmutableConfig conf) throws IOException {
        return IOUtils.readLines(FileSystem.get(conf.unbox()).open(path));
    }

    /**
     * <p>readAllLinesAndDelete.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param lock a boolean.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLinesAndDelete(String path, String jobName, boolean lock, ImmutableConfig conf) throws IOException {
        return readAllLinesAndDelete(new Path(path), jobName, lock, conf);
    }

    /**
     * <p>readAllLinesAndDelete.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param jobName a {@link java.lang.String} object.
     * @param lock a boolean.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    public static List<String> readAllLinesAndDelete(Path path, String jobName, boolean lock, ImmutableConfig conf) throws IOException {
        List<String> lines = Collections.emptyList();

        try {
            if (lock) {
                if (isLocked(path, jobName, conf)) {
                    return lines;
                }

                lock(path, jobName, conf);
            }

            lines = readAllLines(path, jobName, lock, conf);
            FileSystem.get(conf.unbox()).delete(path, false);
        } finally {
            if (lock) {
                unlock(path, jobName, conf);
            }
        }

        return lines;
    }

    /**
     * <p>write.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void write(String path, String content, ImmutableConfig conf) throws IOException {
        write(new Path(path), content, conf);
    }

    /**
     * <p>writeLine.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void writeLine(String path, String content, ImmutableConfig conf) throws IOException {
        write(new Path(path), content + "\n", conf);
    }

    /**
     * <p>write.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param content a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void write(Path path, String content, ImmutableConfig conf) throws IOException {
        FSDataOutputStream output = FileSystem.get(conf.unbox()).append(path);
        IOUtils.write(content, output);
    }

    /**
     * <p>writeAndUnlock.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void writeAndUnlock(String path, String content, String jobName, ImmutableConfig conf) throws IOException {
        writeAndUnlock(new Path(path), content, jobName, conf);
    }

    /**
     * <p>writeAndUnlock.</p>
     *
     * @param path a {@link org.apache.hadoop.fs.Path} object.
     * @param content a {@link java.lang.String} object.
     * @param jobName a {@link java.lang.String} object.
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     * @throws java.io.IOException if any.
     */
    public static void writeAndUnlock(Path path, String content, String jobName, ImmutableConfig conf) throws IOException {
        write(path, content, conf);
        unlock(path, jobName, conf);
    }
}
