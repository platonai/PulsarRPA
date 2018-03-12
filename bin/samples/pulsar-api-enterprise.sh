#!/usr/bin/env bash

echo "Enterprise class Big Data support"
echo "Do not run this script directly, run the specified commands alone in this script"

# crawl under distribution mode, a smaller seed collection
bin/crawl.sh -E conf/alternatives/information/cluster @conf/seeds/main.txt information_pulsar_0.2 information_1101_integration_test 3
# crawl under distribution mode, a bigger seeds collection, running in background
bin/crawl.sh -E conf/alternatives/information/cluster @conf/seeds/main.txt information_pulsar_0.2 information_1101_integration_test 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# crawl under local mode. Config dir, seed file, crawl id, indexer url and depth are given
bin/crawl.sh -E conf/alternatives/information/local @conf/seeds/main.txt information_tmp http://master:8983/solr/information_tmp/ 4
# crawl under local mode. Use default config dir, no injection, crawl id, indexer url and limit are given
bin/crawl.sh -E default false information_tmp http://master:8983/solr/information_tmp/ 1

# start pulsar master
bin/pulsar -E master

# inject a seed
bin/pulsarjob -E inject http://news.163.com/domestic/
# inject a seed with crawl options
bin/pulsarjob -E inject "http://news.163.com/domestic/ -i 1s -d 2"
# inject seeds in a file
bin/pulsarjob -E inject @conf/seeds/main.txt
# generate top 100 fetch tasks
bin/pulsarjob -E generate
# fetch the last batch
bin/pulsarjob -E fetch
# fetch the last batch, use zookeeper config to find out index url, index document into the given collection
bin/pulsarjob -E fetch -index -collection information_tmp
# fetch all generated batch of tasks, re-fetch them, without indexing
bin/pulsarjob -E fetch all -resume
# update outgoing graph
bin/pulsarjob -E updateoutgraph
# update incoming graph
bin/pulsarjob -E updateingraph
