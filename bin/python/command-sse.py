import json
import requests
import sseclient

def main():
    command = """
        Go to https://www.amazon.com/dp/B0C1H26C46
        After page load: scroll to the middle.

        Summarize the product.
        Extract: product name, price, ratings.
        Find all links containing /dp/.
        """

    # Send command to server
    response = requests.post(
        "http://localhost:8182/api/commands/spoken?mode=async",
        headers={"Content-Type": "text/plain"},
        data=command
    )
    command_id = response.text
    print(command_id)

    # Connect to SSE stream
    url = f"http://localhost:8182/api/commands/{command_id}/stream"
    headers = {"Accept": "text/event-stream"}

    # Create a requests response object with the headers
    response = requests.get(url, headers=headers, stream=True)

    # Process the SSE stream
    client = sseclient.SSEClient(response)
    for event in client.events():
        if event.data:
            data = event.data.strip()
            print(f"SSE update: {data}")

            # Check if command has completed
            if '"status":"COMPLETED"' in data or '"status":"FAILED"' in data:
                print("Command execution finished")
                break

if __name__ == "__main__":
    main()