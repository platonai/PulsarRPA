#!/usr/bin/env bash

bin="$(dirname "$0")"
bin="$(cd "$bin"; pwd)"

"$bin"/crawl.sh -v conf/alternatives/information/local @conf/alternatives/information/local/seeds/sxrb.txt information_tmp http://master:8983/solr/information_1101_integration_test 4
# "$bin"/crawl.sh -v conf/alternatives/information/seeds/all.txt information_`$bin/version -s`_`date +%m`01 information_1101_integration_test 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &
# "$bin"/crawl.sh -v conf/alternatives/information/seeds/all.txt information_`$bin/version -s`_`date +%m`01 information_1101_integration_test 1000 > logs/crawl-information-`date +%Y%m`.out 2>&1 &

# bin/crawl.sh -v conf/alternatives/information/local @conf/alternatives/information/local/seeds/sxrb.txt information_tmp http://master:8983/solr/information_tmp/ 4
