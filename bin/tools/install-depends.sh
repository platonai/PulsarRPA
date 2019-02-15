#!/usr/bin/env bash

cd /tmp/

echo "Installing latest stable google-chrome"
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome*.deb
sudo apt-get install -f
google-chrome -version

echo
echo "Installing proper chromedriver"

echo "If you are using Chrome version 73, please download ChromeDriver 73.0.3683.20"
echo "If you are using Chrome version 72, please download ChromeDriver 2.46 or ChromeDriver 72.0.3626.69"
echo "If you are using Chrome version 71, please download ChromeDriver 2.46 or ChromeDriver 71.0.3578.137"

echo "see http://chromedriver.chromium.org/downloads"

CHROMEDRIVER_VERSION=72.0.3626.7

wget https://npm.taobao.org/mirrors/chromedriver/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip
unzip chromedriver*.zip
sudo cp chromedriver /usr/local/bin/
chromedriver -version

cd -
