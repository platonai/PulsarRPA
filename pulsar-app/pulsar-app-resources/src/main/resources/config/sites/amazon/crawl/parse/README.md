Scrape all entities in a product page

all X-SQLs can be find [here](sql/crawl).

    POST http://crawl0:8182/api/x/a/v2/q
    Content-Type: application/json

    {
      "authToken": "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c",
      "url": "https://item.jd.com/10023068375912.html",
      "args": "-i 0s",
      "sqls": {
        "asin": "{{x-asin.sql}}",
        "sims-1": "{{x-asin-sims-consolidated-1.sql}}",
        "sims-2": "{{x-asin-sims-consolidated-2.sql}}",
        "sims-3": "{{x-asin-sims-consolidated-3.sql}}",
        "sims-consider": "{{x-asin-sims-consider.sql}}",
        "top-reviews": "{{x-asin-top-reviews.sql}}"
      },
      "callbackUrl": "http://localhost:8182/api/hello/echo",
      "priority": "HIGHER2"
    }

Note: 

    1. replace authToken to your own authToken
    2. replace {{xxx.sql}} to be the real sql
    3. replace callbackUrl to your own callbackUrl
    4. x-similar-items.sql is another entity but not supported currently
