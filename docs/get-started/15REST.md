REST Service
=

When PulsarRPA runs as a REST service, X-SQL can be used to collect web pages or directly query web data from anywhere at any time, without the need to open an IDE. It's like an upgraded version of the Google search box: upgrading keyword queries to SQL queries.

Example:

```bash
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as category,
      dom_first_slim_html(dom, '#bylineInfo') as brand,
      cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
      dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
      dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
      dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
      str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1d -njr 3', 'body');");
```

Example code can be found here: [bash](../../bin/scrape.sh), [batch](../../bin/scrape.bat), [java](../../pulsar-client/src/main/java/ai/platon/pulsar/client/Scraper.java), [kotlin](../../pulsar-client/src/main/kotlin/ai/platon/pulsar/client/Scraper.kt), [php](../../pulsar-client/src/main/php/Scraper.php).

------

[Prev](14AI-extraction.md) [Home](1home.md) [Next](16console.md)