/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import org.junit.Test
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestRuntimes {
    @Test
    fun testEnv() {
        println(System.getenv("USER"))
    }

    @Test
    fun testCheckIfProcessRunning() {
        val running = Runtimes.checkIfProcessRunning("java")
        assertTrue { running }
    }

    @Test
    fun testLocateBinary() {
        if (SystemUtils.IS_OS_LINUX) {
            val locations = Runtimes.locateBinary("dir")
            assertTrue { locations.isNotEmpty() }
        }
    }

    @Test
    fun testDeleteBrokenSymbolicLinksUsingBash() {
        if (SystemUtils.IS_OS_WINDOWS) {
            System.err.println("Files.createSymbolicLink failed on Windows")
            return
        }
        
        val tmp = AppPaths.getTmp("test")
        val file = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.createDirectories(file.parent)
        Files.writeString(file, "to be deleted")
        val symbolicPath = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.createSymbolicLink(symbolicPath, file)

        assertTrue { Files.exists(file) }
        assertTrue { Files.exists(symbolicPath) }

        Files.delete(file)
        assertFalse { Files.exists(file) }
        assertFalse { Files.exists(symbolicPath) }
        assertTrue { Files.isSymbolicLink(symbolicPath) }

        Runtimes.deleteBrokenSymbolicLinks(tmp)
        if (SystemUtils.IS_OS_WINDOWS) {
            // TODO: what happens on windows
        } else {
            assertFalse { Files.isSymbolicLink(symbolicPath) }
        }
    }

    @Test
    fun testDeleteBrokenSymbolicLinksUsingJava() {
        val tmpDir = AppPaths.getTmp("test")
        val file = tmpDir.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.createDirectories(file.parent)
        Files.writeString(file, "to be deleted")
        val symbolicPath = tmpDir.resolve(RandomStringUtils.randomAlphabetic(5))
        AppFiles.createSymbolicLink(symbolicPath, file)

        assertTrue { Files.exists(file) }
        assertTrue { Files.exists(symbolicPath) }

        Files.delete(file)
        assertFalse { Files.exists(file) }
        if (AppFiles.supportSymbolicLink(symbolicPath)) {
            assertFalse { Files.exists(symbolicPath) }
            assertTrue { Files.isSymbolicLink(symbolicPath) }
        }

        Files.list(tmpDir).filter { Files.isSymbolicLink(it) && !Files.exists(it) }.forEach { Files.delete(it) }

        assertFalse { Files.isSymbolicLink(symbolicPath) }
    }

    @Test
    fun testUnallocatedDiskSpaces() {
        FileSystems.getDefault().fileStores.forEach {
            try {
                println(String.format("%-30s%-10s%-20s%s", it.name(), it.type(),
                    Strings.compactFormat(it.unallocatedSpace), Strings.compactFormat(it.totalSpace)))
            } catch (e: Exception) {
                println(e.message)
            }
        }

        val spaces = Runtimes.unallocatedDiskSpaces()
        assertTrue { spaces.isNotEmpty() }
    }
}
