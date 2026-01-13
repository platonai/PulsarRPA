# Example Response

## Examples

Example 2: Open task - Ask Browser4 for Anything:
```shell
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Find the latest news about AI technology from https://techcrunch.com/
"
```
------------------------------------------------------------------------------
Example 3: Automation/Extraction Task - Zero Code:
```shell
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
    Go to https://www.amazon.com/dp/B08PP5MSVB

    After browser launch: clear browser cookies.
    After page load: scroll to the middle.

    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
"
```
------------------------------------------------------------------------------
Example 4: For Advanced Extract Task — LLM + X-SQL: Precise, Flexible, Powerful:
```shell
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
    Go to https://www.amazon.com/dp/B08PP5MSVB

    After browser launch: clear browser cookies.
    After page load: scroll to the middle.

    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.

    X-SQL:
    ```sql
    select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      dom_first_slim_html(dom, 'img:expr(width > 400)') as img
    from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
    ```
"
```

## Example 4

(base) PS D:\workspace\Browser4\Browser4-feat> curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
>>     Go to https://www.amazon.com/dp/B08PP5MSVB
>>
>>     After browser launch: clear browser cookies.
>>     After page load: scroll to the middle.
>>
>>     Summarize the product.
>>     Extract: product name, price, ratings.
>>     Find all links containing /dp/.
>>
>>     X-SQL:
>>     ```sql
>>     select
>>       dom_base_uri(dom) as url,
>>       dom_first_text(dom, '#productTitle') as title,
>>       dom_first_slim_html(dom, 'img:expr(width > 400)') as img
>>     from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
>>     ```
>> "
{
"commandResult" : {
"fields" : {
"product name" : "苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)",
"price" : "",
"ratings" : "4.1 颗星，最多 5 颗星 (33,522)"
},
"pageSummary" : "该产品为苹果 iPhone 12（完全解锁翻新版），具体型号为64GB、黑色，可访问Amazon Renewed品牌旗舰店查看。其评分为4.1颗星（基于33,522条评价），另有128GB、256GB尺寸及白色等颜色选项。需注意，当前该商品无法配送至中国大陆，建议选择其他配送地点或查看类似商品。",
"xsqlResultSet" : [ {
"url" : "https://www.amazon.com/dp/B08PP5MSVB",
"title" : "苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)",
"img" : "<img src='https://m.media-amazon.com/images/I/51fYXSnSu9L._AC_SY879_.jpg' vi='165.6 513.4 467.4 635' alt='苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)'/>"
} ]
},
"createTime" : "2026-01-13T04:58:51.878850200Z",
"done" : true,
"event" : "fields",
"finishTime" : "2026-01-13T05:01:05.762337400Z",
"id" : "5234cc91-8ba4-45e5-862a-ed1428a4f7e9",
"instructResults" : [ {
"name" : "pageSummary",
"statusCode" : 200,
"result" : "该产品为苹果 iPhone 12（完全解锁翻新版），具体型号为64GB、黑色，可访问Amazon Renewed品牌旗舰店查看。其评分为4.1颗星（基于33,522条评价），另有128GB、256GB尺寸及白色等颜色选项。需注意，当前该商品无法配送至中国大陆，建议选择其他配送地点或查看类似商品。",
"resultType" : "string"
}, {
"name" : "fields",
"statusCode" : 200,
"result" : {
"product name" : "苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)",
"price" : "",
"ratings" : "4.1 颗星，最多 5 颗星 (33,522)"
},
"resultType" : "map"
} ],
"lastModifiedTime" : "2026-01-13T05:01:05.762337400Z",
"message" : "created,textContent,pageSummary,fields",
"pageContentBytes" : 2108253,
"pageStatus" : "OK",
"pageStatusCode" : 200,
"state" : "done",
"status" : "OK",
"statusCode" : 200
}

## Example 5 - run-sse.ps1 script output

