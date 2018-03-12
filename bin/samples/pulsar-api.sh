#!/usr/bin/env bash

echo "Do not run this script directly, run the following commands separately"

# crawl under distribution mode, a smaller seed collection
bin/crawl.sh conf/alternatives/information/cluster @conf/alternatives/information/seeds/test_10.txt information_pulsar_`cat VERSION`_`date +%m`01 information_1101_integration_test 3 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under distribution mode, a bigger seeds collection
bin/crawl.sh conf/alternatives/information/cluster @conf/alternatives/information/seeds/all.txt information_pulsar_`cat VERSION`_`date +%m`01 information_1101_integration_test 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under local mode. Config dir, seed file, crawl id, indexer url and depth are given
bin/crawl.sh conf/alternatives/information/local @conf/alternatives/information/local/seeds/sxrb.txt information_tmp http://master:8983/solr/information_tmp/ 4
# crawl under local mode. Use default config dir, no injection, crawl id, indexer url and limit are given
bin/crawl.sh default false information_tmp http://master:8983/solr/information_tmp/ 1

# start pulsar master
bin/pulsar master

# inject a seed
bin/pulsar inject http://www.sxrb.com/sxxww/
# generate top 100 fetch tasks
bin/pulsar generate
# fetch last batch
bin/pulsar fetch
# fetch all generated batch of tasks, re-fetch them, without indexing
bin/pulsar fetch all -resume
# update outgoing graph
bin/pulsar updateoutgraph
# update incoming graph
bin/pulsar updateingraph

# run a sample job, the sample job is a most mininal job, so it can be used to check whether the environment is OK
bin/pulsar samplejob
# truncate the webpage table for default crawl if the crawlId starts with tmp_ or ends with _tmp
bin/pulsar webdb -crawlId information_tmp -truncate
# read a row from the table
bin/pulsar webdb -get http://www.sxrb.com/sxxww/

# parse a web page from the given url, print the parse result
bin/pulsar simpleparser http://www.sxrb.com/sxxww/
# extract a web page from the given url, print the extract result
bin/pulsar simpleextractor http://bbs.tianya.cn/post-feeling-4241027-2.shtml -i pt1s -p 2000 -d 1 -fd body -fu .+ -fa 8,40 \
  -e news -ed body -Ftitle=.atl-title! \
  -Fcontent=.atl-content -Finfo=".atl-menu%.atl-info:eq(1)" -Fauthor=".atl-menu%.atl-info:eq(1)%span:eq(0)%a"
# index a web page from the given url
bin/pulsar simpleindexer --indexer-url http://master:8983/solr/information_tmp/ --depth 0 http://www.sxrb.com/sxxww/
# index a web page from the given url, with crawl options are applied
bin/pulsar simpleindexer http://bbs.tianya.cn/post-feeling-4241027-2.shtml -i pt1s -d 1 -e news -ed body -Ftitle=.art_tit -Fcontent=.art_txt -Finfo=.art_info -Fauthor=.editer

# open a interactive console to check special urls using CrawlFilters
bin/pulsar crawlfilterchecker
# open a interactive console to check special urls using UrlFilters
bin/pulsar urlfilterchecker
# open a interactive console to check special urls using UrlNormalizers
bin/pulsar urlnormalizerchecker
