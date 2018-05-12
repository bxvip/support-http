package co.bxvip.android.commonlib.http.ext

import android.util.Log
import android.widget.Toast
import co.bxvip.android.commonlib.http.BaseStringResult
import co.bxvip.android.commonlib.http.BuildConfig
import co.bxvip.android.commonlib.http.HttpManager
import co.bxvip.android.commonlib.http.NetworkUtil
import co.bxvip.android.commonlib.http.ext.Ku.Companion.TAG
import co.bxvip.android.commonlib.utils.CommonInit
import co.bxvip.tools.partials.partially1
import okhttp3.*
import okhttp3.FormBody
import java.io.IOException


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
 *     desc  : 新网络请求
 * </pre>
 */

fun <T> http(init: RequestWrapper<T>.() -> Unit) {
    if (!NetworkUtil.isNetworkAvailable(CommonInit.ctx)) {
        Log.d(TAG, "请连接网络....")
        Toast.makeText(CommonInit.ctx, "请连接网络....", Toast.LENGTH_SHORT).show()
        return
    }
    val wrap = RequestWrapper<T>()
    wrap.init()
    val request = wrap.request()
    if (request == null) {
        Toast.makeText(CommonInit.ctx, "不支持该请求方式....", Toast.LENGTH_SHORT).show()
        return
    }
    wrap.execute(request)
}

class RequestWrapper<T> {
    var url = ""
    var method: String = "GET"
    var classOfT: Class<T> = String::class.java as Class<T>
    var useDefaultResultBean = true
    protected val _params: MutableMap<String, String> = mutableMapOf()
    protected val _fileParams: MutableMap<String, String> = mutableMapOf()
    protected val _headers: MutableMap<String, String> = mutableMapOf()

    internal var _success: (T) -> Unit = {}
    internal var _fail: (Throwable) -> Unit = {}
    internal var _40000Page: () -> Unit = {}

    private val pairs = fun(map: MutableMap<String, String>, makePairs: RequestPairs.() -> Unit) {
        val requestPair = RequestPairs()
        requestPair.makePairs()
        map.putAll(requestPair.pairs)
    }

    val body = pairs.partially1(_params)
    val headers = pairs.partially1(_headers)


    fun onSuccess(onSuccess: (T) -> Unit) {
        _success = onSuccess
    }

    fun onFail(onError: (Throwable) -> Unit) {
        _fail = onError
    }

    fun on40000Page(on40000: () -> Unit) {
        _40000Page = on40000
    }

    fun request(): Request? {
        val req = when (method) { "get", "Get", "GET" -> Request.Builder().url(getGetUrl(fillUrl(url), _params) { it.toQueryString() })
            "post", "Post", "POST" -> Request.Builder().url(fillUrl(url)).post(fillRequestForm(_params))
            "put", "Put", "PUT" -> Request.Builder().url(fillUrl(url)).put(fillRequestForm(_params))
            "delete", "Delete", "DELETE" -> Request.Builder().url(fillUrl(url)).delete(fillRequestForm(_params))
            else -> null
        }
        req?.headers(fillRequestHeader(_headers))
        return req?.build()
    }

