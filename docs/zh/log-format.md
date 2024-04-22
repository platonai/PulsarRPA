# Pulsar 日志格式解释

PulsarRPA 精心设计了日志和指标子系统，以记录系统中发生的每个事件。本文档解释了典型日志的格式。

PulsarRPA 将所有日志分割成几个独立的文件：

```
logs/pulsar.log    - 默认日志
logs/pulsar.pg.log - 主要报告加载/获取任务的状态
logs/pulsar.m.log  - 指标
```

加载任务的状态是主要关注点。您只需注意几个符号：💯 💔 🗙 ⚡💿 🔃🤺，就可以洞察整个系统的状态。

**加载任务日志解释**

以下是5个报告已加载任务状态的示例日志：

```
2022-09-24 11:46:26.045  INFO [-worker-14] a.p.p.c.c.L.Task - 3313. 💯 ⚡ U for N got 200 580.92 KiB in 1m14.277s, fc:1 | 75/284/96/277/6554 | 106.32.12.75 | 3xBpaR2 | https://www.walmart.com/ip/Restored-iPhone-7-32GB-Black-T-Mobile-Refurbished/329207863   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:09.190  INFO [-worker-32] a.p.p.c.c.L.Task - 3738. 💯 💿 U  got 200 452.91 KiB in 55.286s, last fetched 9h32m50s ago, fc:1 | 49/171/82/238/6172 | 121.205.220.179 | https://www.walmart.com/ip/Boost-Mobile-Apple-iPhone-SE-2-Cell-Phone-Black-64GB-Prepaid-Smartphone/490934488   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:28.567  INFO [-worker-17] a.p.p.c.c.L.Task - 2269. 💯 🔃 U for SC got 200 565.07 KiB <- 543.41 KiB in 1m22.767s, last fetched 16m58s ago, fc:6 | 58/230/98/295/6272 | 27.158.125.76 | 9uwu602 | https://www.walmart.com/ip/Straight-Talk-Apple-iPhone-11-64GB-Purple-Prepaid-Smartphone/356345388?variantFieldId=actual_color   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:18.390  INFO [r-worker-8] a.p.p.c.c.L.Task - 3732. 💔 ⚡ U for N got 1601 0 <- 0 in 32.201s, fc:1/1 Retry(1601) rsp: CRAWL, rrs: EMPTY_0B | 2zYxg52 | https://www.walmart.com/ip/Apple-iPhone-7-256GB-Jet-Black-AT-T-Locked-Smartphone-Grade-B-Used/182353175?variantFieldId=actual_color   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:13.860  INFO [-worker-60] a.p.p.c.c.L.Task - 2828. 🗙 🗙 U for SC got 200 0 <- 348.31 KiB <- 684.75 KiB in 0s, last fetched 18m55s ago, fc:2 | 34/130/52/181/5747 | 60.184.124.232 | 11zTa0r2 | https://www.walmart.com/ip/Walmart-Family-Mobile-Apple-iPhone-11-64GB-Black-Prepaid-Smartphone/209201965?athbdg=L1200   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
```

以下示例日志报告了一个正在重试的页面：

```
2022-09-24 11:46:12.167  INFO [-worker-62] a.p.p.c.i.S.Task - 3744. 🤺 Trying 2th 10s later | U  got 1601 0 <- 0 in 1m0.612s, last fetched 10s ago, fc:1/1 Retry(1601) rsp: CRAWL | https://www.walmart.com/ip/iPhone-7-128GB-Silver-Boost-Mobile-Used-Grade-B/662547852  
```

本文档解释了日志中的每个字段。

## 第一部分：由日志系统预定义的一般信息

```
日期       时间         日志级别  线程名称   日志名称
2022-09-24 11:46:12.167  INFO      [-worker-62] a.p.p.c.i.S.Task -
2022-09-24 11:46:09.190  INFO      [-worker-32] a.p.p.c.c.L.Task -
```

## 第二部分：页面加载状态

这部分包含了 PageId、TaskStatus、PageStatus、PageCategory、FetchReason、FetchCode、PageSize 和 FetchTime 等信息。

