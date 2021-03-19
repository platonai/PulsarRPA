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
package org.apache.gora.mongodb.authentications;

import de.flapdoodle.embed.mongo.*;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.IStreamProcessor;
import de.flapdoodle.embed.process.io.LogWatchStreamProcessor;
import de.flapdoodle.embed.process.io.NamedOutputStreamProcessor;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.mongodb.store.MongoStoreParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;

import static de.flapdoodle.embed.process.io.Processors.console;
import static de.flapdoodle.embed.process.io.Processors.namedConsole;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Driver to set up an embedded MongoDB database instance for use in our  * unit tests.
 * This class is specially written to automate authentication mechanisms.
 * We use embedded mongodb which is available from
 * https://github.com/flapdoodle-oss/embedmongo.flapdoodle.de
 */
class GoraMongodbAuthenticationTestDriver extends GoraTestDriver {
    private static final Logger log = LoggerFactory.getLogger(GoraMongodbAuthenticationTestDriver.class);
    private static final int INIT_TIMEOUT_MS = 30000;
    private static final String USER_ADDED_TOKEN = "Successfully added user";
    private ThreadLocal<Boolean> started = new ThreadLocal<>();
    private int port;
    private MongodExecutable _mongodExe;
    private MongodProcess _mongod;
    private MongodStarter runtime;
    private IMongodConfig mongodConfig;
    private String adminUsername = "madhawa";
    private String adminPassword = "123";
    private Version.Main useVersion;
    private String authMechanisms;
    private boolean auth = false;

    GoraMongodbAuthenticationTestDriver(String authMechanisms, Version.Main useVersion) throws IOException {
        super(MongoStore.class);
        this.authMechanisms = authMechanisms;
        this.useVersion = useVersion;
        started.set(false);
        if (!this.authMechanisms.equals("MONGODB-CR")) {
            auth = true;
        }

    }

