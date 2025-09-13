package ai.platon.pulsar.client

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import kotlin.system.exitProcess

/**
 * A simple scraper which just send an X-SQL request to the pulsar server and get the scrape result
 * */
fun main() {
    val sql = """
        select
            dom_first_text(dom, '#productTitle') as `title`,
            dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
            dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
            array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
            dom_base_uri(dom) as `baseUri`
        from
            load_and_select('https://www.amazon.com/dp/B0FFTT2J6N -i 10s', ':root')
    """

    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8182/api/x/e"))
        .header("Content-Type", "text/plain")
        .POST(BodyPublishers.ofString(sql)).build()
    val response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString()).body()

    println(response)
}
