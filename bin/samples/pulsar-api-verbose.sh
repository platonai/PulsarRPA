#!/usr/bin/env bash

echo "Do not run this script directly, run the specified commands alone in this script"

# start pulsar master
bin/pulsar master

# crawl under distribution mode
bin/crawl.sh conf/alternatives/information/seeds/all.txt information_`cat VERSION`_`date +%m`01 information_1101_integration_test 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under local mode. Config dir, seed file, crawl id, indexer url and depth are given
bin/crawl-local.sh conf/alternatives/information/local @conf/alternatives/information/local/seeds/sxrb.txt information_tmp http://master:8983/solr/information_tmp/ 4
# crawl under local mode. Use default config dir, no injection, crawl id, indexer url and depth are given
bin/crawl-local.sh default false information_tmp http://master:8983/solr/information_tmp/ 1

# inject a seed
bin/pulsar inject http://www.sxrb.com/sxxww/ -crawlId information_tmp
# inject a seed with crawl options
bin/pulsar inject "http://www.sxrb.com/sxxww/ -i 1s -d 2" -crawlId information_tmp
# inject seeds in file
bin/pulsar inject @conf/alternatives/information/local/seeds/all.txt -crawlId information_tmp
# generate top 100 fetch tasks
bin/pulsar generate -topN 1000 -crawlId information_tmp
# fetch all tasks for batch id in file last-batch-id
bin/pulsar fetch @/tmp/pulsar-$USER/last-batch-id -crawlId information_tmp -index -solrUrl http://master:8983/solr/information_tmp/
# fetch all generated tasks, use zookeeper to find the index server
bin/pulsar fetch -all -crawlId information_tmp -fetchMode native -index -collection information_tmp
# fetch all generated tasks, re-fetch if it's fetch just now but not updated, without indexing
bin/pulsar fetch -all -resume -crawlId information_tmp
# update outgoing graph
bin/pulsar updateoutgraph @/tmp/pulsar-$USER/last-batch-id -crawlId information_tmp
# update incoming graph
bin/pulsar updateingraph @/tmp/pulsar-$USER/last-batch-id -crawlId information_tmp

bin/pulsar samplejob -crawlId information_tmp -limit 1000
bin/pulsar webdb -crawlId information_tmp -truncate
bin/pulsar webdb -crawlId information_tmp -get http://www.sxrb.com/sxxww/

# parse and index separately
bin/pulsar parse -reparse -crawlId information_tmp
bin/pulsar solrindex -all -collection information_tmp -crawlId information_tmp -limit 1000
bin/pulsar solrindex -reindex -collection information_tmp -crawlId information_tmp -limit 1000
bin/pulsar solrindex -reindex -collection information_tmp -crawlId information_tmp -limit 1000 > logs/pulsar-$USER-solrindex.log 2>&1 &
