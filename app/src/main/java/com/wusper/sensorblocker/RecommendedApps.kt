package com.wusper.sensorblocker

/**
 * Common domestic (China) shopping and video apps that frequently access sensors.
 */
object RecommendedApps {
    val packages: Set<String> = setOf(
        // Shopping
        "com.taobao.taobao",           // 淘寶
        "com.jingdong.app.mall",       // 京東
        "com.xunmeng.pinduoduo",       // 拼多多
        "com.tmall.wireless",          // 天貓
        "com.suning.mobile.ebuy",      // 蘇寧
        "com.vip.vip",                 // 唯品會
        "com.achievo.vipshop",         // 唯品會 alt
        "com.alibaba.wireless",        // 1688
        "com.meituan.retail.v.android",// 美團優選
        "com.sankuai.meituan",         // 美團
        "com.dianping.v1",             // 大眾點評
        "com.xiaohongshu",             // 小紅書
        "com.xingin.xhs",              // 小紅書 alt
        // Video / short video
        "com.ss.android.ugc.aweme",    // 抖音
        "com.ss.android.ugc.aweme.lite", // 抖音極速
        "com.smile.gifmaker",          // 快手
        "com.kuaishou.nebula",         // 快手極速
        "tv.danmaku.bili",             // 嗶哩嗶哩
        "com.bilibili.app.in",         // B站國際
        "com.youku.phone",             // 優酷
        "com.iqiyi.i18n",              // 愛奇藝
        "com.qiyi.video",              // 愛奇藝
        "com.tencent.qqlive",          // 騰訊視頻
        "com.hunantv.imgo.activity",   // 芒果TV
        "com.letv.android.client",     // 樂視
        "com.sohu.sohuvideo",          // 搜狐視頻
        "com.ss.android.article.news", // 今日頭條
        "com.ss.android.article.lite", // 頭條極速
        "com.zhihu.android",           // 知乎
        "com.baidu.tieba",             // 百度貼吧
        "com.sina.weibo",              // 微博
        "com.tencent.mm",              // 微信（部分場景用感測器）
        "com.tencent.mobileqq"         // QQ
    )
}
