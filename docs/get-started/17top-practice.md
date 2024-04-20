Top Project Practice
=

[Prev](16console.md) | [Home](1home.md) | [Next](18miscellaneous.md)

[Exotic Amazon](https://github.com/platonai/exotic-amazon) (Chinese mirror: [exotic-amazon](https://gitee.com/platonai_galaxyeye/exotic-amazon)) is a **complete solution** for crawling the entire amazon.com website, **ready to use out of the box**, containing most data types of Amazon, and it will be permanently provided for free and open source.

The methods and processes for data collection of other e-commerce platforms are basically similar. You can modify and adjust the business logic based on this project, and its infrastructure solves all the difficulties faced by large-scale data collection.

Thanks to the comprehensive Web data management infrastructure provided by PulsarRPA, the entire solution consists of no more than 3500 lines of Kotlin code and less than 700 lines of X-SQL to extract more than 650 fields.

## Data Introduction

- Best Seller - Updated daily, about 32,000 categories, about 4,000,000 product records
- Most Wished For - Updated daily, about 25,000 categories, about 3,500,000 product records
- New Releases - Updated daily, about 25,000 categories, about 3,000,000 product records
- Movers and Shakers - About 20 categories, updated every hour
- Products - About 20,000,000 products, updated monthly
- 70+ fields
- Titles, prices, inventory, images, descriptions, specifications, stores, etc.
- Sponsored products, similar products, related products, etc.
- Hot reviews, etc.
- Review - Updated daily

## Getting Started

```bash
git clone https://github.com/platonai/exotic-amazon.git
cd exotic-amazon && mvn

java -jar target/exotic-amazon*.jar
# Or on Windows:
java -jar target/exotic-amazon-{the-actual-version}.jar
```

Open [System Glances](http://localhost:8182/api/system/status/glances) to get a clear view of the system status.

## Handling Extraction Results

### Extraction Rules

All [extraction rules](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/) (Chinese mirror: [exotic-amazon](https://gitee.com/platonai_galaxyeye/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/)) are written in X-SQL. Data type conversion and data cleaning are also handled by powerful X-SQL inline processing, which is an important reason why we developed X-SQL. A good example of X-SQL is [x-asin.sql](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql) (Chinese mirror: [exotic-amazon](https://gitee.com/platonai_galaxyeye/exotic-amazon/blob/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)), which extracts more than 70 fields from each product page.

### Saving Extraction Results in the Local File System

By default, results are written in json format to the local file system.

### Saving Extraction Results to the Database

There are several ways to save results to the database:

1. Serialize the results as key-value pairs and save them as a field of the WebPage object, which is the core data structure of the entire system and this feature is also enabled by default.
2. Write the results to a JDBC-compatible database, such as MySQL, PostgreSQL, MS SQL Server, Oracle, etc.
3. Write a few lines of code to save the results to any destination you wish.

------

[Prev](16console.md) | [Home](1home.md) | [Next](18miscellaneous.md)
