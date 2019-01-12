#!/usr/bin/env bash

echo "Do not run this script directly, run the following commands separately"

# crawl under distribution mode, a smaller seed collection
bin/crawl.sh conf/alternatives/information/cluster @conf/alternatives/information/seeds/test_10.txt information_pulsarjob_`cat VERSION`_`date +%m`01 information_tmp 3 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under distribution mode, a bigger seeds collection
bin/crawl.sh conf/alternatives/information/cluster @conf/alternatives/information/seeds/all.txt information_pulsarjob_`cat VERSION`_`date +%m`01 information_tmp 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under local mode. Config dir, seed file, crawl id, indexer url and depth are given
bin/crawl.sh conf/alternatives/information/local @conf/alternatives/information/local/seeds/sxrb.txt information_tmp http://master:8983/solr/information_tmp/ 4
# crawl under local mode. Use default config dir, no injection, crawl id, indexer url and limit are given
bin/crawl.sh default false information_tmp http://master:8983/solr/information_tmp/ 1

# start pulsarjob master
bin/pulsarjob master

# inject a seed
bin/pulsarjob inject http://www.sxrb.com/sxxww/
# generate top 100 fetch tasks
bin/pulsarjob generate
# fetch last batch
bin/pulsarjob fetch
# fetch all generated batch of tasks, re-fetch them, without indexing
bin/pulsarjob fetch all -resume
# update outgoing graph
bin/pulsarjob updateoutgraph
# update incoming graph
bin/pulsarjob updateingraph

# run a sample job, the sample job is a most mininal job, so it can be used to check whether the environment is OK
bin/pulsarjob samplejob
# truncate the webpage table for default crawl if the crawlId starts with tmp_ or ends with _tmp
bin/pulsarjob webdb -crawlId information_tmp -truncate
# read a row from the table
bin/pulsarjob webdb -get http://www.sxrb.com/sxxww/

# parse a web page from the given url, print the parse result
bin/pulsarjob simpleparser http://www.sxrb.com/sxxww/
# extract a web page from the given url, print the extract result
bin/pulsarjob simpleextractor http://bbs.tianya.cn/post-feeling-4241027-2.shtml -i pt1s -p 2000 -d 1 -fd body -fu .+ -fa 8,40 \
  -e news -ed body -Ftitle=.atl-title! \
  -Fcontent=.atl-content -Finfo=".atl-menu%.atl-info:eq(1)" -Fauthor=".atl-menu%.atl-info:eq(1)%span:eq(0)%a"
# index a web page from the given url
bin/pulsarjob simpleindexer --indexer-url http://master:8983/solr/information_tmp/ --depth 0 http://www.sxrb.com/sxxww/
# index a web page from the given url, with crawl options are applied
bin/pulsarjob simpleindexer http://bbs.tianya.cn/post-feeling-4241027-2.shtml -i pt1s -d 1 -e news -ed body -Ftitle=.art_tit -Fcontent=.art_txt -Finfo=.art_info -Fauthor=.editer

# open a interactive console to check special urls using CrawlFilters
bin/pulsarjob crawlfilterchecker
# open a interactive console to check special urls using UrlFilters
bin/pulsarjob urlfilterchecker
# open a interactive console to check special urls using UrlNormalizers
bin/pulsarjob urlnormalizerchecker
