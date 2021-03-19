/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.gora.mongodb;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.gora.GoraTestDriver;
import ai.platon.pulsar.gora.mongodb.store.MongoStore;
import ai.platon.pulsar.gora.mongodb.store.MongoStoreParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Driver to set up an embedded MongoDB database instance for use in our
 * unit tests. We use embedded mongodb which is available from
 * https://github.com/flapdoodle-oss/embedmongo.flapdoodle.de
 */
public class GoraMongodbTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory
          .getLogger(GoraMongodbTestDriver.class);

  private MongodExecutable _mongodExe;
  private MongodProcess _mongod;
  private MongoClient _mongo;
  private final Version.Main version;

  /**
   * Constructor for this class.
   */
  public GoraMongodbTestDriver() {
    this(Version.Main.PRODUCTION);
  }

  public GoraMongodbTestDriver(Version.Main version) {
    super(MongoStore.class);
    this.version = version;
  }

  /**
   * Initiate the MongoDB server on the default port
   */
  @Override
  public void setUpClass() throws IOException {
    IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
            .defaultsWithLogger(Command.MongoD, log)
            .processOutput(ProcessOutput.getDefaultInstanceSilent())
            .build();

    MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);

    int port = Network.getFreeServerPort();
    IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(version)
            .net(new Net(port, Network.localhostIsIPv6())).build();

    // Store Mongo server "host:port" in Hadoop configuration
    // so that MongoStore will be able to get it latter
    conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, "127.0.0.1:" + port);

    log.info("Starting embedded Mongodb server on {} port.", port);
    try {

      _mongodExe = runtime.prepare(mongodConfig);
      _mongod = _mongodExe.start();

      _mongo = new MongoClient("localhost", port);
    } catch (Exception e) {
      log.error("Error starting embedded Mongodb server... tearing down test driver.");
      tearDownClass();
    }
  }

  /**
   * Tear the server down
   */
  @Override
  public void tearDownClass() {
    log.info("Shutting down mongodb server...");
    _mongod.stop();
    _mongodExe.stop();
  }

  public Mongo getMongo() {
    return _mongo;
  }

}
