package ai.platon.pulsar.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A simple scraper which just send an X-SQL request to the pulsar server and get the scrape result
 * */
public class Scraper {

    public static void main(String[] args) throws IOException, InterruptedException {
        String sql = "select\n" +
                "            dom_first_text(dom, '#productTitle') as `title`,\n" +
                "            dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,\n" +
                "            dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,\n" +
                "            array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,\n" +
                "            dom_base_uri(dom) as `baseUri`\n" +
                "        from\n" +
                "            load_and_select('https://www.amazon.com/dp/B0C1H26C46', ':root')";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8182/api/x/e"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(sql)).build();
        String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();

        System.out.println(response);
    }
}
