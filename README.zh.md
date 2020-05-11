Pulsar 说明文档
===================
Pulsar 是一款面向非结构数据的智能数据处理系统，扩展 SQL 以支持数据处理全周期，涵盖数据采集，结构化，分析，存储和 BI 报表等。

![产品截图](docs/images/pulsar-product-screenshot-1.png)
![产品截图](docs/images/pulsar-product-screenshot-2.png)

# 主要特性
- X-SQL：扩展 SQL，覆盖大数据处理完整生命周期：采集，提取，转换，统计，机器学习、NLP、知识图谱以及 BI 报表
- 网络爬虫：浏览器渲染，Ajax，爬虫调度，页面评分，系统监控，高性能分布式，Solr/Elastic 索引
- BI 集成：无缝衔接 BI 套件。将原始的非结构数据转变为报表，并获得商业见解，仅需要一条或几条简单的 X-SQL
- 大数据：适应大规模的数据处理任务。分布式架构，支持 HBase/MongoDB 等多种底层存储

更多信息请访问 [platonic.fun](http://platonic.fun)

## Native API
启动一个会话：

    val session = PulsarContext.createSession()
    # specify a test url
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

采集单个网页：

    session.load(url, "-expires 1d")

从一个入口链接采集一组网页：

    session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")

采集并提取单个网页：

        val page = session.load(url, "-expires 1d")
        val document = session.parse(page)
        val products = document.select("li[data-sku]").map {
            it.selectFirstOrNull(".p-name em")?.text() to it.selectFirstOrNull(".p-price")?.text()
        }
        products.forEach { (name, price) -> println("$price $name") }

从一个入口链接采集页面并进行网页提取：

        val pages = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
        val documents = pages.map { session.parse(it) }
        val products = documents.mapNotNull { it.selectFirstOrNull(".product-intro") }.map {
            it.selectFirstOrNull(".sku-name")?.text() to it.selectFirstOrNull(".p-price")?.text()
        }
        products.forEach { (name, price) -> println("$price $name") }

上述例子可以在 [示例](pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/Manual.kt) 中找到。

## X-SQL

采集并提取一组网页，并将其转变为 SQL 表：

    SELECT
      DOM_FIRST_TEXT(DOM, '.sku-name') AS NAME,
      DOM_FIRST_TEXT(DOM, '.summary-price') AS PRICE,
      DOM_BASE_URI(DOM) AS URI,
      DOM_FIRST_IMG(DOM, '.main-img') AS MAIN_IMAGE,
      DOM_FIRST_IMG(DOM, '460x460') AS MAIN_IMAGE2,
      DOM_FIRST_TEXT(DOM, '.parameter2') AS PARAMETERS,
      DOM_FIRST_TEXT(DOM, '.comment-item') AS COMMENT1
    FROM LOAD_OUT_PAGES('https://list.jd.com/list.html?cat=652,12345,12349', 'a[href~=item]', 1, 20)
    WHERE LOCATE('item', DOM_BASE_URI(DOM)) > 0;

你可以下载 pulsar 源代码自己运行上述 X-SQL 或者通过我们的[在线演示版](http://bi.platonic.fun/question/65)运行。

文件 [sql-history.sql](sql-history.sql) 中包含了所有测试用的X-SQL。
更多 X-SQL 函数可以在 [ai.platon.pulsar.ql.h2.udfs](pulsar-ql-server/src/main/kotlin/ai/platon/pulsar/ql/h2/udfs) 目录下找到。

## BI 集成
使用定制的 [Metabase](https://github.com/platonai/metabase) 编写 X-SQL 并将网页转变为报表。

现在，您公司里的每个人都可以提出有价值的问题，并立即使用互联网数据作出回答。

# Build & Run
## 安装依赖项
    bin/tools/install-depends.sh

## 安装 MongoDB
    这一步不是必须的，但如果没有一个后端存储，Pulsar 关闭后将不会保留任何数据。
    在 Ubuntu/Debian 系统上：

        sudo apt-get install mongodb

## 从源代码构建
    git clone https://github.com/platonai/pulsar.git
    cd pulsar && mvn -Pthird -Pplugins
## 启动 pulsar 服务器
    bin/pulsar
## 使用 Web 控制台
Web console [http://localhost:8082](http://localhost:8082) is already open in your browser now, enjoy playing with Web SQL.

### 使用 Metabase
下载 [Metabase](https://github.com/platonai/metabase) 定制版, 然后执行:

    git clone https://github.com/platonai/metabase.git
    cd metabase
    bin/build && bin/start

# 大规模网络爬虫
从种子链接出发抓取大规模文本网页，解析后使用 Solr 建立索引：

    -- coming soon ..
    bin/crawl.sh default false awesome_crawl_task http://localhost:8983/solr/awesome_crawl_task/ 1

# 企业版:

Pulsar 企业版提供了更多高级特性：

## 自动网页挖掘（Auto Web Mining）
使用高级信息处理和机器学习技术，将网页精确还原为表格，不需要配置规则，也不需要单独训练。

您可以通过我们的[人工智能网页收割演示](http://bi.platonic.fun/dashboard/20)来体验自动网页挖掘的强大能力。
