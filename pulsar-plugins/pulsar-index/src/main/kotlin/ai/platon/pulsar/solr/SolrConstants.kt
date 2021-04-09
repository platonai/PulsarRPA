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
package ai.platon.pulsar.solr

import ai.platon.pulsar.solr.SolrConstants

interface SolrConstants {
    companion object {
        const val SOLR_PREFIX = "solr."
        const val USE_AUTH = SOLR_PREFIX + "auth"
        const val USERNAME = SOLR_PREFIX + "auth.username"
        const val PASSWORD = SOLR_PREFIX + "auth.password"
    }
}