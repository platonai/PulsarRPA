/*******************************************************************************
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
 ******************************************************************************/
package ai.platon.pulsar.rest.api.resources;

import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.common.options.ParseOptions;
import ai.platon.pulsar.common.options.PulsarOptions;
import ai.platon.pulsar.crawl.component.*;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.parse.html.JsoupParser;
import ai.platon.pulsar.crawl.parse.html.JsoupUtils;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.WebPageFormatter;
import ai.platon.pulsar.rest.api.model.response.LinkDatum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hadoop.classification.InterfaceStability;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/pages")
public class PagesResource {

    public static final Logger LOG = LoggerFactory.getLogger(PagesResource.class);

    private ImmutableConfig conf;
    private WebDb webDb;
    private FetchComponent fetchComponent;
    private ParseComponent parseComponent;
    private IndexComponent indexComponent;
    private UpdateComponent updateComponent;
    private LoadComponent loadComponent;
    private MetricsCounters metricsCounters;
    private ExecutorService executorService;
    private int maxThreads = 50;
    // TODO: Check thread safety
    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // @Autowired and @Inject have the same purpose, but @Autowired annotation issues better messages
    // @Inject
    @Autowired
    public PagesResource(
            WebDb webDb,
            FetchComponent fetchComponent,
            ParseComponent parseComponent,
            IndexComponent indexComponent,
            UpdateComponent updateComponent,
            LoadComponent loadComponent,
            MetricsCounters metricsCounters,
            ImmutableConfig conf) {
        this.webDb = webDb;
        this.fetchComponent = fetchComponent;
        this.parseComponent = parseComponent;
        this.indexComponent = indexComponent;
        this.updateComponent = updateComponent;
        this.loadComponent = loadComponent;
        this.metricsCounters = metricsCounters;
        this.conf = conf;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }

    @GetMapping("/")
    public String get(
            @RequestParam("url") String url,
            @RequestParam("encoding") String encoding) {
        WebPage page = webDb.getOrNil(url);
        return page.isNil() ? "" : page.getContentAsString();
    }

    @GetMapping("/get-content")
    public String getHtml(
            @RequestParam("url") String url,
            @RequestParam("args") String args,
            @RequestParam("sanitize") boolean sanitize,
            @RequestParam("pithy") boolean pithy,
            @RequestParam("encoding") String encoding
    ) throws IOException {
        args += " -ps"; // Force parse
        WebPage page = loadComponent.load(url, args);
        String content = page.isNil() ? "" : page.getContentAsString();

        if (sanitize) {
            JsoupParser parser = new JsoupParser(page, conf);
            Document doc = parser.parse();

            content = JsoupUtils.toHtmlPiece(doc, pithy);
        }

        return content;
    }

    @GetMapping("/show")
    public String show(
            @RequestParam("args") String args,
            @RequestParam("url") String url
    ) {
        WebPage page = webDb.getOrNil(url);
        if (page.isNil()) {
            return "";
        }

        String[] options = PulsarOptions.Companion.split(args);
        for (String option : options) {
            if (option.equals("-a") || option.equals("--all")) {
                return new WebPageFormatter(page).withFields().withText().format();
            }
            if (option.equals("-f") || option.equals("--fields")) {
                return new WebPageFormatter(page).withFields().format();
            }
            if (option.equals("-s") || option.equals("--status")) {
                return new WebPageFormatter(page).format();
            }
        }

        return new WebPageFormatter(page).format();
    }

