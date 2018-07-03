package co.bxvip.android.commonlib.http

/**
 * Http管理类
 * Created by SillySnnall on 2018/3/20.
 */
object HttpManager {

    var _HttpManagerCallBack: HttpManagerCallback? = null
    var _CountUrlCallBack: CountUrlCallBack? = null

    fun setCountUrlCallBack(init: CountUrlCallBack.() -> Unit) {
        _CountUrlCallBack = CountUrlCallBack().apply(init)
    }

    fun setHttpManagerCallBack(init: HttpManagerCallback.() -> Unit) {
        _HttpManagerCallBack = HttpManagerCallback().apply(init)
    }
}