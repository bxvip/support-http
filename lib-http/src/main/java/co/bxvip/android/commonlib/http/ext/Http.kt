package co.bxvip.android.commonlib.http.ext

import android.util.Log
import android.widget.Toast
import co.bxvip.android.commonlib.http.BaseStringResult
import co.bxvip.android.commonlib.http.HttpManager
import co.bxvip.android.commonlib.http.NetworkUtil
import co.bxvip.android.commonlib.http.ext.Ku.Companion.TAG
import co.bxvip.android.commonlib.utils.CommonInit
import co.bxvip.tools.partials.partially1
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.FormBody
import java.io.IOException
import java.net.ConnectException


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
        Ku.post {
            Toast.makeText(CommonInit.ctx, "请连接网络....", Toast.LENGTH_SHORT).show()
        }
        return
    }
    val wrap = RequestWrapper<T>()
    wrap.init()
    val request = wrap.request()
    if (request == null) {
        Ku.post {
            Toast.makeText(CommonInit.ctx, "不支持该请求方式....", Toast.LENGTH_SHORT).show()
        }
        return
    }
    wrap.execute(request)
}

/**
 * 根据tag销毁请求
 */
fun cancelCallByTag(tag: String) {
    for (call in Ku.getKClient().dispatcher().queuedCalls()) {
        if (call.request().tag() == tag)
            call.cancel()
    }
    for (call in Ku.getKClient().dispatcher().runningCalls()) {
        if (call.request().tag() == tag)
            call.cancel()
    }
}

/**
 * 取消所有的tag
 */
fun cancelAllHttpCall() {
    for (call in Ku.getKClient().dispatcher().queuedCalls()) {
        call.cancel()
    }
    for (call in Ku.getKClient().dispatcher().runningCalls()) {
        call.cancel()
    }
}

val pairs = fun(map: MutableMap<String, String>, makePairs: RequestPairs.() -> Unit) {
    val requestPair = RequestPairs()
    requestPair.makePairs()
    map.putAll(requestPair.pairs)
}

open class RequestWrapper<T> {
    var url = ""
    var subUrl = ""
    var method: String = "GET"
    var classOfT: Class<T> = String::class.java as Class<T>
    var useDefaultResultBean = true
    var needCommonParam = true
    var needEncrypt = true
    var needTry = true
    var tag = ""
    private var tryCount = 0
    protected val _params: MutableMap<String, String> = mutableMapOf()
    protected val _fileParams: MutableMap<String, String> = mutableMapOf()
    protected val _headers: MutableMap<String, String> = mutableMapOf()

    internal var _success: (T) -> Unit = {}
    internal var _fail: (String) -> Unit = { }
    internal var _40000Page: () -> Unit = {}

    val body = pairs.partially1(_params)
    val headers = pairs.partially1(_headers)


    fun onSuccess(onSuccess: (T) -> Unit) {
        _success = onSuccess
    }

    fun onFail(onError: (String) -> Unit) {
        _fail = onError
    }

    fun on40000Page(on40000: () -> Unit) {
        _40000Page = on40000
    }

    fun request(): Request? {
        val req = when (method) { "get", "Get", "GET" -> Request.Builder().url(getGetUrl(fillUrl(url, subUrl), needCommonParam, _params) { it.toQueryString() })
            "post", "Post", "POST" -> Request.Builder().url(fillUrl(url, subUrl)).post(fillRequestForm(needCommonParam, _params))
            "put", "Put", "PUT" -> Request.Builder().url(fillUrl(url, subUrl)).put(fillRequestForm(needCommonParam, _params))
            "delete", "Delete", "DELETE" -> Request.Builder().url(fillUrl(url, subUrl)).delete(fillRequestForm(needCommonParam, _params))
            else -> null
        }
        req?.headers(fillRequestHeader(_headers))
        if (tag.isNotEmpty()) {
            req?.tag(tag)
        }
        return req?.build()
    }