    @GetMapping(value = "/get-links")
    public String getLinks(
            @RequestParam("args") String args,
            @RequestParam("url") String url
    ) {
        WebPage page = webDb.getOrNil(url);
        if (page.isNil()) {
            return "[]";
        }

        boolean ll = false;
        boolean dl = false;
        boolean lk = false;
        for (String option : PulsarOptions.Companion.split(args)) {
            switch (option) {
                case "-ll":
                case "--live-links":
                    ll = true;
                    break;
                case "-dl":
                case "--dead-links":
                    dl = true;
                    break;
                case "-lk":
                case "--links":
                    lk = true;
                    break;
            }
        }

        ParseOptions parseOptions = ParseOptions.Companion.parse(args);
        if (parseOptions.isParse()) {
            parseComponent.parse(page, parseOptions.isReparseLinks(), parseOptions.isNoLinkFilter());
        }

        Map<String, String> links = new HashMap<>();
        if (lk) {
            page.getLinks().forEach(l -> links.put(l.toString(), l.toString()));
        }
        if (ll) {
            page.getLiveLinks().values().forEach(l -> links.put(l.getUrl().toString(), l.getAnchor().toString()));
        }
        if (dl) {
            page.getDeadLinks().forEach(l -> links.put(l.toString(), l.toString()));
        }

        List<LinkDatum> response = links.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().length()))
                .distinct()
                .map(e -> new LinkDatum(e.getKey(), e.getValue(), 0))
                .collect(Collectors.toList());

        return gson.toJson(response);
    }

    @GetMapping("/clear-dead-links")
    public String clearDeadLinks(
            @RequestParam("url") String url
    ) {
        WebPage page = webDb.getOrNil(url);
        if (!page.isNil()) {
            page.getDeadLinks().clear();

            webDb.put(page);
            webDb.flush();

            page = webDb.getOrNil(url);
        }

        return page.isNil() ? "{}" : gson.toJson(new WebPageFormatter(page).withLinks(true).toMap());
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @GetMapping("/load")
    public String load(
            @RequestParam("url") String url,
            @RequestParam("args") String args
    ) {
        LOG.debug("Loading " + url);

        LoadOptions loadOptions = LoadOptions.Companion.parse(args);

        WebPage page = loadComponent.load(url, loadOptions);
        loadComponent.flush();

        Map<String, Object> result = new WebPageFormatter(page)
                .withText(loadOptions.getWithText())
                .withLinks(loadOptions.getWithLinks())
                .withEntities(loadOptions.getWithModel())
                .toMap();
        return gson.toJson(result);
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @InterfaceStability.Evolving
    @GetMapping("/outgoing/load")
    public String loadOutPages(
            @RequestParam("url") String url,
            @RequestParam("args") String loadArgs,
            @RequestParam("lf") String linkArgs,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit,
            @RequestParam("args2") String loadArgs2,
            @RequestParam("log") int log,
            HttpServletRequest request
    ) {
        linkArgs += ",log," + log;

        Map<String, Object> results = loadComponent.loadOutPages(url,
                LoadOptions.Companion.parse(loadArgs),
                LinkOptions.parse(linkArgs), start, limit,
                LoadOptions.Companion.parse(loadArgs2),
                "",
                log);
        loadComponent.flush();

        // Failed to inject HttpServletRequest using jersey-test-framework-provider-inmemory
        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        return gson.toJson(results);
    }

    @GetMapping("/fetch")
//  @Produces(MediaType.APPLICATION_JSON)
    public String fetch(
            @RequestParam("args") String args,
            @RequestParam("url") String url,
            @RequestParam("log") int log,
            HttpServletRequest request
    ) {
        LoadOptions loadOptions = LoadOptions.Companion.parse(args);

        WebPage page = loadComponent.load(url, loadOptions);
        loadComponent.flush();

        Map<String, String> header = new LinkedHashMap<>();
        if (request != null) {
            header.put("uri", request.getRequestURI());
            header.put("queryString", request.getQueryString());
        }

        if (log > 0) {
            header.put("loadOptions", loadOptions.toString());
        }

        Map<String, Object> doc = new WebPageFormatter(page)
                .withText(loadOptions.getWithText())
                .withLinks(loadOptions.getWithLinks())
                .withEntities(loadOptions.getWithModel())
                .toMap();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("header", header);
        response.put("body", doc);

        return gson.toJson(response);
    }

    @GetMapping(value = "/outgoing/fetch")
    public String fetchOutgoingPages(
            @RequestParam("url") String url,
            @RequestParam("args") String args,
            @RequestParam("lf") String linkArgs,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit,
            @RequestParam("args2") String args2,
            @RequestParam("log") int log,
            @RequestParam("async") boolean async,
            HttpServletRequest request
    ) {
        // force parse and allow retrying
        args += ",-ps,--retry";
        args2 += ",-ps,--expires,1d";
        linkArgs += ",log," + log;

        Map<String, Object> results = loadComponent.loadOutPages(
                url, LoadOptions.Companion.parse(args),
                LinkOptions.parse(linkArgs), start, limit,
                LoadOptions.Companion.parse(args2),
                "",
                log);
        loadComponent.flush();

        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        return gson.toJson(results);
    }

    @GetMapping("/fetch-all")
    public String fetchAll(
            @RequestParam("args") String args,
            @RequestParam("urls") String urls,
            HttpServletRequest request
    ) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }

        args += ",-ps,--expires,0s";
        LoadOptions loadOptions = LoadOptions.Companion.parse(args);

        List<Map<String, Object>> docs = Stream.of(urls.split("\n"))
                .map(url -> loadComponent.load(url, loadOptions))
                .map(p -> new WebPageFormatter(p).toMap()).collect(Collectors.toList());
        loadComponent.flush();

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("totalCount", docs.size());
        results.put("count", docs.size());

        results.put("docs", docs);

        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        return gson.toJson(results);
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @PutMapping("/parse")
    public String parse(
            String query,
            @RequestParam("url") String url,
            @RequestParam("args") String args
    ) {
        LoadOptions loadOptions = LoadOptions.Companion.parse(args);

//    LOG.debug("LoadOptions: " + loadOptions.toString());
//    LOG.debug("Query: " + query);
        WebPage page = loadComponent.load(url, loadOptions);
        loadComponent.flush();

        return gson.toJson(new WebPageFormatter(page).withEntities(loadOptions.getWithModel()).toMap());
    }

    @PutMapping(value = "/outgoing/parse")
    public String parseOutgoingPages(
            String query,
            @RequestParam("url") String url,
            @RequestParam("args") String loadArgs,
            @RequestParam(value = "lf", defaultValue = "-amin,5,-amax,50,-umin,23,-umax,150") String linkArgs,
            @RequestParam(value = "start", defaultValue = "1") int start,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "args2", defaultValue = "-ps,--expires,1d,--with-entities") String loadArgs2,
            @RequestParam(value = "log", defaultValue = "0") int log,
            HttpServletRequest request
    ) {
        loadArgs += ",-ps";
        loadArgs2 += ",-ps";
        linkArgs += ",-log," + log;

        Map<String, Object> results = loadComponent.loadOutPages(url,
                LoadOptions.Companion.parse(loadArgs),
                LinkOptions.parse(linkArgs), start, limit,
                LoadOptions.Companion.parse(loadArgs2),
                query,
                log);
        loadComponent.flush();

        // Failed to inject HttpServletRequest using jersey-test-framework-provider-inmemory
        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        results.put("metricsCounters", metricsCounters.getStatus(true));

        return gson.toJson(results);
    }

    @GetMapping("/index")
    public String index(@RequestParam("url") String url) {
        WebPage page = loadComponent.load(url);
        IndexDocument doc = indexComponent.index(page);

        return gson.toJson(doc.asMultimap());
    }

    @GetMapping(value = "/outgoing/index")
    public String indexOutgoingPages(
            @RequestParam("url") String url,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit,
            HttpServletRequest request
    ) {
        WebPage page = loadComponent.load(url);

        int totalCount = page.getLiveLinks().size();
        LoadOptions loadOptions = LoadOptions.Companion.create();
        Map<String, Map<String, List<String>>> indexDocuments = page.getSimpleLiveLinks().stream()
                .skip(start > 1 ? start - 1 : 0).limit(limit)
                .map(l -> loadComponent.load(l, loadOptions))
                .map(indexComponent::index)
                .collect(Collectors.toMap(IndexDocument::getUrl, IndexDocument::asMultimap, (i, i2) -> i2));

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("totalCount", totalCount);
        results.put("count", indexDocuments.size());

        results.put("docs", indexDocuments);

        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        return gson.toJson(results);
    }

    @GetMapping("/index-all")
    public String indexAll(
            @RequestParam("args") String args,
            @RequestParam("urls") String urls,
            HttpServletRequest request
    ) {
        if (urls.isEmpty()) {
            return "[]";
        }

        String[] urlArray = urls.split("\n");
        int totalCount = urlArray.length;
        LoadOptions loadOptions = LoadOptions.Companion.parse(args);
        Map<String, Map<String, List<String>>> indexDocuments = Stream.of(urlArray)
                .map(l -> loadComponent.load(l, loadOptions))
                .map(p -> indexComponent.index(p))
                .collect(Collectors.toMap(IndexDocument::getUrl, IndexDocument::asMultimap, (i, i2) -> i2));

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("totalCount", totalCount);
        results.put("count", indexDocuments.size());

        results.put("docs", indexDocuments);

        if (request != null) {
            results.put("uri", request.getRequestURI());
            results.put("queryString", request.getQueryString());
        }

        return gson.toJson(results);
    }

    @GetMapping(value = "/update")
    public String updateByOutgoingPages(
            @RequestParam("url") String url,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit,
            @RequestParam("persist") boolean persist
    ) {
        WebPage page = webDb.getOrNil(url);
        if (page.isNil()) {
            return "{}";
        }

        LoadOptions loadOptions = LoadOptions.Companion.parse("-ps,-prst");
        List<WebPage> outgoingPages = page.getSimpleLiveLinks().stream()
                .skip(start > 1 ? start - 1 : 0).limit(limit)
                .map(l -> loadComponent.load(l, loadOptions))
                .collect(Collectors.toList());

        updateComponent.updateByOutgoingPages(page, outgoingPages);

        loadComponent.flush();

        return gson.toJson(new WebPageFormatter(page).toMap());
    }

    /**
     * Remove seed
     *
     * @param url url to delete
     * @return boolean
     */
    @DeleteMapping("/delete")
    public String delete(@RequestParam("url") String url) {
        boolean ret = webDb.delete(url);
        return String.valueOf(ret);
    }
}
