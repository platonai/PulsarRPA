/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.gora.mongodb.authentications;

import de.flapdoodle.embed.mongo.distribution.Version;
import ai.platon.pulsar.gora.mongodb.store.TestMongoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform {@link TestMongoStore} tests on MongoDB 3.2.x server with Plain Authentication mechanism.
 */
public class PLAIN_AuthenticationTest extends TestMongoStore {
  private static Logger log = LoggerFactory
          .getLogger(PLAIN_AuthenticationTest.class);
  static {
    try {
      setTestDriver(new GoraMongodbAuthenticationTestDriver("PLAIN", Version.Main.V3_4));
    } catch (Exception e) {
      log.error("MongoDb Test Driver initialization failed. "+ e.getMessage());
    }
  }
}
