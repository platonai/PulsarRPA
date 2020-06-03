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

import ai.platon.pulsar.common.config.Params

interface PulsarJobBase {
    val status: Map<String, Any>

    fun run(params: Params): Map<String, Any>

    /**
     * Stop the job with the possibility to resume. Subclasses should override
     * this, since by default it calls [.killJob].
     *
     * @return true if succeeded, false otherwise
     */
    @Throws(Exception::class)
    fun stopJob(): Boolean

    /**
     * Kill the job immediately. Clients should assume that any results that the
     * job produced so far are in inconsistent state or missing.
     *
     * @return true if succeeded, false otherwise.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun killJob(): Boolean
}
