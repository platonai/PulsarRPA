Console
=

[PulsarRPAPro](https://github.com/platonai/PulsarRPAPro), [domestic mirror](https://gitee.com/platonai_galaxyeye/PulsarRPAPro), includes:

- A **command-line tool** that can directly execute web data collection tasks from the command line without the need to open an IDE to write code.
- A **Web console** that facilitates the management of PulsarRPA services and writing SQL to access the Web and extract data.
- An upgraded PulsarRPA server where we can write SQL on the client side to access the Web and extract data.
- A mini-program based on self-supervised machine learning for automatic information extraction, where the AI algorithm identifies all fields on the detail page with over 95% of fields having an accuracy of over 99%.
- A mini-program based on self-supervised machine learning that automatically learns and outputs all collection rules.
- A set of data collection examples from top-tier sites.

**Exotic can learn from websites, automatically generate all extraction rules, treat the Web as a database for querying, and deliver scalable Web data in a complete and precise manner:**

1. Step 1: Use advanced artificial intelligence to automatically extract each field from web pages and generate extraction SQL.
2. Step 2: Test SQL and improve them as necessary to match front-end business requirements.
3. Step 3: Create scheduling rules in the Web console to continuously run SQL and download all Web data, thus driving your business forward.

## Running Exotic Server and Opening the Console

You can choose to directly download the executable jar package or build Exotic from the source code.

Download the latest executable jar package:

```bash
wget http://static.platonic.fun/repo/ai/platon/exotic/exotic-standalone.jar
```

Build from source code:

```bash
git clone https://github.com/platonai/PulsarRPAPro.git
cd PulsarRPAPro
mvn clean && mvn
cd exotic-standalone/target/
```

Run the server and open the Web console:

```bash
# Linux:
java -jar exotic-standalone*.jar serve

# Windows:
java -jar exotic-standalone[-the-actual-version].jar serve
```

Note: If you are using CMD or PowerShell on Windows, you may need to remove the wildcard * and use the full name of the jar package.

If PulsarRPAPro is running in GUI mode, the Web console should open within a few seconds, or you can manually open it:

http://localhost:2718/exotic/crawl/

## Performing Auto Extraction

We can use the harvest command to learn from a set of item pages using unsupervised machine learning:

```bash
java -jar exotic-standalone*.jar harvest https://www.hua.com/flower/  -diagnose -refresh
```

## Collecting Pages Using Generated SQL

The Harvest command uses unsupervised machine learning to automatically extract fields and generate the best CSS selectors for all possible fields and extraction SQL. We can use the sql command to execute SQL.

Please note that the website in this demonstration uses **CSS obfuscation** technology, so the CSS selectors are **difficult to read and often change**. Apart from machine learning-based solutions, **no other effective technology** can solve this problem.

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
from load_and_select('https://shopee.sg/(Local-Stock)-(GEBIZ-ACRA-REG)-PLA-3D-Printer-Filament-Standard-Colours-Series-1.75mm-1kg-i.182524985.8326053759?sp_atk=3afa9679-22cb-4c30-a1db-9d271e15b7a2&xptdk=3afa9679-22cb-4c30-a1db-9d271e15b7a2',  'div.page-product');
```

## Exploring More Capabilities of PulsarRPAPro

Run the executable jar package directly for help to explore more provided features:

```bash
# Note: remove the wildcard `*` and use the full name of the jar on Windows
java -jar exotic-standalone*.jar
```

The command will print help information:

```
Usage: java -jar exotic-standalone*.jar [options] harvest <url> [args...]
           (to harvest webpages automatically using our advanced AI)
   or  java -jar exotic-standalone*.jar [options] scrape <url> [args...]
           (to scrape a webpage or a batch ofwebpages)
   or  java -jar exotic-standalone*.jar [options] sql <sql>
           (to execute a X-SQL)
   or  java -jar exotic-standalone*.jar [options] serve
           (to run the standalone server: both the REST server and the web console)

Arguments following the urls are passed as the arguments for harvest or scrape methods.

where options include:
    -headless       to run browser in headless mode
    -? -h -help
                    print this help message to the error stream
    --help [topic [-v|-verbose])]
                    print this help message to the output stream, or print help message for topic
                    the topic can be one of: [harvest|scrape|SQL], case insensitive
```

And the most typical examples:

```
Examples:

# harvest automatically with diagnosis
    java -jar exotic-standalone*.jar harvest https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/  -diagnose

# arrange links
    java -jar exotic-standalone*.jar arrange https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ 

# harvest automatically with page component specified
    java -jar exotic-standalone*.jar harvest https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/  -outLink a[href~=/dp/] -component "#centerCol" -component "#buybox"

# scrape specified fields in a single page
    java -jar exotic-standalone*.jar scrape https://www.amazon.com/dp/B0C1H26C46  -field "#productTitle" -field "#acrPopover" -field "#acrCustomerReviewText" -field "#askATFLink"

# scrape specified fields from out pages
    java -jar exotic-standalone*.jar scrape https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/  -outLink a[href~=/dp/] -field "#productTitle" -field "#acrPopover" -field "#acrCustomerReviewText" -field "#askATFLink"
```

------

[Prev](15REST.md) [Home](1home.md) [Next](17top-practice.md)