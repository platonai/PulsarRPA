<?php

$url = "http://localhost:8182/x/e";
$sql = `select
            dom_first_text(dom, '#productTitle') as title,
            dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
            dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as price,
            array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as categories,
            dom_base_uri(dom) as baseUri
        from
            load_and_select('https://www.amazon.com/dp/B00BTX5926', ':root')`;

$headerArray = array("Content-type: text/plain");
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headerArray);
$output = curl_exec($ch);
curl_close($ch);

print_r($output);
