package ai.platon.pulsar.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CommandSSE {
    public static void main(String[] args) throws Exception {
        var command = """
                Go to https://www.amazon.com/dp/B0C1H26C46
                After page load: scroll to the middle.
                
                Summarize the product.
                Extract: product name, price, ratings.
                Find all links containing /dp/.
                """;

        // Send command to server
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8182/api/commands/plain?mode=async"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(command))
                .build();
        var id = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println(id);

        // Receive server send events
        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        var sseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8182/api/commands/" + id + "/stream"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        // Process the SSE stream until command completes
        client.send(sseRequest, HttpResponse.BodyHandlers.ofLines())
                .body()
                .forEach(line -> {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        System.out.println("SSE update: " + data);
                    }
                });
    }
}
