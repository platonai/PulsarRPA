/**
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
package ai.platon.pulsar.rest.service;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.TableUtils;
import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import ai.platon.pulsar.persist.rdb.model.dao.OrmliteDaoFactory;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class ServerInstanceService {

  private Dao<ServerInstance, Long> serverInstanceDao;

  public ServerInstanceService(OrmliteDaoFactory daoFactory, boolean autoTruncate) {
    serverInstanceDao = daoFactory.createDao(ServerInstance.class);
    if (autoTruncate) {
      truncate();
    }
  }

  public ServerInstance register(ServerInstance serverInstance) {
    try {
      serverInstanceDao.createOrUpdate(serverInstance);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return serverInstance;
  }

  public void unregister(Long id) {
    try {
      serverInstanceDao.deleteById(id);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<ServerInstance> list() {
    try {
      return serverInstanceDao.queryForAll();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<ServerInstance> list(ServerInstance.Type type) {
    try {
      return serverInstanceDao.queryForEq("type", type);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ServerInstance get(Long id) {
    try {
      return serverInstanceDao.queryForId(id);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void truncate() {
    try {
      TableUtils.dropTable(serverInstanceDao.getConnectionSource(), ServerInstance.class, true);
      TableUtils.createTableIfNotExists(serverInstanceDao.getConnectionSource(), ServerInstance.class);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
