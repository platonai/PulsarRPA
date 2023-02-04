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
package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Load resources
 */
object ResourceLoader {
    private val logger = LoggerFactory.getLogger(ResourceLoader::class.java)
    private val lastModifiedTimes = ConcurrentHashMap<Path, Instant>()
    private val userClassFactories = ConcurrentLinkedDeque<ClassFactory>()
    private val classLoader = Thread.currentThread().contextClassLoader ?: ResourceLoader::class.java.classLoader

    var lineFilter: (line: String) -> Boolean = { line ->
        !line.startsWith("# ") && !line.startsWith("-- ") && line.isNotBlank()
    }

    /**
     * Add a class factory in order to manage more than one class loader.
     *
     * @param classFactory An object that implements ClassFactory
     */
    fun addClassFactory(classFactory: ClassFactory) {
        userClassFactories.add(classFactory)
    }

    /**
     * Remove a class factory
     *
     * @param classFactory Already inserted class factory instance
     */
    fun removeClassFactory(classFactory: ClassFactory) {
        userClassFactories.remove(classFactory)
    }

    /**
     * Load a class, but check if it is allowed to load this class first. To
     * perform access rights checking, the system property h2.allowedClasses
     * needs to be set to a list of class file name prefixes.
     *
     * @param className the name of the class
     * @return the class object
     */
    @Throws(ClassNotFoundException::class)
    fun <Z> loadUserClass(className: String): Class<Z> {
        // Use provided class factory first.
        for (classFactory in userClassFactories) {
            if (classFactory.match(className)) {
                try {
                    val userClass = classFactory.loadClass(className)
                    if (userClass != null) {
                        return userClass as Class<Z>
                    }
                } catch (e: ClassNotFoundException) { // ignore, try other class loaders
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        // Use local ClassLoader
        return try {
            Class.forName(className) as Class<Z>
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName(className, true, Thread.currentThread().contextClassLoader) as Class<Z>
            } catch (e2: Exception) {
                throw e2
            }
        } catch (e: Error) {
            throw e
        }
    }

    /**
     * Read all lines from one of the following resource: string, file by file name and resource by resource name
     * The front resource have higher priority
     */
    @JvmOverloads
    fun readAllLines(
        stringResource: String?, fileResource: String, resourcePrefix: String = "", filter: Boolean = true
    ): List<String> {
        return getMultiSourceReader(stringResource, fileResource, resourcePrefix)?.useLines { seq ->
            if (filter) {
                seq.filter(lineFilter).toList()
            } else {
                seq.toList()
            }
        } ?: listOf()
    }

    fun readAllLines(fileResource: String) = readAllLines(fileResource, true)

    fun readAllLines(fileResource: String, filter: Boolean): List<String> {
        if (!filter) {
            return readAllLinesNoFilter(fileResource)
        }

        return getResourceAsReader(fileResource)?.useLines {
            it.filter(lineFilter).toList()
        } ?: listOf()
    }

    fun readAllLinesNoFilter(fileResource: String): List<String> {
        return getResourceAsReader(fileResource)?.useLines {
            it.toList()
        } ?: listOf()
    }

    fun readAllLinesIfModified(path: Path): List<String> {
        val lastModified = lastModifiedTimes.getOrDefault(path, Instant.EPOCH)
        val modified = Files.getLastModifiedTime(path).toInstant()

        return takeIf { modified > lastModified }
                ?.let { Files.readAllLines(path).also { lastModifiedTimes[path] = modified } }
                ?: listOf()
    }

    fun readString(fileResource: String): String {
        return readStringTo(fileResource, StringBuilder()).toString()
    }

    fun readStringTo(fileResource: String, sb: StringBuilder): StringBuilder {
        getResourceAsReader(fileResource)?.forEachLine {
            sb.appendln(it)
        }
        return sb
    }

    /**
     * Get a [Reader] attached to the configuration resource with the
     * given `name`.
     *
     * @param name resource name.
     * @return a reader attached to the resource.
     */
    fun getResourceAsStream(name: String): InputStream? {
        return try {
            val url = getResource(name) ?: return null
            if (logger.isDebugEnabled) {
                logger.debug("Find resource $name | $url")
            }
            url.openStream()
        } catch (e: IOException) {
            logger.warn("Failed to read resource {} | {}", name, e.message)
            null
        }
    }

    /**
     * Find the first resource associated by prefix/name
     */
    fun getResourceAsStream(name: String, vararg resourcePrefixes: String): InputStream? {
        var found = false
        return resourcePrefixes.asIterable().filter { it.isNotBlank() }
                .mapNotNull { if (!found) getResourceAsStream("$it/$name") else null }
                .onEach { found = true }
                .firstOrNull() ?: getResourceAsStream(name)
    }

    /**
     * Get a [Reader] attached to the configuration resource with the
     * given `name`.
     *
     * @param fileResource configuration resource name.
     * @return a reader attached to the resource.
     */
    fun getResourceAsReader(fileResource: String, vararg resourcePrefixes: String): Reader? {
        return getResourceAsStream(fileResource, *resourcePrefixes)?.let { InputStreamReader(it) }
    }

    fun exists(name: String) = getResource(name) != null

    /**
     * Get the [URL] for the named resource.
     *
     * Finds a resource with a given name.
     * Find resources first by each registered class loader and then by the default class loader.
     *
     * @see Class.getResource
     * @param  name name of the desired resource
     * @return      A  [java.net.URL] object or `null` if no
     * resource with this name is found
     */
    fun getResource(name: String): URL? {
        var url: URL? = null
        // User provided class loader first
        val it: Iterator<ClassFactory> = userClassFactories.iterator()
        while (url == null && it.hasNext()) {
            url = it.next().javaClass.getResource(name)
        }
        return url ?: classLoader.getResource(name)
    }

    /**
     * Get the [URL] for the named resource.
     *
     * @param name resource name.
     * @param preferredClassLoader preferred class loader, this class loader is used first,
     * fallback to other class loaders if the resource not found by preferred class loader.
     * @return the url for the named resource.
     */
    fun <T> getResource(name: String, preferredClassLoader: Class<T>): URL? {
        return preferredClassLoader.getResource(name) ?: getResource(name)
    }

    @Throws(FileNotFoundException::class)
    fun getMultiSourceReader(stringResource: String?, fileResource: String): Reader? {
        return getMultiSourceReader(stringResource, fileResource, "")
    }

    @Throws(FileNotFoundException::class)
    fun getMultiSourceReader(stringResource: String?, namedResource: String, resourcePrefix: String): Reader? {
        var reader: Reader? = null
        if (!stringResource.isNullOrBlank()) {
            reader = StringReader(stringResource)
        } else {
            if (Files.exists(Paths.get(namedResource))) {
                reader = FileReader(namedResource)
            } else { // Read specified location
                if (!namedResource.startsWith("/") && resourcePrefix.isNotBlank()) {
                    reader = getResourceAsReader("$resourcePrefix/$namedResource")
                }
                // Search in classpath
                if (reader == null) {
                    reader = getResourceAsReader(namedResource)
                }
            }
        }
        return reader
    }

    /**
     * The utility methods will try to use the provided class factories to
     * convert binary name of class to Class object. Used by H2 OSGi Activator
     * in order to provide a class from another bundle ClassLoader.
     */
    interface ClassFactory {
        /**
         * Check whether the factory can return the named class.
         *
         * @param name the binary name of the class
         * @return true if this factory can return a valid class for the provided class name
         */
        fun match(name: String): Boolean

        /**
         * Load the class.
         *
         * @param name the binary name of the class
         * @return the class object
         * @throws ClassNotFoundException If the class is not handle by this factory
         */
        @Throws(ClassNotFoundException::class)
        fun loadClass(name: String): Class<*>?
    }
}
