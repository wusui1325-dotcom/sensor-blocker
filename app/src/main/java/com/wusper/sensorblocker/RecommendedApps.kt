package com.wusper.sensorblocker

/**
 * Common domestic (China) shopping, video, map, and related apps
 * that frequently access motion/orientation sensors.
 */
object RecommendedApps {
    val packages: Set<String> = setOf(
        // ===== Shopping =====
        "com.taobao.taobao",           // 淘寶
        "com.jingdong.app.mall",       // 京東
        "com.xunmeng.pinduoduo",       // 拼多多
        "com.tmall.wireless",          // 天貓
        "com.suning.mobile.ebuy",      // 蘇寧易購
        "com.vip.vip",                 // 唯品會
        "com.achievo.vipshop",         // 唯品會
        "com.alibaba.wireless",        // 1688
        "com.meituan.retail.v.android",// 美團優選
        "com.sankuai.meituan",         // 美團
        "com.dianping.v1",             // 大眾點評
        "com.xiaohongshu",             // 小紅書
        "com.xingin.xhs",              // 小紅書
        "com.jd.jrapp",                // 京東金融
        "com.eg.android.AlipayGphone", // 支付寶

        // ===== Video / Short video / Film =====
        "com.ss.android.ugc.aweme",    // 抖音
        "com.ss.android.ugc.aweme.lite", // 抖音極速版
        "com.smile.gifmaker",          // 快手
        "com.kuaishou.nebula",         // 快手極速版
        "tv.danmaku.bili",             // 嗶哩嗶哩
        "com.bilibili.app.in",         // B站國際版
        "com.youku.phone",             // 優酷
        "com.iqiyi.i18n",              // 愛奇藝國際
        "com.qiyi.video",              // 愛奇藝
        "com.tencent.qqlive",          // 騰訊視頻
        "com.hunantv.imgo.activity",   // 芒果TV
        "com.letv.android.client",     // 樂視視頻
        "com.sohu.sohuvideo",          // 搜狐視頻
        "com.pptv.vas",                // PP視頻 / 聚力視頻
        "com.pplive.androidphone",     // PPTV
        "com.funshion.video.mobile",   // 風行視頻
        "com.baidu.video",             // 百度視頻
        "com.baidu.haokan",            // 好看視頻
        "com.ss.android.article.news", // 今日頭條
        "com.ss.android.article.lite", // 頭條極速版
        "com.zhihu.android",           // 知乎
        "com.baidu.tieba",             // 百度貼吧
        "com.sina.weibo",              // 微博
        "com.tencent.mm",              // 微信
        "com.tencent.mobileqq",        // QQ
        "com.netease.cloudmusic",      // 網易雲音樂
        "com.kugou.android",           // 酷狗音樂
        "com.tencent.qqmusic",         // QQ音樂

        // ===== Maps / Navigation (國產地圖) =====
        "com.autonavi.minimap",        // 高德地圖
        "com.baidu.BaiduMap",          // 百度地圖
        "com.tencent.map",             // 騰訊地圖
        "com.sogou.map.android.maps",  // 搜狗地圖
        "com.mapbar.android.mapbarmap",// 圖吧
        "com.navinfo.gwead",           // 四維圖新
        "com.careland.car",            // 凱立德
        "com.uu.uunavi",               // 悠悠導航
        "cn.buding.martin",            // 布丁導航
        "com.sankuai.meituan.map",     // 美團地圖相關
        "com.amap.android.ams",        // 高德相關
        "com.baidu.navi",              // 百度導航
        "com.tencent.wecar",           // 騰訊車聯 / 地圖相關
        "com.huawei.maps.app",         // 華為地圖（部分裝置）
        "com.xiaomi.smarthome",        // 小米相關可能含地圖
        "com.oplus.maps"               // OPPO 地圖（部分）
    )
}
