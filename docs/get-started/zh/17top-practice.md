顶尖项目实战
=

[Exotic Amazon](https://github.com/platonai/exotic-amazon) （[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon)）是采集整个 amazon.com 网站的**完整解决方案**，**开箱即用**，包含亚马逊大多数数据类型，它将永久免费提供并开放源代码。

其他电商平台数据采集，其方法和流程基本类似，可以在该项目基础上修改调整业务逻辑即可，其基础设施解决了所有大规模数据采集面临的难题。

得益于 PulsarRPA 提供的完善的 Web 数据管理基础设施，整个解决方案由不超过 3500 行的 Kotlin 代码和不到 700 行的 X-SQL 组成，以提取 650 多个字段。

## 数据简介

- Best Seller - 每天更新，约 32,000 个类别，约 4,000,000 个产品记录
- Most Wished For - 每天更新约 25,000 个类别，约 3,500,000 个产品记录
- New Releases - 每天更新，约 25,000 个类别，约 3,000,000 条产品记录
- Movers and Shakers - 约 20 个类别，每小时更新一次
- Products - 约 20,000,000 个产品，每月更新
- 70 多个字段
- 标题、价格、库存、图像、描述、规格、店铺等
- 赞助产品、类似产品、相关产品等
- 热门评论等
- Review - 每天更新

## 开始

```bash
git clone https://github.com/platonai/exotic-amazon.git
cd exotic-amazon && mvn

java -jar target/exotic-amazon*.jar
# Or on Windows:
java -jar target/exotic-amazon-{the-actual-version}.jar
```

打开 [System Glances](http://localhost:8182/api/system/status/glances) 以一目了然地查看系统状态。

## 提取结果处理

### 提取规则

所有 [提取规则](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/)（[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/)）都是用 X-SQL 编写的。数据类型转换、数据清理也由强大的 X-SQL 内联处理，这也是我们开发 X-SQL 的重要原因。一个很好的 X-SQL 例子是 [x-asin.sql](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)（[国内镜像](https://gitee.com/platonai_galaxyeye/exotic-amazon/blob/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl/x-asin.sql)），它从每个产品页面中提取 70 多个字段。

### 将提取结果保存在本地文件系统中

默认情况下，结果以 json 格式写入本地文件系统。

### 将提取结果保存到数据库中

有几种方法可以将结果保存到数据库中：

1. 将结果序列化为键值对，并保存为 WebPage 对象的一个字段，WebPage 是整个系统的核心数据结构，这项特性也会默认开启
2. 将结果写入 JDBC 兼容的数据库，如 MySQL、PostgreSQL、MS SQL Server、Oracle 等
3. 自行编写几行代码，将结果保存到您希望的任何地方

------

[上一章](16console.md) [目录](1home.md) [下一章](18miscellaneous.md)
