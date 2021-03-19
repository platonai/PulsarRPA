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
package org.apache.gora.mongodb.mapreduce;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests related to {@link org.apache.gora.mongodb.store.MongoStore} using
 * mapreduce.
 */
public class TestMongoStoreCountQuery extends GoraMongoMapredTest {

  protected DataStore<String, Employee> employeeStore;
  protected DataStore<String, WebPage> webPageStore;

  @Before
  public void setUp() throws Exception {
    employeeStore = testDriver.createDataStore(String.class, Employee.class);
    webPageStore = testDriver.createDataStore(String.class, WebPage.class);
    testDriver.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testDriver.tearDown();
  }

  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore,
        testDriver.getConfiguration());
  }
}
