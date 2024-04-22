控制台
=

[PulsarRPAPro](https://github.com/platonai/PulsarRPAPro) ，[国内镜像](https://gitee.com/platonai_galaxyeye/PulsarRPAPro)，代表奇异星（Exotic Star），是 PulsarRPA 的专业版和控制台，它包含：

- 一个**命令行工具**，可以从命令行直接执行网页数据采集任务，而不需要打开 IDE 写代码
- 一个 **Web 控制台**，方便我们管理 PulsarRPA 服务，并编写 SQL 来访问 Web，提取数据
- 一个升级版的 PulsarRPA 服务器，我们可以在客户端编写 SQL 来访问 Web，提取数据
- 一个基于 自监督机器学习 自动进行信息提取的小程序，AI 算法识别详情页的所有字段，95% 以上字段精确度 99% 以上
- 一个基于 自监督机器学习 自动学习并输出所有采集规则的小程序
- 一组顶尖站点的数据采集示例

**PulsarRPAPro 可以从网站学习，自动生成所有提取规则，将 Web 当作数据库进行查询，完整精确地交付规模化的 Web 数据：**

1. 步骤1：使用高级人工智能自动提取网页中的每个字段，并生成提取 SQL
2. 步骤2：测试 SQL，并在必要时改进它们以匹配前端业务需求
3. 步骤3：在 Web 控制台中创建调度规则，以连续运行 SQL 并下载所有 Web 数据，从而推动您的业务向前发展

## 运行 Exotic 服务器并打开控制台

你可以选择直接下载可执行 jar 包或者从源代码构建 PulsarRPAPro。

下载最新的可执行 jar 包：

```bash
wget http://static.platonic.fun/repo/ai/platon/exotic/exotic-standalone.jar
```

从源代码构建：

```bash
git clone https://github.com/platonai/PulsarRPAPro.git
cd PulsarRPAPro
mvn clean && mvn
cd exotic-standalone/target/
```

运行服务器并打开 Web 控制台：

```bash
# Linux:
java -jar exotic-standalone*.jar serve

# Windows:
java -jar exotic-standalone[-the-actual-version].jar serve
```

注意:如果您在 Windows 上使用 CMD 或 PowerShell，您可能需要删除通配符 * 并使用 jar 包的全名。

如果 PulsarRPAPro 在 GUI 模式下运行，Web 控制台应该在几秒钟内打开，或者您可以手动打开它：

http://localhost:2718/exotic/crawl/

## 执行自动提取

我们可以使用 harvest 命令，使用无监督的机器学习从一组项目页面中学习：

```bash
java -jar exotic-standalone*.jar harvest https://www.hua.com/flower/ -diagnose -refresh
```

## 使用生成的 SQL 采集页面

Harvest 命令使用无监督的机器学习自动提取字段，并为所有可能的字段和提取 SQL 生成最佳 CSS 选择器。我们可以使用 sql 命令来执行 SQL。

请注意，本演示中的网站使用了 **CSS 混淆**技术，因此 CSS 选择器**很难阅读并且经常改变**。除了基于机器学习的解决方案之外，**没有其他有效的技术**来解决这个问题。

```bash
# Note: remove the wildcard `*` and use the full name of the jar on Windows
java -jar exotic-standalone*.jar sql "
select
    dom_first_text(dom, 'div.-Esc+w.card.product-briefing div.HLQqkk div.flex-column.imEX5V span') as T1C2,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.W2tD8- div.MrYJVA.Ga-lTj') as T1C3,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.W2tD8- div.MrYJVA') as T1C4,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.W2tD8- div.Wz7RdC') as T1C5,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.W2tD8- div._45NQT5') as T1C6,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.W2tD8- div.Cv8D6q') as T1C7,
    dom_first_text(dom, 'div.-Esc+w.card.product-briefing div.HLQqkk div.imEX5V div.pmmxKx') as T1C8,
    dom_first_text(dom, 'div.-Esc+w.card.product-briefing div.HLQqkk div.imEX5V div.mini-vouchers__label') as T1C9,
    dom_first_text(dom, 'div.imEX5V div.PMuAq5 div.flex-no-overflow span.voucher-promo-value.voucher-promo-value--absolute-value') as T1C10,
    dom_first_text(dom, 'div.HLQqkk div.imEX5V div.PMuAq5 label._0b8hHE') as T1C11,
    dom_first_text(dom, 'div.PMuAq5 div.MGNOw3.hInOdW div.dHS5e4.xIMb1R div.LgUWja') as T1C12,
    dom_first_text(dom, 'div.PMuAq5 div.MGNOw3.hInOdW div.dHS5e4.xIMb1R div.Nd79Ux') as T1C13,
    dom_first_text(dom, 'div.MGNOw3.hInOdW div.dHS5e4.xIMb1R div.flex-row div.NPdOlf') as T1C14,
    dom_first_text(dom, 'div.imEX5V div.PMuAq5 div.-+gikn.hInOdW label._0b8hHE') as T1C15,
    dom_first_text(dom, 'div.PMuAq5 div.-+gikn.hInOdW div.items-center button.product-variation') as T1C16,
    dom_first_text(dom, 'div.PMuAq5 div.-+gikn.hInOdW div.items-center button.product-variation') as T1C17,
    dom_first_text(dom, 'div.imEX5V div.PMuAq5 div.-+gikn.hInOdW div._0b8hHE') as T1C18,
    dom_first_text(dom, 'div.PMuAq5 div.-+gikn.hInOdW div.G2C2rT.items-center div') as T1C19,
    dom_first_text(dom, 'div.flex-column.imEX5V div.vdf0Mi div.OozJX2 span') as T1C20,
    dom_first_text(dom, 'div.HLQqkk div.flex-column.imEX5V div.vdf0Mi button.btn.btn-solid-primary.btn--l.GfiOwy') as T1C21,
    dom_first_text(dom, 'div.-Esc+w.card.product-briefing div.HLQqkk div.flex-column.imEX5V span.zevbuo') as T1C22,
    dom_first_text(dom, 'div.-Esc+w.card.product-briefing div.HLQqkk div.flex-column.imEX5V span') as T1C23
from load_and_select('https://shopee.sg/(Local-Stock)-(GEBIZ-ACRA-REG)-PLA-3D-Printer-Filament-Standard-Colours-Series-1.75mm-1kg-i.182524985.8326053759?sp_atk=3afa9679-22cb-4c30-a1db-9d271e15b7a2&xptdk=3afa9679-22cb-4c30-a1db-9d271e15b7a2', 'div.page-product');
"
```

## 探索 PulsarRPAPro 的其他能力

直接运行可执行的 jar 包来获得帮助，以探索所提供的更多功能：

```bash
# Note: remove the wildcard `*` and use the full name of the jar on Windows
java -jar exotic-standalone*.jar
```

该命令将打印帮助信息：

```
Usage: java -jar exotic-standalone*.jar [options] harvest <url> [args...]
           (to harvest webpages automatically using our advanced AI)
   or  java -jar exotic-standalone*.jar [options] scrape <url> [args...]
           (to scrape a webpage or a batch of webpages)
   or  java -jar exotic-standalone*.jar [options] sql <sql>
           (to execute a X-SQL)
   or  java -jar exotic-standalone*.jar [options] serve
           (to run the standalone server: both the REST server and the web console)

Arguments following the urls are passed as the arguments for harvest or scrape methods.

where options include:
    -headless       to run browser in headless mode
    -? -h -help
                    print this help message to the error stream
    --help [topic [-v|-verbose]]
                    print this help message to the output stream, or print help message for topic
                    the topic can be one of: [harvest|scrape|SQL], case insensitive
```

以及最典型的示例：

```
Examples:

# harvest automatically with diagnosis
    java -jar exotic-standalone*.jar harvest https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ -diagnose

# arrange links
    java -jar exotic-standalone*.jar arrange https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/

# harvest automatically with page component specified
    java -jar exotic-standalone*.jar harvest https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ -outLink a[href~=/dp/] -component "#centerCol" -component "#buybox"

# scrape specified fields in a single page
    java -jar exotic-standalone*.jar scrape https://www.amazon.com/dp/B0C1H26C46 -field "#productTitle" -field "#acrPopover" -field "#acrCustomerReviewText" -field "#askATFLink"

# scrape specified fields from out pages
    java -jar exotic-standalone*.jar scrape https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ -outLink a[href~=/dp/] -field "#productTitle" -field "#acrPopover" -field "#acrCustomerReviewText" -field "#askATFLink"
```

------

[上一章](15REST.md) [目录](1home.md) [下一章](17top-practice.md)
