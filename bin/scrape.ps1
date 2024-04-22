#!/usr/bin/env pwsh
# Usage: .\bin\scrape.ps1

Write-Output ""

$sql = @"
select
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), 'node=') as category,
  dom_first_slim_html(dom, '#bylineInfo') as brand,
  cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
  dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46 -i 20s -njr 3', 'body');
"@

# Commented out since PowerShell doesn't support 'echo' command
# Write-Output $sql

Invoke-RestMethod -Method POST -Uri "http://localhost:8182/api/x/e" -Headers @{"Content-Type" = "text/plain"} -Body $sql

Write-Output ""
