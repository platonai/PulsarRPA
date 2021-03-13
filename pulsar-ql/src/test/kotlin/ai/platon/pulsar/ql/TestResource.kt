package ai.platon.pulsar.ql

import java.util.concurrent.atomic.AtomicInteger

object TestResource {

    val productIndexUrl = "http://category.dangdang.com/cid4001403.html"
    val productDetailUrl = "http://product.dangdang.com/1184814777.html"
    val newsIndexUrl = "http://news.baidu.com/guoji"
    val newsDetailUrl = "http://news.163.com/17/1119/09/D3JJF1290001875P.html"
    val newsDetailUrl2 = "http://www.chinanews.com/gn/2018/03-02/8458538.shtml"

    var urlGroups = mutableMapOf<String, Array<String>>()

    val seeds = mapOf(
        0 to "http://product.dangdang.com/1184814777.html",
        1 to "http://category.dangdang.com/cid4003728.html",
        2 to "http://category.dangdang.com/cid4002590.html",
        3 to "https://list.mogujie.com/book/magic/51894",
        4 to "https://category.vip.com/search-5-0-1.html?q=3|103363||&rp=26600|48483&ff=|0|2|2&adidx=1&f=ad&adp=130610&adid=632673",
        5 to "https://list.jd.com/list.html?cat=6728,6742,13246",
        6 to "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
        7 to "https://search.yhd.com/c0-0/k%25E7%2594%25B5%25E8%25A7%2586/",
        8 to "http://mall.goumin.com/mall/list/219",
        9 to "http://sj.zol.com.cn/series/list80_1_1.html",
        10 to "https://search.jd.com/Search?keyword=手机&enc=utf-8&wq=手机"
    )

    val newsSeeds = arrayOf(
        "http://www.mwr.gov.cn/xw/mtzs/xhsxhw/"
    )

    val testedSeeds = listOf(
        /////////////////////////////////////////////////////////
        // The sites below are well tested

        "https://www.mia.com/formulas.html",
        "https://www.mia.com/diapers.html",
        "http://category.dangdang.com/cid4002590.html",
        "https://list.mogujie.com/book/magic/51894",
        "https://list.jd.com/list.html?cat=6728,6742,13246",
        "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
        "https://search.yhd.com/c0-0/k%25E7%2594%25B5%25E8%25A7%2586/",
        "https://www.amazon.cn/b/ref=sa_menu_Accessories_l3_b888650051?ie=UTF8&node=888650051",
        "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",

        "https://www.lagou.com/zhaopin/chanpinzongjian/?labelWords=label",
        "https://mall.ccmn.cn/mallstocks/",
        "https://sh.julive.com/project/s/i1",
        "https://www.meiju.net/Mlist//Mju13.html",
        "http://mall.molbase.cn/p/612",
        "https://www.haier.com/xjd/all.shtml",
        "https://bj.nuomi.com/540",
        "https://www.haozu.com/sh/fxxiezilou/",
        "http://www.dianping.com/",
        "http://www.dianping.com/wuhan/ch55/g163",

        "https://p4psearch.1688.com/p4p114/p4psearch/offer.htm?spm=a2609.11209760.it2i6j8a.680.3c312de1W6LoPE&keywords=不锈钢螺丝", // very long time
        // company
        "https://www.cyzone.cn/company/list-0-0-1/",
        "https://www.cyzone.cn/capital/list-0-1-4/",
        // flower
        "https://www.hua.com/flower/",
        "https://www.hua.com/gifts/chocolates/",
        "https://www.hua.com/yongshenghua/yongshenghua_large.html",
        "http://www.cityflower.net/goodslist/5/",
        "http://www.cityflower.net/goodslist/2/",
        "http://www.cityflower.net/goodslist/1/0/0-0-4-0.html",
        "http://www.cityflower.net/",
        "http://www.zgxhcs.com/",
        // laobao
        "https://www.zhaolaobao.com/productlist.html?classifyId=77",
        "https://www.zhaolaobao.com/productlist.html?classifyId=82",
        "https://www.zhaolaobao.com/",
        // snacks
        "http://www.lingshi.com/",
        "http://www.lingshi.com/list/f64_o1.htm",
        "http://www.lingshi.com/list/f39_o1.htm",
        // jobs
        "https://www.lagou.com/gongsi/",
        "https://www.lagou.com/zhaopin/chanpinzongjian/",
        // love
        "http://yuehui.163.com/",
        // movie
        "http://v.hao123.baidu.com/v/search?channel=movie&category=科幻",
        "https://youku.com/",
        "https://movie.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_3&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fmovie.youku.com",
        "https://auto.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_16&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fauto.youku.com",
        "http://list.youku.com/category/video?spm=a2h1n.8251847.0.0",

        // pets
        "http://shop.boqii.com/cat/list-576-0-0-0.html",
        "http://shop.boqii.com/small/",
        "http://shop.boqii.com/brand/",
        "http://longyu.cc/shop.php?mod=exchange",
        "http://longdian.com/",
        "http://longdian.com/et_special.php?id=75",
        // menu
        "http://life.hao123.com/menu",
        "http://www.chinacaipu.com/shicai/sjy/junzao/xianggu/",

        "http://sj.zol.com.cn/",
        "http://sj.zol.com.cn/series/list80_1_1.html",
        "http://sj.zol.com.cn/pad/android/",
        "https://www.taoshouyou.com/game/wangzherongyao-2256-0-21",
        // property
        "https://www.ausproperty.cn/building/melbourne/",
        "http://jp.loupan.com/xinfang/",
        "https://malaysia.fang.com/house/",
        ""
    ).filter { it.isNotBlank() }

    val counter = AtomicInteger(0)

    init {
        urlGroups["baidu"] = arrayOf(
            "https://www.baidu.com/s?wd=马航&oq=马航&ie=utf-8"
        )
        urlGroups["jd"] = arrayOf(
            "https://list.jd.com/list.html?cat=652,12345,12349",
            "https://item.jd.com/1238838350.html",
            "http://search.jd.com/Search?keyword=长城葡萄酒&enc=utf-8&wq=长城葡萄酒",
            "https://detail.tmall.com/item.htm?id=536690467392",
            "https://detail.tmall.com/item.htm?id=536690467392",
            "http://item.jd.com/1304924.html",
            "http://item.jd.com/3564062.html",
            "http://item.jd.com/1304923.html",
            "http://item.jd.com/3188580.html",
            "http://item.jd.com/1304915.html"
        )
        urlGroups["mogujie"] = arrayOf(
            "http://list.mogujie.com/book/jiadian/10059513",
            "http://list.mogujie.com/book/skirt",
            "http://shop.mogujie.com/detail/1kcnxeu",
            "http://shop.mogujie.com/detail/1lrjy2c"
        )
        urlGroups["meilishuo"] = arrayOf(
            "http://www.meilishuo.com/search/catalog/10057053",
            "http://www.meilishuo.com/search/catalog/10057051",
            "http://item.meilishuo.com/detail/1lvld0y",
            "http://item.meilishuo.com/detail/1lwebsw"
        )
        urlGroups["vip"] = arrayOf(
            "http://category.vip.com/search-1-0-1.html?q=3|29736",
            "https://category.vip.com/search-5-0-1.html?q=3|182725",
            "http://detail.vip.com/detail-2456214-437608397.html",
            "https://detail.vip.com/detail-2640560-476811105.html"
        )
        urlGroups["wikipedia"] = arrayOf(
            "https://en.wikipedia.org/wiki/URL",
            "https://en.wikipedia.org/wiki/URI",
            "https://en.wikipedia.org/wiki/URN"
        )
    }
}