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

import ai.platon.pulsar.common.Runtimes.checkIfProcessRunning
import ai.platon.pulsar.common.Runtimes.deleteBrokenSymbolicLinks
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestAppRuntimes {
    @Test
    fun testEnv() {
        println(System.getenv("USER"))
    }

    @Test
    fun testJavaProcess() {
        val running = checkIfProcessRunning("HMaster")
        println(running)
    }

    @Test
    fun testDeleteBrokenSymbolicLinksUsingBash() {
        val tmp = Paths.get(SystemUtils.JAVA_IO_TMPDIR)
        val file = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.writeString(file, "to be deleted")
        val symbolicPath = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.createSymbolicLink(symbolicPath, file)

        assertTrue { Files.exists(file) }
        assertTrue { Files.exists(symbolicPath) }

        Files.delete(file)
        assertFalse { Files.exists(file) }
        assertFalse { Files.exists(symbolicPath) }
        assertTrue { Files.isSymbolicLink(symbolicPath) }

        deleteBrokenSymbolicLinks(tmp)
        if (SystemUtils.IS_OS_WINDOWS) {
            // TODO: what happens on windows
        } else {
            assertFalse { Files.isSymbolicLink(symbolicPath) }
        }
    }

    @Test
    fun testDeleteBrokenSymbolicLinksUsingJava() {
        val tmp = Paths.get(SystemUtils.JAVA_IO_TMPDIR)
        val file = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.writeString(file, "to be deleted")
        val symbolicPath = tmp.resolve(RandomStringUtils.randomAlphabetic(5))
        Files.createSymbolicLink(symbolicPath, file)

        assertTrue { Files.exists(file) }
        assertTrue { Files.exists(symbolicPath) }

        Files.delete(file)
        assertFalse { Files.exists(file) }
        assertFalse { Files.exists(symbolicPath) }
        assertTrue { Files.isSymbolicLink(symbolicPath) }

        Files.list(tmp).filter { Files.isSymbolicLink(it) && !Files.exists(it) }.forEach { Files.delete(it) }

        assertFalse { Files.isSymbolicLink(symbolicPath) }
    }
}
