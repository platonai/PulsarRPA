用html,css,javascript创建一个单网页，提供一个输入框，用户输入一段文本后，点击提交，向服务器发送一个REST API请求，服务器返回一个uuid，网页用这个uuid轮询执行结果。

请求如下：

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    Go to https://www.amazon.com/dp/B0C1H26C46
    After page load: click #title, then scroll to the middle.
    
    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
  '
```

返回示例：


```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B0C1H26C46",
    "pageSummaryPrompt": "Provide a brief introduction of this product.",
    "dataExtractionRules": "product name, price, and ratings",
    "linkExtractionRules": "all links containing `/dp/` on the page",
    "onPageReadyActions": ["click #title", "scroll to the middle"]
  }'
```