    fun execute(request: Request) {
        Ku.getKThreadPool().execute {
            Ku.getKClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    try {
                        if (HttpManager._HttpManagerCallBack != null && HttpManager._HttpManagerCallBack?._onSwitchUrl?.invoke()!!) execute(changeRequestUrl(request))
                        else {
                            Log.d(TAG, "网络异常:${e.toString()}")
                            Ku.getKHander().post {
                                _fail(e!!)
                            }
                        }
                    } catch (e: Exception) {
                        KLog.exceptionLog(call, e)
                    }
                }

                override fun onResponse(call: Call?, response: Response?) {
                    try {
                        if (response?.isSuccessful!!) {
                            val data = response.body()?.string()?.trim()
                            if (useDefaultResultBean) {
                                val fromJsonB = Ku.getKGson().fromJson(data, BaseStringResult::class.java)
                                if (40000 == fromJsonB?.msg) {
                                    if (HttpManager._HttpManagerCallBack?._onResponse400000 != null) {
                                        HttpManager._HttpManagerCallBack?._onResponse400000?.invoke()
                                    }
                                    HttpManager.handler?.post {
                                        _40000Page.invoke()
                                    }
                                    return
                                }
                                if (45000 == fromJsonB?.msg) {
                                    // 服务器正在维护
                                    if (HttpManager._HttpManagerCallBack?._onResponse450000 != null) {
                                        HttpManager._HttpManagerCallBack?._onResponse450000?.invoke(fromJsonB.data.toString())
                                    }
                                    return
                                }

                                // 返回原始数据判断

                                if (classOfT.name == "java.lang.String") {
                                    Ku.post {
                                        _success(data as T)
                                    }
                                } else {
                                    val fromJson = Ku.getKGson().fromJson(data, classOfT)
                                    Ku.post {
                                        _success(fromJson)
                                    }
                                }
                            } else {
                                Ku.post {
                                    _success(Ku.getKGson().fromJson(data, classOfT))
                                }
                            }
                        } else {
                            if (response.body() != null) {
                                val string = response.body()!!.string()
                                if (string.contains("<head>") && string.contains("<body>") && string.contains("<html>")) {
                                    // 切换线路
                                    if (BuildConfig.DEBUG)
                                        Log.e(TAG, "isSuccessful 为 false,请求失败")
                                    if (HttpManager._HttpManagerCallBack?._onSwitchUrl?.invoke()!!) execute(changeRequestUrl(request))
                                    else {
                                        Ku.post {
                                            _fail(Exception("error:code < 200 or code > 300"))
                                        }
                                    }
                                }
                            } else {
                                Ku.post {
                                    _fail(Exception("请求失败!"))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Ku.post {
                            _fail(e)
                        }
                        KLog.exceptionLog(call, e)
                    }
                }
            })
        }
    }

    private fun changeRequestUrl(request: Request): Request {
        val body = request.body()
        val headers = request.headers()
        val method = request.method()
        return request.newBuilder().url(fillUrl()).method(method, body).headers(headers).build()
    }


    private fun getGetUrl(url: String, params: MutableMap<String, String>, toQueryString: (map: Map<String, String>) -> String): String {
        return if (params.isEmpty()) url else "$url?${toQueryString(params)}"
    }

    private fun <K, V> Map<K, V>.toQueryString(): String = this.map { "${it.key}=${it.value}" }.joinToString("&")
}

class RequestPairs {
    var pairs: MutableMap<String, String> = HashMap()
    operator fun String.minus(value: String) {
        pairs[this] = value
    }
}

/**
 * 构造 url
 */
private fun fillUrl(url: String = ""): String {
    return if (url.isEmpty()) "${
    HttpManager._HttpManagerCallBack?._onRequestUrl?.invoke() ?: ""
    }${
    HttpManager._HttpManagerCallBack?._onRequstSecondUrl?.invoke() ?: ""
    }" else {
        url
    }
}

/**
 * 构造 formBody
 */
private fun fillRequestForm(params: MutableMap<String, String>?): FormBody {
    val builder = FormBody.Builder()
    params?.map { builder.add(it.key, it.value) }
    if (HttpManager._HttpManagerCallBack?._onFormBodyBefore != null) {
        val hashMap = HttpManager._HttpManagerCallBack?._onFormBodyBefore?.invoke()
        if (hashMap != null) {
            for ((k, v) in hashMap) {
                builder.add(k, v)
            }
        }
    }
    return builder.build()
}

/**
 * 构造 header
 */
private fun fillRequestHeader(params: MutableMap<String, String>?): Headers {
    val builder = Headers.Builder()
    params?.map { builder.add(it.key, it.value) }
    if (HttpManager._HttpManagerCallBack?._onRequestCommonHeader != null) {
        val hashMap = HttpManager._HttpManagerCallBack?._onRequestCommonHeader?.invoke()
        if (hashMap != null) {
            for ((k, v) in hashMap) {
                builder.add(k, v)
            }
        }
    }
    return builder.build()
}

