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
package org.warps.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for common filesystem operations.
 * TODO: register class loaders
 */
public class ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceLoader.class);

    private ClassLoader classLoader;

    public ResourceLoader() {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ResourceLoader.class.getClassLoader();
        }
    }

    public List<String> readAllLines(String stringResource, String fileResource, String resourcePrefix) {
        try (Reader reader = getReader(stringResource, fileResource, resourcePrefix)) {
            return new BufferedReader(reader).lines()
                    .filter(l -> !l.startsWith("#") && StringUtils.isNotBlank(l))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error(StringUtil.stringifyException(e));
        }

        return new ArrayList<>(0);
    }

    public List<String> readAllLines(String stringResource, String fileResource) {
        return readAllLines(stringResource, fileResource, "");
    }

    public List<String> readAllLines(String fileResource) {
        return readAllLines(null, fileResource, "");
    }

    public Reader getReader(String stringResource, String fileResource) throws FileNotFoundException {
        return getReader(stringResource, fileResource, "");
    }

    public Reader getReader(String stringResource, String fileResource, String resourcePrefix) throws FileNotFoundException {
        Reader reader = null;
        if (!StringUtils.isBlank(stringResource)) {
            reader = new StringReader(stringResource);
        } else {
            if (Files.exists(Paths.get(fileResource))) {
                reader = new FileReader(fileResource);
            } else {
                // Read specified location
                if (!fileResource.startsWith("/") && StringUtils.isNotBlank(resourcePrefix)) {
                    reader = getResourceAsReader(resourcePrefix + "/" + fileResource);
                }

                // Read default config dir
//        if (reader == null) {
//          reader = getResourceAsReader("conf/" + fileResource);
//        }

                // Search in classpath
                if (reader == null) {
                    reader = getResourceAsReader(fileResource);
                }
            }
        }

        return reader;
    }

    public Reader getResourceAsReader(String fileResource, String... resourcePrefixes) {
        final Reader[] reader = {null};
        return Stream.of(resourcePrefixes)
                .filter(StringUtils::isNotBlank)
                .map(resourcePrefix -> {
                    if (reader[0] == null) reader[0] = getResourceAsReader(resourcePrefix + "/" + fileResource);
                    return reader[0];
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(getResourceAsReader(fileResource));
    }

    /**
     * Get a {@link Reader} attached to the configuration resource with the
     * given <code>name</code>.
     *
     * @param name configuration resource name.
     * @return a reader attached to the resource.
     */
    public Reader getResourceAsReader(String name) {
        try {
            URL url = getResource(name);

            if (url == null) {
                // LOG.info(name + " not found");
                return null;
            } else {
                LOG.info("Found resource " + name + " at " + url);
            }

            return new InputStreamReader(url.openStream());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the {@link URL} for the named resource.
     *
     * @param name resource name.
     * @return the url for the named resource.
     */
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }
}
