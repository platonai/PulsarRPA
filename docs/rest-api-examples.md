# RES API Examples

## 💬 For Beginners - Perform Actions and Extract Data

Use the `command` API to perform actions and extract data from a webpage:

### Summary

   ```shell
   curl -X POST "http://localhost:8182/api/ai/command" -H "Content-Type: text/plain" -d '
     Visit https://www.amazon.com/dp/B0C1H26C46
     Summarize the product.
   '
   ```

### Summary with Actions

   ```shell
   curl -X POST "http://localhost:8182/api/ai/command" -H "Content-Type: text/plain" -d '
     Visit https://www.amazon.com/dp/B0C1H26C46
     Summarize the product.
     After page load: click #title, then scroll to the middle.
   '
   ```

### Summary And Extract Data

   ```shell
   curl -X POST "http://localhost:8182/api/ai/command" -H "Content-Type: text/plain" -d '
     Visit https://www.amazon.com/dp/B0C1H26C46
     Summarize the product.
     Extract: product name, price, ratings.
   '
   ```

### Summary, Extract Data, Find Links, and Perform Actions

   ```shell
   curl -X POST "http://localhost:8182/api/ai/command" -H "Content-Type: text/plain" -d '
     Visit https://www.amazon.com/dp/B0C1H26C46
     Summarize the product.
     Extract: product name, price, ratings.
     Find all links containing /dp/.
     After page load: click #title, then scroll to the middle.
   '
   ```

### Use a stricter JSON-based version

   ```shell
   curl -X POST "http://localhost:8182/api/ai/command" -H "Content-Type: application/json" -d '{
     "url": "https://www.amazon.com/dp/B0C1H26C46",
     "pageSummaryPrompt": "Provide a brief introduction of this product.",
     "dataExtractionRules": "product name, price, and ratings",
     "linkExtractionRules": "all links containing `/dp/` on the page",
     "onPageReadyActions": ["click #title", "scroll to the middle"]
   }'
   ```

## 💬 Free Chat with AI

Use the `chat` API to ask any questions:

```shell
curl http://localhost:8182/api/ai/chat?prompt=What-is-the-most-fantastical-technology-today
```

Use `post` method to send a longer prompt:

```shell
curl -X POST "http://localhost:8182/api/ai/chat" -H "Content-Type: application/json" -d '
What is the most fantastical technology today?
You should return a list of 5 items.
'
```

> 💡 **Tip:** You can use free chat to check if the LLM works well.

## 🎓 For Advanced Users - LLM + X-SQL

```bash
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
    llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
    dom_base_uri(dom) as url,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

The extracted data:
```json
{
  "llm_extracted_data": {
    "product name": "Apple iPhone 15 Pro Max",
    "price": "$1,199.00",
    "ratings": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

## 🔍 Complex Data Extraction with X-SQL:


```sql
select
    llm_extract(dom, 'product name, price, ratings, score') as llm_extracted_data,
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

📚 Example Code:
* [Amazon Product Page Scraping (100+ fields)](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [All Amazon Page Types Scraping](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
