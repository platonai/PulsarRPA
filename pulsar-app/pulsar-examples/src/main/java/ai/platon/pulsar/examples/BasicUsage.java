package ai.platon.pulsar.examples;

import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.dom.FeaturedDocument;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.session.PulsarSession;
import com.google.gson.Gson;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicUsage {
    public static void main(String[] args) {
        // create a pulsar session
        PulsarSession session = PulsarContexts.createSession();
        // the main url we are playing with
        String url = "https://list.jd.com/list.html?cat=652,12345,12349";
        // load a page, fetch it from the web if it has expired or if it's being fetched for the first time
        WebPage page = session.load(url, "-expires 1d");
        // parse the page content into a Jsoup document
        FeaturedDocument document = session.parse(page, false);
        // do something with the document
        // ...

        // or, load and parse
        FeaturedDocument document2 = session.loadDocument(url, "-expires 1d");
        // do something with the document
        // ...

        // load all pages with links specified by -outLink
        List<WebPage> pages = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]");
        // load, parse and scrape fields
        List<Map<String, String>> fields = session.scrape(url, "-expires 1d", "li[data-sku]",
                Arrays.asList(".p-name em", ".p-price"));
        // load, parse and scrape named fields
        List<Map<String, String>> fields2 = session.scrape(url, "-i 1d", "li[data-sku]",
                Map.of("name", ".p-name em", "price", ".p-price"));

        System.out.println("== document");
        System.out.println(document.getTitle());
        System.out.println(document.selectFirstOptional("title").map(Element::text));

        System.out.println("== document2");
        System.out.println(document2.getTitle());
        System.out.println(document2.selectFirstOptional("title").map(Element::text));

        System.out.println("== pages");
        System.out.println(pages.stream().map(WebPage::getUrl).collect(Collectors.toList()));

        Gson gson = new Gson();
        System.out.println("== fields");
        System.out.println(gson.toJson(fields));

        System.out.println("== fields2");
        System.out.println(gson.toJson(fields2));
    }
}
