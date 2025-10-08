#!/bin/bash

curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "onBrowserLaunchedActions": [
  "clear browser cookies",
  "navigate to the home page",
  "click a random link"
  ],
  "onPageReadyActions": ["click #title", "scroll to the middle"],
  "pageSummaryPrompt": "Provide a brief introduction of this product.",
  "dataExtractionRules": "product name, price, and ratings",
  "uriExtractionRules": "all links containing /dp/ on the page"
}'

curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
"