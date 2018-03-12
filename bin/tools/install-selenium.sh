#!/usr/bin/env bash

cd /tmp/

echo "Installing latest stable google-chrome"
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome*.deb
sudo apt-get install -f
google-chrome -version

echo
echo "Installing latest stable chromedriver"
wget https://npm.taobao.org/mirrors/chromedriver/2.34/chromedriver_linux64.zip
unzip chromedriver*.zip
sudo cp chromedriver /usr/local/bin/
chromedriver -version

cd -
