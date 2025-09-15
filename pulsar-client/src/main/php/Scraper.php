<?php

/**
 * A simple scraper which just send an X-SQL request to the pulsar server and get the scrape result
 * */

$url = "http://localhost:8182/api/x/e";
$sql = "select
            dom_first_text(dom, '#productTitle') as title,
            dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
            dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as price,
            array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as categories,
            dom_base_uri(dom) as baseUri
        from
            load_and_select('https://www.amazon.com/dp/B08PP5MSVB', ':root')";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: text/plain'));
# curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $sql);
$output = curl_exec($ch);
curl_close($ch);

# echo $output;
