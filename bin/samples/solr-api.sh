#!/usr/bin/env bash

echo "Do not run this script directly, run the specified commands alone in this script"

## Create a collection
bin/solr create -c information_tmp_local -n information -shards 2 -replicationFactor 2

# Clear the index
curl http://qiwur.com:8983/solr/information_tmp/update?commitWithin=3000 -d '{delete:{query:"*:*"}}'
curl http://qiwur.com:8983/solr/information_tmp/update?commitWithin=3000 -d '{delete:{query:"*:*"}}'

## General query
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d 'q=*:*&fl=id'

## 条件查询
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[2016-10-19T00:00:00.000Z TO NOW] AND article_title:["" TO *]&
fl=article_title,fetch_time_history,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　最近24小时发布，标题非空，作者非空
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW-1DAY/DAY TO NOW] AND article_title:["" TO *]　AND author:["" TO *] &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　最近24小时发布，标题非空，发布时间非空
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW-1DAY/DAY TO NOW] AND article_title:["" TO *]　AND publish_time:[* TO *] &
fl=publish_time&
sort=publish_time desc&
TZ=Asia/Shanghai&indent=on&wt=json&rows=1000'

#　最近24小时发布
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=text:"2016年" AND last_crawl_time:[NOW-1DAY/DAY TO *] AND article_title:["" TO *]　&
fl=url&TZ=Asia/Shanghai&indent=on&wt=json&rows=1000'

#　最近24小时发布
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW-1DAY/DAY TO *] &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　今天发布
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　今天发布，指定domain
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] AND domain:sxrb.com&
fl=article_title,domain,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　今天发布，带监控条件
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] AND text:(+("国务院" OR "总理" OR "中央" OR "领导") AND +("李克强"^2 OR "张高丽" OR "刘延东" OR "汪洋" OR "马凯" OR "常万全" OR "杨洁篪" OR "郭声琨")) &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　今天发布，作者非空
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] AND author:["" TO *] &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　昨天发布
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW-1DAY/DAY TO NOW/DAY] AND text:("宁吉喆") &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　最近七天发布，指定作者或者责任编辑
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY-7DAYS TO NOW] AND (author:"王玥" OR director:"余普") &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　最近一年（？）发布，标题非空，作者非空
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW-1YEAR/DAY TO NOW/DAY%2B1DAY] AND article_title:["" TO *]　AND author:["" TO *]　&
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,site_name&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

# 综合查询
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-03T00:00:00Z TO 2016-10-25T00:00:00Z] AND resource_category:论坛 &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,site_name&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

## 这个查询发现一个bug：host:tianya　导致没有结果
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-03T00:00:00Z TO 2016-10-25T00:00:00Z] AND resource_category:论坛 AND host:tianya &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,site_name&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-03T00:00:00Z TO 2016-10-25T00:00:00Z] AND resource_category:论坛 AND (host:tianya OR site_name:tianya) AND (author:张 OR director:张) &
fl=article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,site_name&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10'

#　最近一天抓取，标题非空，详细页
#　根据最新策略，仅详细页被索引
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-19T00:00:00.000Z TO *] AND article_title:["" TO *] ANT page_category:detail
&fl=article_title,fetch_time_history,page_category
TZ=Asia/Shanghai&indent=on&wt=json&rows=10&
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-19T00:00:00.000Z TO *] ANT page_category:detail
&fl=page_category&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10&
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-19T00:00:00.000Z TO *] ANT page_category:detail&
fl=article_title,fetch_time_history&
TZ=Asia/Shanghai&indent=on&wt=json&rows=10&
'

############################################################################
## 统计
############################################################################

#### 曝光量统计 ####
# 今天抓取
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d 'q=last_crawl_time:[NOW/DAY TO *]&fl=url'
# 今天抓取，按domain分组
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-7DAYS TO NOW] &
fl=url,domain,last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY-7DAYS&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.field=domain&
'

# 过去24小时抓取
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d 'q=last_crawl_time:[NOW-1DAY/DAY TO *]&fl=url'

