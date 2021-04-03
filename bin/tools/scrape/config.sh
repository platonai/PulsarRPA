#!/bin/bash

host=crawl3

# The host of the API server
if [[ "$host" = "" ]]; then
  export host=localhost
fi

# Ask the administrator for your username and authToken
export authToken=gJn6fUBh-1-af1639a924d7232099a037e9544cf43f

# Important!
# Change to your own callback url, we will post the result of x-sql to this url
export callbackUrl="http://localhost:8182/api/hello/echo"

# The example target url for our x-sql
export fetchUrl='https://www.amazon.com/dp/B00BTX5926 -i 1s -retry';