    fun execute(request: Request) {
        Ku.getKThreadPool().execute {
            Ku.getKClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException) {
                    if (!call!!.isCanceled) {
                        Ku.getKThreadPool().execute {
                            try {
                                UnifiedErrorUtil.unifiedError(_fail, e, true, {
                                    val changeSucceed = HttpManager._HttpManagerCallBack != null && HttpManager._HttpManagerCallBack?._onSwitchUrl?.invoke()!!
                                    if (needTry && url.isNotEmpty() && changeSucceed) {
                                        tryCount++
                                        if (tryCount <= Ku.maxTryCount) execute(request()!!) else {
                                            UnifiedErrorUtil.unifiedError(_fail, e, false, {})
                                        }
                                    } else {
                                        UnifiedErrorUtil.unifiedError(_fail, e, false, {})
                                    }
                                })
                            } catch (e: Exception) {
                                Ku.getKHander().post {
                                    UnifiedErrorUtil.unifiedError(_fail, e, false, {})
                                }
                            }
                            KLog.exceptionLog(call, e, "http onFailure()", 1)
                        }
                    }
                }

                override fun onResponse(call: Call?, response: Response?) {
                    if (!call!!.isCanceled) {
                        Ku.getKThreadPool().execute {
                            val retryCode: (Exception) -> Unit = {
                                val changeSucceed = HttpManager._HttpManagerCallBack != null && HttpManager._HttpManagerCallBack?._onSwitchUrl?.invoke()!!
                                if (needTry && url.isNotEmpty() && changeSucceed) {
                                    tryCount++
                                    if (tryCount <= Ku.maxTryCount) execute(request()!!) else {
                                        UnifiedErrorUtil.unifiedError(_fail, it, false, {})
                                    }
                                } else {
                                    UnifiedErrorUtil.unifiedError(_fail, it, false, {})
                                }
                            }
                            try {
                                if (response?.isSuccessful!!) {
                                    if (HttpManager._CountUrlCallBack != null) HttpManager._CountUrlCallBack?._onSucceedUrl?.invoke(call.request())
                                    val data = response.body()?.string()?.trim()
                                    when {
                                        useDefaultResultBean -> {

                                            val fromJsonB = Ku.getKGson().fromJson(data, BaseStringResult::class.java)
                                            when {
                                                40000 == fromJsonB?.msg -> Ku.getKHander().post {
                                                    if (HttpManager._HttpManagerCallBack?._onResponse400000 != null) {
                                                        HttpManager._HttpManagerCallBack?._onResponse400000?.invoke()
                                                    }
                                                    _40000Page.invoke()
                                                }
                                                45000 == fromJsonB?.msg -> Ku.getKHander().post {
                                                    // 服务器正在维护
                                                    if (HttpManager._HttpManagerCallBack?._onResponse450000 != null) {
                                                        HttpManager._HttpManagerCallBack?._onResponse450000?.invoke(Ku.getKGson().toJson(fromJsonB.data))
                                                    }
                                                }
                                                classOfT.name == "java.lang.String" -> Ku.post {
                                                    _success(data as T)
                                                }
                                                else -> {
                                                    val fromJson = Ku.getKGson().fromJson(data, classOfT)
                                                    Ku.post {
                                                        _success(fromJson)
                                                    }
                                                }
                                            }
                                        }
                                        else -> Ku.post {
                                            _success(Ku.getKGson().fromJson<T>(data, classOfT))
                                        }
                                    }
                                } else {
                                    when {
                                        response.body() != null -> {
                                            val string = response.body()!!.string()
                                            when {
                                                string.contains("<html") ||
                                                        string.contains("<body") ||
                                                        string.contains("<head") ->
                                                    UnifiedErrorUtil.unifiedError(_fail, JsonSyntaxException("非json数据"), true, { retryCode.invoke(ConnectException("error:code < 200 or code > 300")) })
                                                else -> UnifiedErrorUtil.unifiedError(_fail, ConnectException("error:code < 200 or code > 300"), false, {})
                                            }
                                        }
                                        else -> UnifiedErrorUtil.unifiedError(_fail, Exception("请求失败!"), false, {})
                                    }
                                }
                            } catch (e: Exception) {
                                UnifiedErrorUtil.unifiedError(_fail, e, true, {
                                    retryCode.invoke(e)
                                })
                                KLog.exceptionLog(call, e, level = 1)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun getGetUrl(url: String, needCommonParam: Boolean = true, params: MutableMap<String, String>, toQueryString: (map: Map<String, String>) -> String): String {
        if (needCommonParam && HttpManager._HttpManagerCallBack?._onFormBodyBefore != null) {
            val hashMap = HttpManager._HttpManagerCallBack?._onFormBodyBefore?.invoke()
            if (hashMap != null) {
                for ((k, v) in hashMap) {
                    params[k] = v
                }
            }
        }
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
private fun fillUrl(url: String = "", subUrl: String = ""): String {
    return if (url.isEmpty()) "${
    HttpManager._HttpManagerCallBack?._onRequestUrl?.invoke() ?: ""
    }${
    HttpManager._HttpManagerCallBack?._onRequstSecondUrl?.invoke() ?: ""
    }$subUrl" else {
        url + subUrl
    }
}

/**
 * 构造 formBody
 */
private fun fillRequestForm(needCommonParam: Boolean = true, params: MutableMap<String, String>?): FormBody {
    val builder = FormBody.Builder()
    params?.map { builder.add(it.key, it.value) }
    if (needCommonParam && HttpManager._HttpManagerCallBack?._onFormBodyBefore != null) {
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