# 最近7天抓取，按domain分组
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-7DAYS TO NOW] &
fl=url,domain,last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY-7DAYS&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.field=domain&
'

# 最近7天抓取，按天分组（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
# 表达式：rint(div(sum(ms(last_crawl_time),28800000),86400000))
# 解释：转换为标准时间戳 -> 加8小时时差 -> 除以一天的毫秒数 -> 取整
# 备注：有误差，需要进一步调查
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-7DAYS TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),28800000),86400000))
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-7DAYS TO NOW] &
fl=publish_time,last_crawl_time&
TZ=Asia/Shanghai&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),28800000),86400000))
'

### 曝光量统计 ###
# 今日抓取，按小时分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
# 表达式：rint(div(sum(ms(last_crawl_time),28800000),86400000))
# 解释：转换为标准时间戳 -> 加8小时时差 -> 除以一天的毫秒数 -> 取整
# 备注：有误差，需要进一步调查 - 返回的是UTC时间，需要在客户端重新调整
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY&
facet.range.end=NOW&
facet.range.gap=%2B1HOUR&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),mul(8,3600000)),mul(24,3600000)))
'

# 今日发布，按小时分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] &
fl=publish_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=publish_time&
facet.range.start=NOW/DAY&
facet.range.end=NOW&
facet.range.gap=%2B1HOUR&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),mul(8,3600000)),mul(24,3600000)))
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY&
facet.range.end=NOW&
facet.range.gap=%2B1HOUR&
group=true&
group.limit=10&
group.func=rint(div(ms(last_crawl_time),mul(24,3600000)))
'

# 本月抓取，按天分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
# 表达式：rint(div(sum(ms(last_crawl_time),28800000),86400000))
# 解释：转换为标准时间戳 -> 加8小时时差 -> 除以一天的毫秒数 -> 取整
# 备注：有误差，需要进一步调查
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-30DAYS TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/MONTH&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),mul(8,3600000)),mul(24,3600000)))
'

# 本月发布，按天分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY-30DAYS TO NOW] &
fl=publish_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=publish_time&
facet.range.start=NOW/MONTH&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.func=rint(div(ms(publish_time),mul(24,3600000)))
'

# 本月抓取，带监控条件，按天分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/MONTH TO NOW/DAY%2B2DAYS] AND text:(+("国务院" OR "总理" OR "中央" OR "领导") AND +("李克强"^2 OR "张高丽" OR "刘延东" OR "汪洋" OR "马凯" OR "常万全" OR "杨洁篪" OR "郭声琨")) &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/MONTH&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.func=rint(div(ms(last_crawl_time),mul(24,3600000)))
'

# 最近30天抓取
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-30DAYS TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY-30DAYS&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.func=rint(div(ms(last_crawl_time),mul(24,3600000)))
'

# 当日抓取，按天分组，使用facet机制（注意这种方式不支持分布式查询，分布式需要在客户端再次计算）
# 备注：有误差，需要进一步调查
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY TO NOW] &
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY&
facet.range.end=NOW&
facet.range.gap=%2B1HOUR&
group=true&
group.limit=10&
group.func=rint(div(ms(last_crawl_time),mul(24,3600000)))
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-7DAYS TO NOW] &
fl=url,domain,last_crawl_time&
TZ=Asia/Shanghai&
group=true&
group.func=rint(div(ms(last_crawl_time),mul(24,3600000)))
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=page_category:detail&
facet=true&facet.date=first_crawl_time&facet.date.start=NOW/DAY-2DAYS&facet.date.end=NOW/DAY+1DAY&facet.date.gap=+1DAY
'

#### 资源类型统计 ####
# 今天
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY TO NOW] &
fl=resource_category,last_crawl_time&
TZ=Asia/Shanghai&
group=true&
group.limit=2&
group.field=resource_category&
'

# 昨天
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-1DAY TO NOW/DAY] &
fl=resource_category,last_crawl_time&
TZ=Asia/Shanghai&
group=true&
group.limit=10&
group.field=resource_category&
'

