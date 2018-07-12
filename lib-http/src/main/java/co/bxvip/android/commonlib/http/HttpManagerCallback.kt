package co.bxvip.android.commonlib.http

import okhttp3.Request

/**
 *
 * ┌───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┐
 * │Esc│ │ F1│ F2│ F3│ F4│ │ F5│ F6│ F7│ F8│ │ F9│F10│F11│F12│ │P/S│S L│P/B│ ┌┐    ┌┐    ┌┐
 * └───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┘ └┘    └┘    └┘
 * ┌──┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───────┐┌───┬───┬───┐┌───┬───┬───┬───┐
 * │~`│! 1│@ 2│# 3│$ 4│% 5│^ 6│& 7│* 8│( 9│) 0│_ -│+ =│ BacSp ││Ins│Hom│PUp││N L│ / │ * │ - │
 * ├──┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─────┤├───┼───┼───┤├───┼───┼───┼───┤
 * │Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{ [│} ]│ | \ ││Del│End│PDn││ 7 │ 8 │ 9 │   │
 * ├────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤└───┴───┴───┘├───┼───┼───┤ + │
 * │Caps │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  │             │ 4 │ 5 │ 6 │   │
 * ├─────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────────┤    ┌───┐    ├───┼───┼───┼───┤
 * │Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│  Shift   │    │ ↑ │    │ 1 │ 2 │ 3 │   │
 * ├────┬──┴─┬─┴──┬┴───┴───┴───┴───┴───┴──┬┴───┼───┴┬────┬────┤┌───┼───┼───┐├───┴───┼───┤ E││
 * │Ctrl│Ray │Alt │         Space         │ Alt│code│fuck│Ctrl││ ← │ ↓ │ → ││   0   │ . │←─┘│
 * └────┴────┴────┴───────────────────────┴────┴────┴────┴────┘└───┴───┴───┘└───────┴───┴───┘
 *
 * <pre>
 *     author: vic
 *     time  : 18-5-12
 *     desc  : ${END}
 * </pre>
 */
/**
 * 网络监听需要的回调
 */
open class HttpManagerCallback {
    var _onRequestUrl: (() -> String)? = null
    var _onRequstSecondUrl: (() -> String)? = null
    var _onFormBodyBefore: (() -> HashMap<String, String>)? = null
    var _onRequestCommonHeader: (() -> HashMap<String, String>)? = null
    var _onSwitchUrl: (() -> Boolean)? = null
    var _onResponse400000: (() -> Unit)? = null
    var _onResponse450000: ((data: String) -> Unit)? = null
    var _onFailDoLog: ((request: Request, message: String, level: Int) -> Unit)? = null

    @Synchronized
    fun onFormBodyBefore(listener: () -> HashMap<String, String>) {
        _onFormBodyBefore = listener
    }

    @Synchronized
    fun onResponse40000(listener: () -> Unit) {
        _onResponse400000 = listener
    }

    @Synchronized
    fun onResponse45000(listener: (data: String) -> Unit) {
        _onResponse450000 = listener
    }

    fun onRequestCommonHeaders(listener: () -> HashMap<String, String>) {
        _onRequestCommonHeader = listener
    }

    @Synchronized
    fun onRequestUrl(listener: () -> String) {
        _onRequestUrl = listener
    }

    @Synchronized
    fun onRequestSecondUrl(listener: () -> String) {
        _onRequstSecondUrl = listener
    }

    @Synchronized
    fun onSwitchUrl(listener: () -> Boolean) {
        _onSwitchUrl = listener
    }

    @Synchronized
    fun onFailDoLog(listener: (request: Request, message: String, level: Int) -> Unit) {
        _onFailDoLog = listener
    }
}

open class CountUrlCallBack {
    var _onFailUrl: ((req: Request?) -> Unit)? = null
    var _onSucceedUrl: ((req: Request?) -> Unit)? = null

    fun onFailUrl(listener: (req: Request?) -> Unit) {
        _onFailUrl = listener
    }

    fun onSucceedUrl(listener: (req: Request?) -> Unit) {
        _onSucceedUrl = listener
    }
}
