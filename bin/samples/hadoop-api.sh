#!/usr/bin/env bash

echo "Do not run this script directly, run the specified commands alone in this script"
exit 0

hadoop fs -copyFromLocal conf/alternatives/information/local/seeds/sxrb_1.txt hdfs://galaxyeye:9000/tmp/pulsar-seeds
