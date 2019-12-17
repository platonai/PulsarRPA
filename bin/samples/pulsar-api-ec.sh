#!/usr/bin/env bash

echo "Do not run this script directly, run the following commands separately"

# crawl under distribution mode, a smaller seed collection
bin/crawl conf/alternatives/ec/cluster @conf/alternatives/ec/seeds/amazon.txt ec_pulsarjob_"$(cat VERSION)"_"$(date +%m)"01 ec_tmp 3 > logs/crawl-ec-"$(date +%Y%m)".out 2>&1 &
# crawl under distribution mode, a bigger seeds collection
bin/crawl conf/alternatives/ec/cluster @conf/alternatives/ec/seeds/all.txt ec_pulsarjob_"$(cat VERSION)"_"$(date +%m)"01 ec_tmp 1000 > logs/crawl-ec-"$(date +%Y%m)".out 2>&1 &
# crawl under local mode. Config dir, seed file, crawl id, indexer url and depth are given
bin/crawl conf/alternatives/ec/local @conf/alternatives/ec/local/seeds/amazon.txt ec_tmp  4
# crawl under local mode. Use default config dir, no injection, crawl id, indexer url and limit are given
bin/crawl default false ec_tmp 1

# inject a seed
bin/pulsarjob inject https://www.amazon.com/
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

# truncate the webpage table for default crawl if the crawlId starts with tmp_ or ends with _tmp
bin/pulsarjob webdb -crawlId ec_tmp -truncate
# read a row from the table
bin/pulsarjob webdb -get https://www.amazon.com/
