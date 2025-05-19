package ai.platon.pulsar.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Command {
    public static void main(String[] args) throws Exception {
        var command = """
                Go to https://www.amazon.com/dp/B0C1H26C46
                After page load: scroll to the middle.

                Summarize the product.
                Extract: product name, price, ratings.
                Find all links containing /dp/.
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8182/api/commands/spoken"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(command)).build();
        String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println(response);
    }
}
