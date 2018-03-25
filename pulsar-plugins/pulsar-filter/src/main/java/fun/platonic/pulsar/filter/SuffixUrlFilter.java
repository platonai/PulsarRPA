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

package fun.platonic.pulsar.filter;

import fun.platonic.pulsar.common.ResourceLoader;
import fun.platonic.pulsar.common.SuffixStringMatcher;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.filter.UrlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_RESOURCE_PREFIX;

/**
 * Filters URLs based on a file of URL suffixes. The file is named by
 * <ol>
 * <li>property "urlfilter.suffix.file" in ./conf/pulsar-default.xml, and</li>
 * <li>attribute "file" in plugin.xml of this plugin</li>
 * </ol>
 * Attribute "file" has higher precedence if defined. If the config file is
 * missing, all URLs will be rejected.
 * <p>
 * <p>
 * This filter can be configured to work in one of two modes:
 * <ul>
 * <li><b>default to reject</b> ('-'): in this mode, only URLs that match
 * suffixes specified in the config file will be accepted, all other URLs will
 * be rejected.</li>
 * <li><b>default to accept</b> ('+'): in this mode, only URLs that match
 * suffixes specified in the config file will be rejected, all other URLs will
 * be accepted.</li>
 * </ul>
 * <p>
 * The format of this config file is one URL suffix per line, with no preceding
 * whitespace. Order, in which suffixes are specified, doesn't matter. Blank
 * lines and comments (#) are allowed.
 * </p>
 * <p>
 * A single '+' or '-' sign not followed by any suffix must be used once, to
 * signify the mode this plugin operates in. An optional single 'I' can be
 * appended, to signify that suffix matches should be case-insensitive. The
 * default, if not specified, is to use case-sensitive matches, i.e. suffix
 * '.JPG' does not match '.jpg'.
 * </p>
 * <p>
 * NOTE: the format of this file is different from urlfilter-prefix, because
 * that plugin doesn't support allowed/prohibited prefixes (only supports
 * allowed prefixes). Please note that this plugin does not support regular
 * expressions, it only accepts literal suffixes. I.e. a suffix "+*.jpg" is most
 * probably wrong, you should use "+.jpg" instead.
 * </p>
 * <h4>Example 1</h4>
 * <p>
 * The configuration shown below will accept all URLs with '.html' or '.htm'
 * suffixes (case-sensitive - '.HTML' or '.HTM' will be rejected), and prohibit
 * all other suffixes.
 * <p>
 * <p>
 * <pre>
 *  # this is a comment
 *
 *  # prohibit all unknown, case-sensitive matching
 *  -
 *
 *  # collect only HTML files.
 *  .html
 *  .htm
 * </pre>
 * <p>
 * </p>
 * <h4>Example 2</h4>
 * <p>
 * The configuration shown below will accept all URLs except common graphical
 * formats.
 * <p>
 * <p>
 * <pre>
 *  # this is a comment
 *
 *  # allow all unknown, case-insensitive matching
 *  +I
 *
 *  # prohibited suffixes
 *  .gif
 *  .png
 *  .jpg
 *  .jpeg
 *  .bmp
 * </pre>
 * <p>
 * </p>
 *
 * @author Andrzej Bialecki
 */
public class SuffixUrlFilter implements UrlFilter {

    public static final String PARAM_URLFILTER_SUFFIX_FILE = "urlfilter.suffix.file";
    public static final String PARAM_URLFILTER_SUFFIX_RULES = "urlfilter.suffix.rules";
    private static final Logger LOG = LoggerFactory.getLogger(SuffixUrlFilter.class);
    private SuffixStringMatcher suffixes;
    private boolean modeAccept = false;
    private boolean filterFromPath = false;
    private boolean ignoreCase = false;

    public SuffixUrlFilter(ImmutableConfig conf) throws IOException {
        try {
            String stringResource = conf.get(PARAM_URLFILTER_SUFFIX_RULES);
            String fileResource = conf.get(PARAM_URLFILTER_SUFFIX_FILE, "suffix-urlfilter.txt");
            String resourcePrefix = conf.get(PULSAR_CONFIG_RESOURCE_PREFIX, "");
            List<String> lines = new ResourceLoader().readAllLines(stringResource, fileResource, resourcePrefix);

            parse(lines);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public SuffixUrlFilter(List<String> lines) throws IOException {
        parse(lines);
    }

    public static void main(String args[]) throws IOException {
        ImmutableConfig conf = new ImmutableConfig();
        SuffixUrlFilter filter;
        if (args.length >= 1)
            filter = new SuffixUrlFilter(new ResourceLoader().readAllLines(args[0], null));
        else {
            filter = new SuffixUrlFilter(conf);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            String out = filter.filter(line);
            if (out != null) {
                System.out.println("ACCEPTED " + out);
            } else {
                System.out.println("REJECTED " + out);
            }
        }
    }

    public String filter(String url) {
        if (url == null)
            return null;
        String _url;
        if (ignoreCase)
            _url = url.toLowerCase();
        else
            _url = url;
        if (filterFromPath) {
            try {
                URL pUrl = new URL(_url);
                _url = pUrl.getPath();
            } catch (MalformedURLException e) {
                // don't care
            }
        }

        String a = suffixes.shortestMatch(_url);
        if (a == null) {
            if (modeAccept)
                return url;
            else
                return null;
        } else {
            if (modeAccept)
                return null;
            else
                return url;
        }
    }

    public void parse(List<String> lines) throws IOException {
        // handle missing config file
        if (lines.isEmpty()) {
            LOG.warn("Missing urlfilter.suffix.file, all URLs will be rejected!");
            suffixes = new SuffixStringMatcher(new String[0]);
            modeAccept = false;
            ignoreCase = false;
            return;
        }

        List<String> aSuffixes = new ArrayList<>();
        boolean allow = false;
        boolean ignore = false;

        for (String line : lines) {
            char first = line.charAt(0);
            switch (first) {
                case ' ':
                case '\n':
                case '#': // skip blank & comment lines
                    break;
                case '-':
                    allow = false;
                    if (line.contains("P"))
                        filterFromPath = true;
                    if (line.contains("I"))
                        ignore = true;
                    break;
                case '+':
                    allow = true;
                    if (line.contains("P"))
                        filterFromPath = true;
                    if (line.contains("I"))
                        ignore = true;
                    break;
                default:
                    aSuffixes.add(line);
            }
        }

        if (ignore) {
            for (int i = 0; i < aSuffixes.size(); i++) {
                aSuffixes.set(i, aSuffixes.get(i).toLowerCase());
            }
        }

        suffixes = new SuffixStringMatcher(aSuffixes);
        modeAccept = allow;
        ignoreCase = ignore;
    }

    public boolean isModeAccept() {
        return modeAccept;
    }

    public void setModeAccept(boolean modeAccept) {
        this.modeAccept = modeAccept;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public void setFilterFromPath(boolean filterFromPath) {
        this.filterFromPath = filterFromPath;
    }
}
