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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load resources
 */
public class ResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(ResourceLoader.class);

    /**
     * The utility methods will try to use the provided class factories to
     * convert binary name of class to Class object. Used by H2 OSGi Activator
     * in order to provide a class from another bundle ClassLoader.
     */
    public interface ClassFactory {

        /**
         * Check whether the factory can return the named class.
         *
         * @param name the binary name of the class
         * @return true if this factory can return a valid class for the provided class name
         */
        boolean match(String name);

        /**
         * Load the class.
         *
         * @param name the binary name of the class
         * @return the class object
         * @throws ClassNotFoundException If the class is not handle by this factory
         */
        Class<?> loadClass(String name) throws ClassNotFoundException;
    }

    private static List<ClassFactory> userClassFactories = Collections.synchronizedList(new ArrayList<>());

    /**
     * Add a class factory in order to manage more than one class loader.
     *
     * @param classFactory An object that implements ClassFactory
     */
    public static void addClassFactory(ClassFactory classFactory) {
        userClassFactories.add(classFactory);
    }

    /**
     * Remove a class factory
     *
     * @param classFactory Already inserted class factory instance
     */
    public static void removeClassFactory(ClassFactory classFactory) {
        userClassFactories.remove(classFactory);
    }

    private static List<ClassFactory> getUserClassFactories() {
        return userClassFactories;
    }

    private static ClassLoader classLoader;
    static {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ResourceLoader.class.getClassLoader();
        }
    }

    // private static Map<String, byte[]> loadedResources = Collections.synchronizedMap(new HashMap<>());

    public ResourceLoader() {
    }

    /**
     * Load a class, but check if it is allowed to load this class first. To
     * perform access rights checking, the system property h2.allowedClasses
     * needs to be set to a list of class file name prefixes.
     *
     * @param className the name of the class
     * @return the class object
     */
    @SuppressWarnings("unchecked")
    public static <Z> Class<Z> loadUserClass(String className) throws ClassNotFoundException {
        // Use provided class factory first.
        for (ClassFactory classFactory : getUserClassFactories()) {
            if (classFactory.match(className)) {
                try {
                    Class<?> userClass = classFactory.loadClass(className);
                    if (userClass != null) {
                        return (Class<Z>) userClass;
                    }
                } catch (ClassNotFoundException e) {
                    // ignore, try other class loaders
                } catch (Exception e) {
                    throw e;
                }
            }
        }

        // Use local ClassLoader
        try {
            return (Class<Z>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            try {
                return (Class<Z>) Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            } catch (Exception e2) {
                throw e2;
            }
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Read all lines from one of the following resource: string, file by file name and resource by resource name
     * The front resource have higher priority
     * */
    public static List<String> readAllLines(String stringResource, String fileResource, String resourcePrefix) {
        try (Reader reader = getMultiSourceReader(stringResource, fileResource, resourcePrefix)) {
            if (reader != null) {
                return new BufferedReader(reader).lines()
                        .filter(l -> !l.startsWith("#") && StringUtils.isNotBlank(l))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error(Strings.stringifyException(e));
        }

        return new ArrayList<>(0);
    }

    public static List<String> readAllLines(String stringResource, String fileResource) {
        return readAllLines(stringResource, fileResource, "");
    }

    public static List<String> readAllLines(String fileResource) {
        try (Reader reader = getResourceAsReader(fileResource)) {
            if (reader != null) {
                return new BufferedReader(reader).lines()
                        .filter(l -> !l.startsWith("#") && StringUtils.isNotBlank(l))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error(Strings.stringifyException(e));
        }

        return new ArrayList<>(0);
    }

    public static String readString(String fileResource) {
        return readStringTo(fileResource, new StringBuilder());
    }

    public static String readStringTo(String fileResource, StringBuilder sb) {
        try (Reader reader = getResourceAsReader(fileResource)) {
            if (reader != null) {
                new BufferedReader(reader).lines().forEach(sb::append);
            }
        } catch (IOException e) {
            log.error(Strings.stringifyException(e));
        }

        return sb.toString();
    }

    /**
     * Get a {@link Reader} attached to the configuration resource with the
     * given <code>name</code>.
     *
     * @param name resource name.
     * @return a reader attached to the resource.
     */
    @Nullable
    public static InputStream getResourceAsStream(String name) {
        Objects.requireNonNull(name);
        try {
            URL url = getResource(name);

            if (url == null) {
                // LOG.info(name + " not found");
                return null;
            } else {
                log.info("Find resource " + name + " at " + url);
            }

            return url.openStream();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find the first resource associated by prefix/name
     * */
    @Nullable
    public static InputStream getResourceAsStream(String name, String... resourcePrefixes) {
        Objects.requireNonNull(name);
        InputStream[] streams = {null};
        return Stream.of(resourcePrefixes)
                .filter(StringUtils::isNotBlank)
                .map(resourcePrefix -> {
                    if (streams[0] == null) {
                        streams[0] = getResourceAsStream(resourcePrefix + "/" + name);
                    }
                    return streams[0];
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(getResourceAsStream(name));
    }

    /**
     * Get a {@link Reader} attached to the configuration resource with the
     * given <code>name</code>.
     *
     * @param fileResource configuration resource name.
     * @return a reader attached to the resource.
     */
    @Nullable
    public static Reader getResourceAsReader(String fileResource, String... resourcePrefixes) {
        Objects.requireNonNull(fileResource);
        InputStream stream = getResourceAsStream(fileResource, resourcePrefixes);
        return stream == null ? null : new InputStreamReader(stream);
    }

    /**
     * Get the {@link URL} for the named resource.
     *
     * Finds a resource with a given name.
     * Find resources first by each registered class loader and then by the default class loader.
     *
     * @see Class#getResource(java.lang.String)
     *
     * @param  name name of the desired resource
     * @return      A  {@link java.net.URL} object or {@code null} if no
     *              resource with this name is found
     */
    public static URL getResource(String name) {
        URL url = null;
        // User provided class loader first
        Iterator<ClassFactory> it = userClassFactories.iterator();
        while (url == null && it.hasNext()) {
            url = it.next().getClass().getResource(name);
        }
        return url != null ? url : classLoader.getResource(name);
    }

    /**
     * Get the {@link URL} for the named resource.
     *
     * @param name resource name.
     * @param preferredClassLoader preferred class loader, this class loader is used first,
     *                             fallback to other class loaders if the resource not found by preferred class loader.
     * @return the url for the named resource.
     */
    public static <T> URL getResource(String name, Class<T> preferredClassLoader) {
        URL url = preferredClassLoader.getResource(name);
        return url != null ? url : getResource(name);
    }

    public static Reader getMultiSourceReader(String stringResource, String fileResource) throws FileNotFoundException {
        return getMultiSourceReader(stringResource, fileResource, "");
    }

    public static Reader getMultiSourceReader(String stringResource, String namedResource, String resourcePrefix)
            throws FileNotFoundException {
        Reader reader = null;
        if (!StringUtils.isBlank(stringResource)) {
            reader = new StringReader(stringResource);
        } else {
            if (Files.exists(Paths.get(namedResource))) {
                reader = new FileReader(namedResource);
            } else {
                // Read specified location
                if (!namedResource.startsWith("/") && StringUtils.isNotBlank(resourcePrefix)) {
                    reader = getResourceAsReader(resourcePrefix + "/" + namedResource);
                }

                // Search in classpath
                if (reader == null) {
                    reader = getResourceAsReader(namedResource);
                }
            }
        }

        return reader;
    }
}
