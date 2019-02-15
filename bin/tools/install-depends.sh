#!/usr/bin/env bash

install_chrome() {
    echo "Installing latest stable google-chrome"

    wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
    sudo dpkg -i google-chrome*.deb
    sudo apt-get install -f

    google-chrome -version
}

install_chrome_driver() {
    echo
    echo "Installing chromedriver"

    # echo "If you are using Chrome version 73, please download ChromeDriver 73.0.3683.20"
    # echo "If you are using Chrome version 72, please download ChromeDriver 2.46 or ChromeDriver 72.0.3626.69"
    # echo "If you are using Chrome version 71, please download ChromeDriver 2.46 or ChromeDriver 71.0.3578.137"
    # echo "If you are using Chrome from Dev or Canary channel, please download ChromeDriver 2.46, it might work"
    echo "see http://chromedriver.chromium.org/downloads"

    CHROME_DRIVER_VERSION=2.46
    if [[ "$CHROME_VERSION" == "71" ]]; then
      CHROME_DRIVER_VERSION=71.0.3578.137
    elif [[ "$CHROME_VERSION" == "72" ]]; then
      CHROME_DRIVER_VERSION=72.0.3626.7
    elif [[ "$CHROME_VERSION" == "73" ]]; then
      CHROME_DRIVER_VERSION=73.0.3683.20
    fi

    if [[ "$CHROME_VERSION" != "$REAL_CHROME_DRIVER_VERSION" ]]; then
        wget https://npm.taobao.org/mirrors/chromedriver/"$CHROME_DRIVER_VERSION"/chromedriver_linux64.zip
        unzip chromedriver*.zip
        sudo cp chromedriver /usr/local/bin/
    fi

    chromedriver -version
}

cd /tmp/

# find out chrome version
CHROME_VERSION="$(google-chrome -version | head -n1 | awk -F '[. ]' '{print $3}')"

if [[ "$CHROME_VERSION" == "" ]]; then
    install_chrome
fi

REAL_CHROME_DRIVER_VERSION="$(chromedriver -version | head -n1 | cut -d' ' -f2)"
if [[ "$REAL_CHROME_DRIVER_VERSION" == "" ]]; then
    install_chrome_driver
fi

cd -