```
PageId    任务状态  页面状态  页面类别   获取原因     获取代码      页面大小                        获取时间
3313.     💯         ⚡           U            for N           got 200         580.92 KiB                     in 1m14.277s
3738.     💯         💿           U                            got 200         452.91 KiB                     in 55.286s
2269.     💯         🔃           U            for SC          got 200         565.07 KiB <- 543.41 KiB       in 1m22.767s
3732.     💔         ⚡           U            for N           got 1601        0 <- 0 in 32.201s
2828.     🗙          🗙           U            for SC          got 200          0 <- 348.31 KiB <- 684.75 KiB  in 0s
```

`PageId` 是 WebPage 对象的 ID，在进程范围内是唯一的。

`TaskStatus` 是一个 Unicode 符号，可以是以下之一：

- 💯 - 任务成功
- 💔 - 任务失败
- 🗙 - 任务已取消
- 🤺 - 任务正在重试

`PageStatus` 是一个 Unicode 符号，可以是以下之一：

- ⚡ - 页面首次从互联网获取
- 💿 - 页面从硬盘加载
- 🔃 - 页面从互联网更新
- 🗙 - 页面已取消且保持不变

`FetchReason` 指示为什么获取页面。原因可以是以下之一：

- 页面从未被获取
- 自上次获取以来页面已过期
- 应用了 -refresh 选项，因此页面应该刷新
- 页面被安排获取
- 上次获取失败并重试
- 上次获取的页面没有内容
- 上次获取的页面内容太小
- 上次获取的页面内容中缺失了所需字段
- 页面被临时移动

`FetchReason` 包含一个或两个字符，定义如下：

```
symbols[DO_NOT_FETCH] = ""
symbols[NEW_PAGE] = "N"
symbols[REFRESH] = "RR"
symbols[EXPIRED] = "EX"
symbols[SCHEDULED] = "SD"
symbols[RETRY] = "RT"
symbols[NO_CONTENT] = "NC"
symbols[SMALL_CONTENT] = "SC"
symbols[MISS_FIELD] = "MF"
symbols[TEMP_MOVED] = "TM"
symbols[UNKNOWN] = "U"
```

`FetchCode` 是描述获取阶段状态的数字，继承自标准 HTTP 错误代码，通常如下：

```
200 - 成功
1601 - 重试
```

所有可能的代码都在 `ProtocolStatusCodes.java` 中定义。

## 第三部分 - PrevFetchTime、FetchCount、FetchFailure、DOMStatistic、ProxyIP 和 PrivacyContext

```
PrevFetchTime               FetchCount        FetchFailure                           DOMStatistic         ProxyIP           PrivacyContext
                            fc:1 |                                                   75/284/96/277/6554 | 106.32.12.75    | 3xBpaR2
上次获取完成时间为 9小时32分钟50秒前,  fc:1 |                                                   49/171/82/238/6172 | 121.205.220.179
上次获取完成时间为 16分钟58秒前,    fc:6 |                                                   58/230/98/295/6272 | 27.158.125.76   | 9uwu602
                            fc:1/1            Retry(1601) rsp: CRAWL, rrs: EMPTY_0B                                       | 2zYxg52
上次获取完成时间为 18分钟55秒前,    fc:2 |                                                   34/130/52/181/5747 | 60.184.124.232  | 11zTa0r2
```

`PrevFetchTime` 是上次获取操作完成的时间。

`FetchCount` 是所有获取执行的次数，不包括已取消的获取。

`FetchFailure` 是上次获取执行失败的信息，如果成功则为空。

`DOMStatistic` 包含了使用真实浏览器中的 JavaScript 计算的 HTML 文档的简单统计信息，格式如下：

```
58/230/98/295/6272
58/230/98/295/6272 (i/a/nm/st/h)
```

其中：

- i: HTML 文档中的锚点数量
- a: 图片数量
- nm: 数字数量
- st: 小文本数量
- h: 文档的滚动高度（像素）

`DOMStatistic` 表示页面是否正确获取；一个完全加载的页面通常滚动高度超过 5000 像素，低于这个值的页面可能需要重新获取。

对于其他字段，如 ProxyIP 和 PrivacyContext，并不需要解释。

## 第四部分：任务 URL

```
URL
https://www.walmart.com/ip/329207863   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
https://www.walmart.com/ip/490934488   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
https://www.walmart.com/ip/356345388   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
https://www.walmart.com/ip/182353175   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
https://www.walmart.com/ip/209201965   -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
```

URL 字段是要获取的 URL，后面可以跟随加载参数或加载选项。详情请参阅 [加载选项](get-started/3load-options.md)。
