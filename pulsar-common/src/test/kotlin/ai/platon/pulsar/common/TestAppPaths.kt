/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import org.junit.Test
import kotlin.test.assertTrue

class TestAppPaths {

    @Test
    @Throws(Exception::class)
    fun testGet() {
        println(AppPaths.get("scripts", "finish_job-1217.20347.sh"))
        println(AppPaths.getTmp("scripts", "finish_job-1217.20347.sh"))
        assertTrue(AppPaths.getTmp("scripts", "finish_job-1217.20347.sh").toString().contains(AppConstants.TMP_DIR))
    }
}
