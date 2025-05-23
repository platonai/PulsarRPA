import requests
import sseclient
import time

SEEDS_FILE = "seeds.txt"
BATCH_SIZE = 5

def read_urls(filename):
    with open(filename, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip().startswith("http")]

def send_command(url):
    command = f"""
        Go to {url}
        After page load: scroll to the middle.

        Summarize the product.
        Extract: product name, price, ratings.
        Find all links containing /dp/.
        """
    response = requests.post(
        "http://localhost:8182/api/commands/plain?mode=async",
        headers={"Content-Type": "text/plain"},
        data=command
    )
    return response.text.strip()

def stream_sse(command_id, url):
    sse_url = f"http://localhost:8182/api/commands/{command_id}/stream"
    headers = {"Accept": "text/event-stream"}
    response = requests.get(sse_url, headers=headers, stream=True)
    client = sseclient.SSEClient(response)
    print(f"[{url}] SSE stream started.")
    for event in client.events():
        if event.data:
            print(f"[{url}] SSE update: {event.data.strip()}")

def process_batch(urls):
    from threading import Thread
    threads = []
    for url in urls:
        command_id = send_command(url)
        print(f"[{url}] Command ID: {command_id}")
        t = Thread(target=stream_sse, args=(command_id, url))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()

def main():
    urls = read_urls(SEEDS_FILE)
    for i in range(0, len(urls), BATCH_SIZE):
        batch = urls[i:i+BATCH_SIZE]
        print(f"Processing batch {i//BATCH_SIZE+1} with {len(batch)} URLs...")
        process_batch(batch)
        time.sleep(2)  # Optional: short pause between batches

if __name__ == "__main__":
    main()