#### 资源类型分布统计 ####
# 按照所有的resource_category分别统计，在客户端进行整合
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW/DAY-30DAYS TO NOW] AND resource_category:资讯&
fl=last_crawl_time&
TZ=Asia/Shanghai&
facet=true&
facet.range=last_crawl_time&
facet.range.start=NOW/DAY-30DAYS&
facet.range.end=NOW&
facet.range.gap=%2B1DAY&
group=true&
group.limit=10&
group.func=rint(div(sum(ms(last_crawl_time),mul(8,3600000)),mul(24,3600000)))
'

# 使用facet只能进行统计最终结果，不能分时段统计，使用子查询可能能够获得结果，但不能确定
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=*:*&
fl=last_crawl_time&
json.facet=
{
  resource_category:{
    type: terms,
    field: resource_category
  }
}
'

# 某个domain下的不同host发布量
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW/DAY TO NOW] AND domain:"sxrb.com" &
fl=domain,host&rows=100&
TZ=Asia/Shanghai&
json.facet=
{
  host:{
    type: terms,
    field: host
  }
}
'

#### 热点话题 ####
curl http://qiwur.com:8983/solr/information_1101_integration_test/select -d '
q=*:*&
fl=url,author,article_title&
TZ=Asia/Shanghai&
start=0&
rows=10&
indent=on&
qt=tvrh&
tv=true&
tv.all=true&
f.includes.tv.tf=false&
tv.fl=includes&
facet=true&
facet.field=tv.tf
'

curl http://qiwur.com:8983/solr/information_1101_integration_test/select -d 'qt=tvrh&q=text:[* TO *]&fl=article_title&indent=on'

#### 其他统计 ####
# 最近一天抓取，标题非空
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[2016-10-21T00:00:00.000Z TO *] AND article_title:["" TO *] AND publish_time:[2016-10-01T00:00:00.000Z TO *] &
fl=url,host,article_title,encoding,resource_category,author,director,publish_time&
TZ=Asia/Shanghai&
json.facet={
  x : "unique(id)"
}'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=url:tianya &
fl=url,resource_category&
TZ=Asia/Shanghai&
json.facet={
  x : "unique(id)"
}'

curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=page_category:detail&
fl=resource_category&
TZ=Asia/Shanghai&
json.facet={
  x : "unique(resource_category)"
}
'

# domain统计
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=*:*&
fl=domain&
rows=1000&
json.facet={
  x : "unique(domain)"
}
'

# host统计
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=*:*&
fl=host&
rows=1000&
json.facet={
  x : "unique(host)"
}
'

# 错误的抓取时间
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=last_crawl_time:[NOW TO *] OR last_crawl_time:[* TO NOW-30DAYS] &
fl=url,host,last_crawl_time&
TZ=Asia/Shanghai&
json.facet={
  x : "unique(id)"
}
'

# 错误的发布时间
curl http://qiwur.com:8983/solr/information_1101_integration_test/query -d '
q=publish_time:[NOW TO *] OR publish_time:[* TO NOW-30DAYS] &
fl=url,host,publish_time&
TZ=Asia/Shanghai&
json.facet={
  x : "unique(id)"
}
'

# 删除时间错误的记录
## TODO : How to apply timezone
curl http://qiwur.com:8983/solr/information_1101_integration_test/update?commitWithin=3000 -d "<delete><query>publish_time:[NOW+1DAY TO *] OR publish_time:[* TO NOW-30DAYS]</query></delete>"
curl http://qiwur.com:8983/solr/information_1101_integration_test/update?commitWithin=3000 -d "<delete><query>last_crawl_time:[NOW+1DAY TO *] OR last_crawl_time:[* TO NOW-30DAYS]</query></delete>"

# update config to zookeeper
# stop all
bin/solr stop -all
# start main solr server
bin/solr zk -upconfig -d information_configs -n information
bin/solr zk -upconfig -d information_configs -n information -z localhost:9983
# create collections
bin/solr create -c information_1001_integration_test -n information -shards 2 -replicationFactor 2
bin/solr create -c information_taiwan_20170617 -n information -shards 2 -replicationFactor 2
bin/solr create -c information_tmp -n information
# browse collection
http://localhost:8983/solr/information_tmp/browse
