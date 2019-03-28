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
package ai.platon.pulsar.protocol.httpclient;

import ai.platon.pulsar.persist.metadata.MultiMetadata;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Provides the Http protocol implementation with the ability to authenticate
 * when prompted. The goal is to provide multiple authentication types but for
 * now just the {@link HttpBasicAuthentication} authentication type is provided.
 *
 * @see HttpBasicAuthentication
 * @see Http
 * @see HttpResponse
 *
 * @author Matt Tencati
 */
public class HttpAuthenticationFactory implements Configurable {

    /**
     * The HTTP Authentication (WWW-Authenticate) header which is returned by a
     * webserver requiring authentication.
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    public static final Logger LOG = LoggerFactory
            .getLogger(HttpAuthenticationFactory.class);

    private static Map<?, ?> auths = new TreeMap<Object, Object>();

    private Configuration conf = null;

    public HttpAuthenticationFactory(Configuration conf) {
        setConf(conf);
    }

  /*
   * ---------------------------------- * <implementation:Configurable> *
   * ----------------------------------
   */

    public Configuration getConf() {
        return conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
        // if (conf.getBoolean("http.auth.verbose", false)) {
        // log.setLevel(Level.FINE);
        // } else {
        // log.setLevel(Level.WARNING);
        // }
    }

  /*
   * ---------------------------------- * <implementation:Configurable> *
   * ----------------------------------
   */

    @SuppressWarnings("unchecked")
    public HttpAuthentication findAuthentication(MultiMetadata header) {

        if (header == null)
            return null;

        try {
            Collection challenge = null;
            if (header instanceof MultiMetadata) {
                Object o = header.get(WWW_AUTHENTICATE);
                if (o instanceof Collection) {
                    challenge = (Collection<?>) o;
                } else {
                    challenge = new ArrayList<String>();
                    challenge.add(o.toString());
                }
            } else {
                String challengeString = header.get(WWW_AUTHENTICATE);
                if (challengeString != null) {
                    challenge = new ArrayList<>();
                    challenge.add(challengeString);
                }
            }
            if (challenge == null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Authentication challenge is null");
                }
                return null;
            }

            Iterator<?> i = challenge.iterator();
            HttpAuthentication auth = null;
            while (i.hasNext() && auth == null) {
                String challengeString = (String) i.next();
                if (challengeString.equals("NTLM")) {
                    challengeString = "Basic realm=techweb";
                }

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Checking challengeString=" + challengeString);
                }
                auth = HttpBasicAuthentication.getAuthentication(challengeString, conf);
                if (auth != null)
                    return auth;

                // TODO Add additional Authentication lookups here
            }
        } catch (Exception e) {
            LOG.error("Failed with following exception: ", e);
        }
        return null;
    }
}