    private void doStart() throws Exception {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaultsWithLogger(Command.MongoD, log)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        runtime = MongodStarter.getInstance(runtimeConfig);
        try {
            log.info("Starting the mongo server without authentications");
            startWithAuth();
            log.info("Adding admin user");
            addAdmin();
            if (this.authMechanisms.equals("SCRAM-SHA-1")) {
                setSCRAM_SHA_1Credentials();
            }
            if (this.authMechanisms.equals("MONGODB-CR")) {
                setMongoDB_CRCredentials();
                tearDownClass();
                auth = true;
                startWithAuth();
                addAdmin();
            }
            // Store Mongo server "host:port" in Hadoop configuration
            // so that MongoStore will be able to get it latter
            conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, "127.0.0.1:" + port);
            conf.set(MongoStoreParameters.PROP_MONGO_DB, "admin");
            conf.set(MongoStoreParameters.PROP_MONGO_AUTHENTICATION_TYPE, this.authMechanisms);
            conf.set(MongoStoreParameters.PROP_MONGO_LOGIN, adminUsername);
            conf.set(MongoStoreParameters.PROP_MONGO_SECRET, adminPassword);
        } catch (Exception e) {
            log.error("Error starting embedded Mongodb server... tearing down test driver.");
            tearDownClass();
        }
    }

    private void startWithAuth() throws IOException {
        try {
            if(!started.get()) {
                prepareExecutable();
                _mongod = _mongodExe.start();
                started.set(true);
            }
        } catch (Exception e) {
            log.error("Error starting embedded Mongodb server... tearing down test driver.");
            tearDownClass();
        }
    }

    private void prepareExecutable() throws IOException {
        final MongoCmdOptionsBuilder cmdBuilder = new MongoCmdOptionsBuilder();
        cmdBuilder.enableAuth(auth);
        final IMongoCmdOptions cmdOptions = cmdBuilder.build();
        MongodConfigBuilder builder = new MongodConfigBuilder()
                .version(useVersion)
                .cmdOptions(cmdOptions)
                .net(new Net(port, Network.localhostIsIPv6()));
        if (auth) {
            builder.setParameter("authenticationMechanisms", authMechanisms);
        }

        mongodConfig = builder.build();
        _mongodExe = runtime.prepare(mongodConfig);
    }

    private void addAdmin() throws IOException, InterruptedException {
        final String scriptText = "db.createUser(\n" +
                "  {\n" +
                "    user: \"madhawa\",\n" +
                "    pwd: \"123\",\n" +
                "    roles: [ { role: \"root\", db: \"admin\" } ]\n" +
                "  }\n" +
                ");";
        runScriptAndWait(scriptText, USER_ADDED_TOKEN, new String[]{"couldn't add user", "failed to load", "login failed"}, "admin", null, null);
    }

    private void setMongoDB_CRCredentials() throws Exception {
        final String scriptText1 = "var schema = db.system.version.findOne({\"_id\" : \"authSchema\"});\nschema.currentVersion = 3;\ndb.system.version.save(schema);\n";
      //  final String scriptText1 = "db.system.version.remove({});\ndb.system.version.insert({ \"_id\" : \"authSchema\", \"currentVersion\" : 3 });";
        runScriptAndWait(scriptText1, "Successfully added authSchema", null, "admin", adminUsername, adminPassword);
    }

    private void setSCRAM_SHA_1Credentials() throws Exception {
        final String scriptText1 = "db.adminCommand({authSchemaUpgrade: 1});\n";
        runScriptAndWait(scriptText1, "Successfully added authSchema", null, "admin", adminUsername, adminPassword);
    }

    private void runScriptAndWait(String scriptText, String token, String[] failures, String dbName, String username, String password) throws IOException {
        IStreamProcessor mongoOutput;
        if (!isEmpty(token)) {
            mongoOutput = new LogWatchStreamProcessor(
                    token,
                    (failures != null) ? new HashSet<>(asList(failures)) : Collections.emptySet(),
                    namedConsole("[mongo shell output]"));
        } else {
            mongoOutput = new NamedOutputStreamProcessor("[mongo shell output]", console());
        }
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.Mongo)
                .processOutput(new ProcessOutput(
                        mongoOutput,
                        namedConsole("[mongo shell error]"),
                        console()))
                .build();
        MongoShellStarter starter = MongoShellStarter.getInstance(runtimeConfig);

        final File scriptFile = writeTmpScriptFile(scriptText);
        final MongoShellConfigBuilder builder = new MongoShellConfigBuilder();
        if (!isEmpty(dbName)) {
            builder.dbName(dbName);
        }
        if (!isEmpty(username)) {
            builder.username(username);
        }
        if (!isEmpty(password)) {
            builder.password(password);
        }
        starter.prepare(builder
                .scriptName(scriptFile.getAbsolutePath())
                .version(mongodConfig.version())
                .net(mongodConfig.net())
                .build()).start();
        if (mongoOutput instanceof LogWatchStreamProcessor) {
            ((LogWatchStreamProcessor) mongoOutput).waitForResult(INIT_TIMEOUT_MS);
        }
    }

    private File writeTmpScriptFile(String scriptText) throws IOException {
        File scriptFile = File.createTempFile("tempfile", ".js");
        scriptFile.deleteOnExit();
        PrintWriter writer = new PrintWriter(scriptFile, "UTF-8");
        writer.write(scriptText);
        writer.close();
        return scriptFile;
    }

    @Override
    public void setUpClass() throws Exception {
        port = Network.getFreeServerPort();
        log.info("Starting embedded Mongodb server on {} port.", port);
        doStart();
    }

    /**
     * Tear the server down
     */
    @Override
    public void tearDownClass() {
        log.info("Shutting down mongodb server...");
        if(started.get()) {
            _mongod.stop();
            _mongodExe.stop();
            started.set(false);
        }
    }


}
