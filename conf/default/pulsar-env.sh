#!/usr/bin/env bash
#
#/**
# * Copyright 2007 The Apache Software Foundation
# *
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

# Set environment variables here.

# The java implementation to use.  Java 1.8 required.
export JAVA_HOME="/usr/lib/jvm/java-8-sun"

export HDFS_BASE_URI=hdfs://master:9000

# url to test if solr is available, once solr is not available, we stop crawl anything
export SOLR_TEST_URL=http://master:8983

# Extra Java CLASSPATH elements.  Optional.
# export PULSAR_CLASSPATH=

# The maximum amount of heap to use, in MB. Default is 1000.
# export PULSAR_HEAPSIZE=1000

# Extra Java runtime options.
# Below are what we set by default.  May only work with SUN JVM.
# For more on why as well as other possible settings,
# see http://wiki.apache.org/hadoop/PerformanceTuning
# seems not support by java-8-sun
# export PULSAR_OPTS="-XX:+UseConcMarkSweepGC"

# export PULSAR_JMX_BASE="-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
# export PULSAR_MASTER_OPTS="$PULSAR_MASTER_OPTS $PULSAR_JMX_BASE -Dcom.sun.management.jmxremote.port=10101"

# Extra ssh options.  Empty by default.
# export PULSAR_SSH_OPTS="-o ConnectTimeout=1 -o SendEnv=PULSAR_CONF_DIR"

# Where log files are stored.  $PULSAR_HOME/logs by default.
# export PULSAR_LOG_DIR=${PULSAR_HOME}/logs

# Enable remote JDWP debugging of major HBase processes. Meant for Core Developers 
# export PULSAR_MASTER_OPTS="$PULSAR_MASTER_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8070"

# A string representing this instance of pulsar. $USER by default.
# export PULSAR_IDENT_STRING=$USER

# The scheduling priority for daemon processes.  See 'man nice'.
# export PULSAR_NICENESS=10

# The directory where pid files are stored. /tmp by default.
# export PULSAR_PID_DIR=/tmp

# Seconds to sleep between slave commands.  Unset by default.  This
# can be useful in large clusters, where, e.g., slave rsyncs can
# otherwise arrive faster than the master can service them.
# export PULSAR_SLAVE_SLEEP=0.1

# The default log rolling policy is RFA, where the log file is rolled as per the size defined for the 
# RFA appender. Please refer to the log4j.properties file to see more details on this appender.
# In case one needs to do log rolling on a date change, one should set the environment property
# PULSAR_ROOT_LOGGER to "<DESIRED_LOG LEVEL>,DRFA".
# For example:
# PULSAR_ROOT_LOGGER=INFO,DRFA
# The reason for changing default to RFA is to avoid the boundary case of filling out disk space as 
# DRFA doesn't put any cap on the log size. Please refer to HBase-5655 for more context.
