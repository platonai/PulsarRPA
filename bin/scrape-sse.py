#!/usr/bin/env python3

import argparse
import re
import requests
import json
import sys
import time
from typing import List, Dict, Any
from concurrent.futures import ThreadPoolExecutor

# Default parameters
SEEDS_FILE = "seeds.txt"
BATCH_SIZE = 1
MAX_URLS = 2
BASE_URL = "http://localhost:8182/api/x/s"
SSE_URL_TEMPLATE = "http://localhost:8182/api/x/{}/stream"
TIMEOUT = 300  # 5 minutes timeout for SSE connections
MAX_WORKERS = 10  # Maximum parallel SSE connections

SQL_TEMPLATE = """select
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), 'node=') as category,
  dom_first_slim_html(dom, '#bylineInfo') as brand,
  cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
  dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('{url} -i 20s -njr 3', 'body');"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Async web scraping with SSE")
    parser.add_argument("-f", dest="seeds_file", default=SEEDS_FILE, help="Seeds file path")
    parser.add_argument("-b", dest="batch_size", type=int, default=BATCH_SIZE, help="Batch size")
    parser.add_argument("-m", dest="max_urls", type=int, default=MAX_URLS, help="Maximum URLs to process")
    parser.add_argument("-t", dest="timeout", type=int, default=TIMEOUT, help="SSE connection timeout in seconds")
    parser.add_argument("-w", dest="max_workers", type=int, default=MAX_WORKERS, help="Maximum parallel workers")
    return parser.parse_args()


def extract_urls(seeds_file: str, max_urls: int) -> List[str]:
    try:
        with open(seeds_file, 'r') as file:
            content = file.read()
    except FileNotFoundError:
        print(f"[ERROR] {seeds_file} does not exist.")
        sys.exit(1)

    urls = re.findall(r'https?://[^\s/$.?#][^\s]*', content)
    urls = list(dict.fromkeys(urls))[:max_urls]  # Remove duplicates and limit count

    if not urls:
        print(f"[ERROR] No valid HTTP(S) links found in {seeds_file}.")
        sys.exit(1)

    print(f"[INFO] Extracted {len(urls)} valid links.")
    return urls


def submit_task(url: str) -> str:
    sql = SQL_TEMPLATE.replace('{url}', url)
    try:
        response = requests.post(BASE_URL, data=sql, headers={"Content-Type": "text/plain"})
        response.raise_for_status()
        return response.text.strip()
    except requests.RequestException as e:
        print(f"[ERROR] Failed to submit task for {url}: {e}")
        return ""

def process_sse_stream(uuid: str, timeout: int) -> None:
    """
    Process a single SSE stream for a task, parsing events according to the SSE protocol
    and extracting ScrapeResponse data.
    """
    url = SSE_URL_TEMPLATE.format(uuid)
    print(f"[INFO] Connecting to SSE stream for task {uuid}...")

    start_time = time.time()

    try:
        # Set up the connection with a streaming request
        response = requests.get(url, stream=True, timeout=60,
                               headers={"Accept": "text/event-stream"})
        response.raise_for_status()

        # Read the full response content
        full_content = response.content.decode('utf-8')

        # Split the response by double newlines (which separate SSE events)
        events = full_content.split('\n\n')

        for event in events:
            if not event.strip():
                continue

            # Extract data parts
            data_lines = []
            for line in event.split('\n'):
                if line.startswith('data:'):
                    data_lines.append(line[5:].strip())

            # Join all data lines to form the complete JSON
            if data_lines:
                event_data = ''.join(data_lines)

                try:
                    # Parse the ScrapeResponse JSON
                    data = json.loads(event_data)

                    # Format timestamp for log messages
                    timestamp = time.strftime('%H:%M:%S')

                    # Extract all relevant fields from ScrapeResponse
                    status = data.get("status", "Unknown")
                    status_code = data.get("statusCode", 0)
                    page_status = data.get("pageStatus", "Unknown")
                    page_status_code = data.get("pageStatusCode", 0)
                    page_content_bytes = data.get("pageContentBytes", 0)
                    is_done = data.get("isDone", False)

                    # Create a nicely formatted status message
                    status_msg = f"[{timestamp}] Task {uuid} - Status: {status} ({status_code})"
                    if page_status != "Unknown":
                        status_msg += f", Page: {page_status} ({page_status_code})"
                    if page_content_bytes > 0:
                        status_msg += f", Size: {page_content_bytes} bytes"

                    print(status_msg)

                    # Handle completion and results
                    if is_done:
                        result_set = data.get("resultSet", [])
                        if result_set:
                            result_size = len(result_set)
                            print(f"[SUCCESS] Task {uuid} completed with {result_size} results.")

                            # Print a sample of the first result
                            if result_size > 0:
                                sample = json.dumps(result_set[0], indent=2)
                                if len(sample) > 200:
                                    sample = sample[:200] + "..."
                                print(f"Sample result:\n{sample}")
                        else:
                            print(f"[COMPLETE] Task {uuid} completed with no results.")
                        break

                except json.JSONDecodeError as e:
                    print(f"[WARNING] Invalid JSON data in SSE stream: {e}")
                    print(f"Raw data: {event_data}")

    except requests.RequestException as e:
        print(f"[ERROR] SSE connection failed for task {uuid}: {e}")
    except Exception as e:
        print(f"[ERROR] Unexpected error processing SSE stream for task {uuid}: {str(e)}")
    finally:
        elapsed_time = time.time() - start_time
        print(f"[INFO] Closed SSE stream for task {uuid} after {elapsed_time:.1f} seconds")


def process_batch(batch: List[str], timeout: int, max_workers: int) -> None:
    """Process a batch of URLs by submitting tasks and monitoring SSE streams"""
    uuids = []
    for url in batch:
        if not re.match(r'^https?://', url):
            print(f"[WARNING] Invalid URL: {url}")
            continue

        uuid = submit_task(url)
        if uuid:
            print(f"Submitted: {url} -> Task UUID: {uuid}")
            uuids.append(uuid)

    if not uuids:
        print("[WARNING] No valid tasks submitted in this batch")
        return

    print(f"[INFO] Starting SSE streams for {len(uuids)} tasks...")

    # Process SSE streams in parallel using ThreadPoolExecutor
    with ThreadPoolExecutor(max_workers=min(max_workers, len(uuids))) as executor:
        futures = [executor.submit(process_sse_stream, uuid, timeout) for uuid in uuids]

        # Wait for all futures to complete
        for future in futures:
            future.result()  # This will re-raise any exceptions from the threads

    print(f"[INFO] Completed processing batch with {len(uuids)} tasks")


def main() -> None:
    args = parse_args()
    urls = extract_urls(args.seeds_file, args.max_urls)
    total = len(urls)

    for i in range(0, total, args.batch_size):
        batch = urls[i:i+args.batch_size]
        batch_num = i // args.batch_size + 1
        print(f"[INFO] Processing batch {batch_num} with {len(batch)} URLs...")
        process_batch(batch, args.timeout, args.max_workers)


if __name__ == "__main__":
    main()