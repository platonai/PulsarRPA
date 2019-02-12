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
package ai.platon.pulsar.rest.service.logging.log4j;

import ai.platon.pulsar.rest.service.logging.CircularList;
import ai.platon.pulsar.rest.service.logging.ListenerConfig;
import ai.platon.pulsar.rest.service.logging.LogWatcher;
import ai.platon.pulsar.rest.service.logging.LoggerInfo;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.*;

public class Log4jWatcher extends LogWatcher<LoggingEvent> {

  final String name;
  AppenderSkeleton appender = null;

  public Log4jWatcher(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return "Log4j ("+name+")";
  }

  @Override
  public List<String> getAllLevels() {
    return Arrays.asList(
        org.apache.log4j.Level.ALL.toString(),
        org.apache.log4j.Level.TRACE.toString(),
        org.apache.log4j.Level.DEBUG.toString(),
        org.apache.log4j.Level.INFO.toString(),
        org.apache.log4j.Level.WARN.toString(),
        org.apache.log4j.Level.ERROR.toString(),
        org.apache.log4j.Level.FATAL.toString(),
        org.apache.log4j.Level.OFF.toString());
  }

  @Override
  public void setLogLevel(String category, String level) {
    org.apache.log4j.Logger log;
    if(LoggerInfo.ROOT_NAME.equals(category)) {
      log = org.apache.log4j.LogManager.getRootLogger();
    } else {
      log = org.apache.log4j.Logger.getLogger(category);
    }
    if(level==null||"unset".equals(level)||"null".equals(level)) {
      log.setLevel(null);
    }
    else {
      log.setLevel(org.apache.log4j.Level.toLevel(level));
    }
  }

  @Override
  public Collection<LoggerInfo> getAllLoggers() {
    org.apache.log4j.Logger root = org.apache.log4j.LogManager.getRootLogger();
    Map<String,LoggerInfo> map = new HashMap<>();
    Enumeration<?> loggers = org.apache.log4j.LogManager.getCurrentLoggers();
    while (loggers.hasMoreElements()) {
      org.apache.log4j.Logger logger = (org.apache.log4j.Logger)loggers.nextElement();
      String name = logger.getName();
      if( logger == root) {
        continue;
      }
      map.put(name, new Log4jInfo(name, logger));

      while (true) {
        int dot = name.lastIndexOf(".");
        if (dot < 0)
          break;
        name = name.substring(0, dot);
        if(!map.containsKey(name)) {
          map.put(name, new Log4jInfo(name, null));
        }
      }
    }
    map.put(LoggerInfo.ROOT_NAME, new Log4jInfo(LoggerInfo.ROOT_NAME, root));
    return map.values();
  }

  @Override
  public void setThreshold(String level) {
    if(appender==null) {
      throw new IllegalStateException("Must have an appender");
    }
    appender.setThreshold(Level.toLevel(level));
  }

  @Override
  public String getThreshold() {
    if(appender==null) {
      throw new IllegalStateException("Must have an appender");
    }
    return appender.getThreshold().toString();
  }

  @Override
  public void registerListener(ListenerConfig cfg) {
    if(history!=null) {
      throw new IllegalStateException("History already registered");
    }
    history = new CircularList<>(cfg.size);

    appender = new EventAppender(this);
    if(cfg.threshold != null) {
      appender.setThreshold(Level.toLevel(cfg.threshold));
    }
    else {
      appender.setThreshold(Level.WARN);
    }
    Logger log = org.apache.log4j.LogManager.getRootLogger();
    log.addAppender(appender);
  }}
