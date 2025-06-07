import requests
import time
import argparse
import re
from collections import deque

SEEDS_FILE = "seeds.txt"
BATCH_SIZE = 5
MAX_ATTEMPTS = 30
DELAY_SECONDS = 5
MAX_URLS = 20
BASE_URL = "http://localhost:8182/api/scrape/tasks/submit"
STATUS_URL_TEMPLATE = "http://localhost:8182/api/scrape/tasks/{}/status"
MAX_POLLING_TASKS = 10

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
from load_and_select('{url} -i 20s -njr 3', 'body');
"""

def search_seeds_file() -> str:
    """
    The seeds file can be: seeds.txt, bin/seeds.txt, ../seeds.txt, ../bin/seeds.txt
    """
    for path in ["seeds.txt", "bin/seeds.txt", "../seeds.txt", "../bin/seeds.txt"]:
        if os.path.exists(path):
            return path
    raise FileNotFoundError("No seeds file found in the expected locations.")

def extract_urls(seeds_file, max_urls):
    with open(seeds_file, encoding="utf-8") as f:
        content = f.read()
    urls = re.findall(r'https?://[^\s/$.?#][^\s]*', content)
    seen = set()
    deduped = []
    for url in urls:
        if url not in seen:
            seen.add(url)
            deduped.append(url)
        if len(deduped) >= max_urls:
            break
    if not deduped:
        raise Exception(f"No valid HTTP(S) links found in {seeds_file}.")
    print(f"[INFO] Extracted {len(deduped)} valid links.")
    return deduped

def submit_task(url):
    sql = SQL_TEMPLATE.format(url=url)
    resp = requests.post(BASE_URL, data=sql.encode("utf-8"), headers={"Content-Type": "text/plain"})
    return resp.text.strip()

def poll_tasks(uuids):
    task_attempts = {uuid: 0 for uuid in uuids}
    pending = deque(uuids)
    for attempt in range(MAX_ATTEMPTS):
        if not pending:
            print("[INFO] All tasks in this batch completed.")
            return
        time.sleep(DELAY_SECONDS)
        batch = [pending.popleft() for _ in range(min(MAX_POLLING_TASKS, len(pending)))]
        new_pending = []
        for uuid in batch:
            print(f"[INFO] Polling for task {uuid}...")
            status_url = STATUS_URL_TEMPLATE.format(uuid)
            try:
                resp = requests.get(status_url, timeout=10)
                if any(x in resp.text for x in ["completed", "success", "OK"]):
                    print(f"[SUCCESS] Task {uuid} completed.")
                else:
                    task_attempts[uuid] += 1
                    print(f"[PENDING] Task {uuid} still in progress (Attempt {task_attempts[uuid]}).")
                    new_pending.append(uuid)
            except Exception as e:
                print(f"[ERROR] Polling {uuid} failed: {e}")
                new_pending.append(uuid)
        pending.extend(new_pending)
        if new_pending:
            print(f"[INFO] Still waiting on {len(new_pending)} tasks in this batch...")
    if pending:
        print("[WARNING] Timeout reached. The following tasks are still pending:")
        for uuid in pending:
            print(uuid)

def process_batch(batch):
    uuids = []
    for url in batch:
        if not url.startswith("http"):
            print(f"[WARNING] Invalid URL: {url}")
            continue
        uuid = submit_task(url)
        print(f"Submitted: {url} -> Task UUID: {uuid}")
        uuids.append(uuid)
    print(f"[INFO] Started polling for {len(uuids)} tasks...")
    poll_tasks(uuids)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", default=SEEDS_FILE, help="Seeds file")
    parser.add_argument("-b", type=int, default=BATCH_SIZE, help="Batch size")
    parser.add_argument("-a", type=int, default=MAX_ATTEMPTS, help="Max attempts")
    parser.add_argument("-d", type=int, default=DELAY_SECONDS, help="Delay seconds")
    parser.add_argument("-m", type=int, default=MAX_URLS, help="Max URLs")
    args = parser.parse_args()

    global SEEDS_FILE, BATCH_SIZE, MAX_ATTEMPTS, DELAY_SECONDS, MAX_URLS
    SEEDS_FILE = args.f
    BATCH_SIZE = args.b
    MAX_ATTEMPTS = args.a
    DELAY_SECONDS = args.d
    MAX_URLS = args.m

    urls = extract_urls(SEEDS_FILE, MAX_URLS)
    total = len(urls)
    for i in range(0, total, BATCH_SIZE):
        batch = urls[i:i+BATCH_SIZE]
        batch_num = i // BATCH_SIZE + 1
        print(f"[INFO] Processing batch {batch_num} with {len(batch)} URLs...")
        process_batch(batch)

if __name__ == "__main__":
    main()
