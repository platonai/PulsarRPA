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
package ai.platon.pulsar.gora.mongodb.mapreduce;

import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import ai.platon.pulsar.gora.mongodb.store.MongoStore;
import org.apache.gora.store.DataStoreFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests related to {@link MongoStore} using
 * mapreduce.
 */
public class TestMongoStoreWordCount extends GoraMongoMapredTest {

  private MongoStore<String, WebPage> webPageStore;
  private MongoStore<String, TokenDatum> tokenStore;

  @Before
  public void setUp() throws Exception {
    webPageStore = DataStoreFactory.getDataStore(MongoStore.class,
        String.class, WebPage.class, testDriver.getConfiguration());
    tokenStore = DataStoreFactory.getDataStore(MongoStore.class, String.class,
        TokenDatum.class, testDriver.getConfiguration());
  }

  @After
  public void tearDown() throws Exception {
    webPageStore.close();
    tokenStore.close();
  }

  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testWordCount(testDriver.getConfiguration(),
        webPageStore, tokenStore);
  }

  //todo fix config
  @Ignore
  @Test
  public void testSparkWordCount() throws Exception {
    MapReduceTestUtils.testSparkWordCount(testDriver.getConfiguration(),
        webPageStore, tokenStore);
  }

}
