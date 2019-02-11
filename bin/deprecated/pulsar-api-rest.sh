#!/usr/bin/env bash

echo "Do not run this script directly, run the specified commands alone in this script"

## common
curl -X PUT "http://localhost:8182/api/admin/stop?authToken=admin@localhost:iwqxi8iloqpol"

curl "http://localhost:8182/api/port/report"
curl "http://localhost:8182/api/port/active?type=FetchService"
curl "http://localhost:8182/api/port/free?type=FetchService"
curl "http://localhost:8182/api/port/acquire?type=FetchService"
curl -X PUT "http://localhost:8182/api/port/recycle?type=FetchService&port=21000"

curl "http://localhost:8182/api/config"

curl "http://localhost:8182/api/job"
curl "http://localhost:8182/api/job/1"

# Major
curl 'http://localhost:8182/api/seeds'
curl 'http://localhost:8182/api/seeds/home'
curl 'http://localhost:8182/api/pages/links?url=http://pulsar.warps.org/seeds/1'
curl 'http://localhost:8182/api/pages/delete?url=http://pulsar.warps.org/seeds/1'

curl 'http://localhost:8182/api/seeds/inject?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/seeds/uninject?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/seeds/inject-out-pages?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/seeds/uninject-out-pages?url=http://news.china.com/zh_cn/social/index.html'

curl 'http://localhost:8182/api/pages?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages?url=http://news.china.com/zh_cn/social/index.html&encoding=utf-8'

# get page representation, args : -s, -f, -a
curl 'http://localhost:8182/api/pages/representation?args=-s&url=http://news.china.com/zh_cn/social/index.html'
# get page fields, args : -s, -f, -a
curl 'http://localhost:8182/api/pages/fields?args=-s&url=http://news.china.com/zh_cn/social/index.html'
# get page links, args : -ol, -oo, -a
curl 'http://localhost:8182/api/pages/links?args=-ol&url=http://news.china.com/zh_cn/social/index.html'

# get fields of all outgoing pages, args : -s, -f, -a
curl 'http://localhost:8182/api/pages/outgoing/fields?args=-s&url=http://news.china.com/zh_cn/social/index.html'
# get fields of entities of all outgoing pages
curl 'http://localhost:8182/api/pages/outgoing/entities?url=http://news.china.com/zh_cn/social/index.html'

curl 'http://localhost:8182/api/pages/fetch?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages/parse?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages/index?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages/force-refetch?url=http://news.china.com/zh_cn/social/index.html'

curl 'http://localhost:8182/api/pages/outgoing/fetch?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages/outgoing/parse?url=http://news.china.com/zh_cn/social/index.html'
curl 'http://localhost:8182/api/pages/outgoing/index?url=http://news.china.com/zh_cn/social/index.html'

## inject advanced
curl http://localhost:8182/api/seed/inject?crawlId=information_tmp -H "Content-Type: application/json" -X POST -d 'http://www.sxrb.com/sxxww/xwpd/'

curl http://localhost:8182/api/seed/inject?crawlId=information_tmp -H "Content-Type: application/json" -X POST -d '
http://www.sxrb.com/sxxww/xwpd/\t-i pt1s -p 2000 -d 1 -fd body -fu .+ -fa 8,40 \
   -e article -ed body \
   -Ftitle=#title -Fcontent=#content -Fauthor=#author -Fpublish_time=#publish_time \
   -c comments -cd #comments -ci .comment \
   -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time
'

curl http://localhost:8182/api/seed/inject?crawlId=information_tmp -H "Content-Type: application/json" -X POST -d '
http://www.sxrb.com/sxxww/xwpd/sxxww/  -i pt1s -p 1010 -d 1 -fd body -fu .+ -fa 8,40 \
   -e article -ed body \
   -Ftitle=#title -Fcontent=#content -Fauthor=#author -Fpublish_time=#publish_time \
   -c comments -cd #comments -ci .comment \
   -FFauthor=.author -FFcontent=.content -FFpublish_time=.publish-time

http://bbs.tianya.cn/post-feeling-4241027-2.shtml -i pt1s -p 2000 -d 1 -fd body -fu .+ -fa 8,40 -e news -ed body \
    -Ftitle=.art_tit -Fcontent=.art_txt -Finfo=.art_info -Fauthor=.editer -Fnobody=.not-exist
'
