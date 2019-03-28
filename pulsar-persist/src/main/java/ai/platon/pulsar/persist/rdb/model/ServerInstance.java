/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist.rdb.model;

import com.j256.ormlite.field.DatabaseField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

// @XmlRootElement is not required in MOXy
@XmlRootElement
@Entity
public class ServerInstance implements Serializable {

    private static final long serialVersionUID = 591015632997820141L;
    @Id
    @GeneratedValue
    private Long id;
    @Column
    @DatabaseField(uniqueIndexName = "ip_port")
    private String ip;
    @Column
    @DatabaseField(uniqueIndexName = "ip_port")
    private int port;
    @Column
    private String type;

    public ServerInstance() {
    }

    public ServerInstance(int port, Type type) {
        this.ip = "";
        this.port = port;
        this.type = type.name();
    }

    public ServerInstance(String ip, int port, Type type) {
        this.ip = ip;
        this.port = port;
        this.type = type.name();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "id: " + id + ", ip: " + ip + ", port: " + port + ", type: " + type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerInstance other = (ServerInstance) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public enum Type {FetchService, PulsarMaster}
}
