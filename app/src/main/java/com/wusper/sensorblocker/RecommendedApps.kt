package com.wusper.sensorblocker

/**
 * Catalogues of common mainland-China apps.
 *
 * [blockCandidates] may be suggested on first launch for the list-block
 * mode (shopping, short-video, news). Maps, payment, WeChat and QQ are
 * never auto-selected: maps need compass / gyroscope, and payment / IM
 * apps are left to the user.
 */
object RecommendedApps {
    val blockCandidates: Set<String> = setOf(
        // Shopping / local services that often ship "shake" ads
        "com.taobao.taobao",
        "com.jingdong.app.mall",
        "com.xunmeng.pinduoduo",
        "com.tmall.wireless",
        "com.suning.mobile.ebuy",
        "com.vip.vip",
        "com.achievo.vipshop",
        "com.alibaba.wireless",
        "com.alibaba.aliexpresshd",
        "com.sankuai.meituan",
        "com.sankuai.meituan.takeoutnew",
        "com.meituan.retail.v.android",
        "com.dianping.v1",
        "com.xiaohongshu",
        "com.xingin.xhs",
        "com.taobao.idlefish",
        "com.taobao.litetao",
        // Short video / content feeds
        "com.ss.android.ugc.aweme",
        "com.ss.android.ugc.aweme.lite",
        "com.ss.android.ugc.live",
        "com.smile.gifmaker",
        "com.kuaishou.nebula",
        "com.ss.android.article.video",
        "com.youku.phone",
        "com.qiyi.video",
        "com.tencent.qqlive",
        "com.hunantv.imgo.activity",
        "com.baidu.haokan",
        // News / information feeds
        "com.ss.android.article.news",
        "com.ss.android.article.lite",
        "com.tencent.news",
        "com.netease.newsreader.activity",
        "com.sohu.newsclient",
        "com.sina.news"
    )

    val allowCandidates: Set<String> = setOf(
        "com.autonavi.minimap",
        "com.baidu.BaiduMap",
        "com.tencent.map",
        "com.sogou.map.android.maps",
        "com.mapbar.android.mapbarmap",
        "com.baidu.navi",
        "com.amap.android.ams",
        "com.sankuai.meituan.map",
        "com.huawei.maps.app",
        "com.oplus.maps",
        "com.google.android.apps.maps",
        "com.google.android.apps.mapslite"
    )

    /** Must never be auto-checked on the block list. */
    val doNotAutoBlock: Set<String> = allowCandidates + setOf(
        "com.eg.android.AlipayGphone",
        "com.alipay.android.app",
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.tim",
        "com.unionpay"
    )

    fun isBlockCandidate(packageName: String): Boolean =
        packageName in blockCandidates && packageName !in doNotAutoBlock

    fun isAllowCandidate(packageName: String): Boolean = packageName in allowCandidates

    fun firstLaunchBlockTargets(installed: Set<String>): Set<String> {
        return installed.filter { isBlockCandidate(it) }.toSet()
    }
}
