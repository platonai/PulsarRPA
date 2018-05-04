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
package fun.platonic.pulsar.filter;

import fun.platonic.pulsar.common.ResourceLoader;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.URLUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.filter.UrlFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.platonic.pulsar.net.domain.DomainSuffix;
import fun.platonic.pulsar.net.domain.DomainSuffixes;

import java.util.LinkedHashSet;
import java.util.Set;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;

/**
 * <p>
 * Filters URLs based on a file containing domain suffixes, domain names, and
 * hostnames. Only a url that matches one of the suffixes, domains, or hosts
 * present in the file is allowed.
 * </p>
 * <p>
 * <p>
 * Urls are checked in order of domain suffix, domain name, and hostname against
 * entries in the domain file. The domain file would be setup as follows with
 * one entry per line:
 * <p>
 * <pre>
 * com apache.org www.apache.org
 * </pre>
 * <p>
 * <p>
 * The first line is an example of a filter that would allow all .com domains.
 * The second line allows all urls from apache.org and all of its subdomains
 * such as lucene.apache.org and hadoop.apache.org. The third line would allow
 * only urls from www.apache.org. There is no specific ordering to entries. The
 * entries are from more general to more specific with the more general
 * overridding the more specific.
 * </p>
 * <p>
 * The domain file defaults to domain-urlfilter.txt in the classpath but can be
 * overridden using the:
 * <p>
 * <ul>
 * <ol>
 * property "urlfilter.domain.file" in ./conf/pulsar-*.xml, and
 * </ol>
 * <ol>
 * attribute "file" in plugin.xml of this plugin
 * </ol>
 * </ul>
 */
public class DomainUrlFilter implements UrlFilter {

    public static final String PARAM_URLFILTER_DOMAIN_RULES = "urlfilter.domain.rules";
    public static final String PARAM_URLFILTER_DOMAIN_FILE = "urlfilter.domain.file";
    private static final Logger LOG = LoggerFactory.getLogger(DomainUrlFilter.class);
    private Set<String> domainSet = new LinkedHashSet<>();

    private DomainSuffixes tlds;

    /**
     * Constructor
     *
     * @param conf The configuration
     */
    public DomainUrlFilter(ImmutableConfig conf) {
        String stringResource = conf.get(PARAM_URLFILTER_DOMAIN_RULES);
        String resourcePrefix = conf.get(PULSAR_CONFIG_PREFERRED_DIR, "");
        String fileResource = conf.get(PARAM_URLFILTER_DOMAIN_FILE, "domain-urlfilter.txt");
        domainSet.addAll(new ResourceLoader().readAllLines(stringResource, fileResource, resourcePrefix));

        tlds = DomainSuffixes.getInstance();

        LOG.info("Allowed domains : " + StringUtils.join(domainSet, ", "));
    }

    public String filter(String url) {
        try {
            // match for suffix, domain, and host in that order. more general will
            // override more specific
            String domain = URLUtil.getDomainName(url).toLowerCase().trim();
            String host = URLUtil.getHostName(url);
            String suffix = null;

            DomainSuffix domainSuffix = URLUtil.getDomainSuffix(tlds, url);
            if (domainSuffix != null) {
                suffix = domainSuffix.getDomain();
            }

            if (domainSet.contains(suffix) || domainSet.contains(domain) || domainSet.contains(host)) {
                return url;
            }
        } catch (Exception e) {
            LOG.error("Could not apply filter on url: " + url + "\n" + StringUtil.stringifyException(e));
        }

        return null;
    }
}