(base) PS D:\workspace\Browser4\Browser4-feat> .\bin\tests\test-cases\run-sse.ps1
Sending command to server...
Command ID: ed875bfe-c526-401c-8f30-1427f176cb6d
Connecting to SSE stream...
Reading SSE stream...
SSE update: {
SSE update: "createTime" : "2026-01-12T16:44:31.961605400Z",
SSE update: "done" : false,
SSE update: "event" : "created",
SSE update: "id" : "ed875bfe-c526-401c-8f30-1427f176cb6d",
SSE update: "instructResults" : [ ],
SSE update: "lastModifiedTime" : "2026-01-12T16:44:31.964588Z",
SSE update: "message" : "created",
SSE update: "pageContentBytes" : 0,
SSE update: "pageStatus" : "Created",
SSE update: "pageStatusCode" : 201,
SSE update: "state" : "in_progress",
SSE update: "status" : "Processing",
SSE update: "statusCode" : 102
SSE update: }
SSE update: {
SSE update: "commandResult" : {
SSE update: "fields" : {
SSE update: "product name" : "苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)",
SSE update: "ratings" : "4.1 颗星，最多 5 颗星 (33,521)"
SSE update: },
SSE update: "links" : [ "https://www.amazon.com/-/zh/dp/B0CQRDRVND/ref=ex_alt_wg_d?_encoding=UTF8&pf_rd_r=TPZJKEVCTK6YGZJ26EMK&pf_rd_p=4e1b46a8-daf9-4433-b97e-d6df97cf3699&pd_rd_i=B0CQRDRVND&psc=1&pd_rd_w=qeuj8&pd_rd_wg=q6Lcn&pd_rd_r=f82226cb-b27d-4e2f-b2ab-1fcf2264b66d&content-id=amzn1.sym.4e1b46a8-daf9-4433-b97e-d6df97cf3699", "https://www.amazon.com/-/zh/dp/B07ZPKZSSC/ref=vse_cards_0?_encoding=UTF8&pd_rd_w=I21S4&content-id=amzn1.sym.26dedd2b-540b-4bff-af75-7fea8d8f8913&pf_rd_p=26dedd2b-540b-4bff-af75-7fea8d8f8913&pf_rd_r=TPZJKEVCTK6YGZJ26EMK&pd_rd_wg=XNhL4&pd_rd_r=ee1d5db1-048a-443c-bc3c-58eb835b48a4", "https://www.amazon.com/-/zh/dp/B0CHTVMXZJ?th=1?ref_=footer_reload_us", "https://www.amazon.com/-/zh/Apple-iPhone-12-64GB-Black/dp/B08PP5MSVB" ],
SSE update: "pageSummary" : "该产品为苹果iPhone 12（更新/升级版，来自Amazon Renewed），提供64GB、128GB、256GB存储选项，颜色可选黑色/白色，支持解锁及AT&T、Boost Mobile、T-Mobile等多种运营商。设备搭载iOS 16操作系统，RAM内存8GB，评分为4.1星（基于33,521条评价）。需注意，当前所选配送地点无法配送该商品，建议选择其他配送地点。"
SSE update: },
SSE update: "createTime" : "2026-01-12T16:44:31.961605400Z",
SSE update: "done" : true,

Task completed! Fetching final result...

=== FETCHING FINAL RESULT ===

=== FINAL RESULT ===
Command ID: ed875bfe-c526-401c-8f30-1427f176cb6d
Timestamp: 2026-01-13 00:48:50 UTC
Status: COMPLETED

Structured Result:
{
"fields": {
"product name": "苹果 iPhone 12,64GB,黑色 - 完全解锁(续订)",
"ratings": "4.1 颗星，最多 5 颗星 (33,521)"
},
"links": [
"https://www.amazon.com/-/zh/dp/B0CQRDRVND/ref=ex_alt_wg_d?_encoding=UTF8&pf_rd_r=TPZJKEVCTK6YGZJ26EMK&pf_rd_p=4e1b46a8-daf9-4433-b97e-d6df97cf3699&pd_rd_i=B0CQRDRVND&psc=1&pd_rd_w=qeuj8&pd_rd_wg=q6Lcn&pd_rd_r=f82226cb-b27d-4e2f-b2ab-1fcf2264b66d&content-id=amzn1.sym.4e1b46a8-daf9-4433-b97e-d6df97cf3699",
"https://www.amazon.com/-/zh/dp/B07ZPKZSSC/ref=vse_cards_0?_encoding=UTF8&pd_rd_w=I21S4&content-id=amzn1.sym.26dedd2b-540b-4bff-af75-7fea8d8f8913&pf_rd_p=26dedd2b-540b-4bff-af75-7fea8d8f8913&pf_rd_r=TPZJKEVCTK6YGZJ26EMK&pd_rd_wg=XNhL4&pd_rd_r=ee1d5db1-048a-443c-bc3c-58eb835b48a4",
"https://www.amazon.com/-/zh/dp/B0CHTVMXZJ?th=1?ref_=footer_reload_us",
"https://www.amazon.com/-/zh/Apple-iPhone-12-64GB-Black/dp/B08PP5MSVB"
],
"pageSummary": "该产品为苹果iPhone 12（更新/升级版，来自Amazon Renewed），提供64GB、128GB、256GB存储选项，颜色可选黑色/白色，支持解锁及AT&T、Boost Mobile、T-Mobile等多种运营商。设备搭载iOS 16操作系统，RAM内存8GB，评分为4.1星（基于33,521条评价）。需注意，当前所选配送地点无法配送该商品，建议选择其他配送地点。"
}

=== END OF RESULT ===
Cleaning up resources...

Finished run-sse.ps1 script.
