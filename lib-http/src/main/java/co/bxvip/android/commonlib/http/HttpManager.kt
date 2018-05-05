package co.bxvip.android.commonlib.http

import android.os.Handler
import android.os.Looper
import android.util.Log
import co.bxvip.android.commonlib.http.intercepter.CacheInterceptor
import co.bxvip.android.commonlib.http.intercepter.LogInterceptor
import co.bxvip.android.commonlib.http.intercepter.RetryIntercepter
import com.google.gson.Gson
import okhttp3.*
import okhttp3.internal.http2.Header
import org.json.JSONObject
import java.io.IOException


/**
 * Http管理类
 * Created by SillySnnall on 2018/3/20.
 */
object HttpManager {

    private val TAG = "HttpManager"
    private var client: OkHttpClient? = null
    var handler: Handler? = null
    private var gson: Gson? = null
    private var _HttpManagerCallBack: HttpManagerCallback? = null

    init {
        handler = Handler(Looper.getMainLooper())
        gson = Gson()
        client = OkHttpClient.Builder()
                .addInterceptor(RetryIntercepter(2))//重试
                .addInterceptor(LogInterceptor())// 请求打印
                .addInterceptor(CacheInterceptor())
                .build()
    }

    fun setHttpManagerCallBack(init: HttpManagerCallback.() -> Unit) {
        _HttpManagerCallBack = HttpManagerCallback().apply(init)
    }

    /**
     * 请求(GET)
     * @param url 请求地址，需要填写全地址 http://www.baidu.com/1/2
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun request(url: String, success: (String?) -> Unit, fail: (String?) -> Unit, timeout: () -> Unit,
                maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(FormBody.Builder(), String::class.java, success, fail, timeout, maintained, url, "", isCommonParameter, headers)
    }

    /**
     * 请求，已经解析(GET)
     * @param url 请求地址，需要填写全地址 http://www.baidu.com/1/2
     * @param classOfT Gson解析类型
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun <T> requestBean(url: String, classOfT: Class<T>?, success: (T?) -> Unit, fail: (String?) -> Unit,
                        timeout: () -> Unit, maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(FormBody.Builder(), classOfT, success, fail, timeout, maintained, url, "", isCommonParameter, headers)
    }

    /**
     * 请求(POST)
     * @param formBody 参数
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun request(formBody: FormBody.Builder, success: (String?) -> Unit, fail: (String?) -> Unit,
                timeout: () -> Unit, maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(formBody, String::class.java, success, fail, timeout, maintained, "", "", isCommonParameter, headers)
    }

    /**
     * 请求，已经解析(POST)
     * @param formBody 参数
     * @param classOfT Gson解析类型
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun <T> requestBean(formBody: FormBody.Builder, classOfT: Class<T>?, success: (T?) -> Unit, fail: (String?) -> Unit,
                        timeout: () -> Unit, maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(formBody, classOfT, success, fail, timeout, maintained, "", "", isCommonParameter, headers)
    }

    /**
     * 请求(POST)，可以更改二级请求地址
     * @param secondUrl 二级请求地址，需要填写Config.java中的地址
     * @param formBody 参数
     * @param classOfT Gson解析类型
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun requestSecondUrl(secondUrl: String, formBody: FormBody.Builder, success: (String?) -> Unit, fail: (String?) -> Unit,
                         timeout: () -> Unit, maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(formBody, String::class.java, success, fail, timeout, maintained, "", secondUrl, isCommonParameter, headers)
    }

    /**
     * 请求，已经解析(POST),可以更改二级请求地址
     * @param secondUrl 二级请求地址，需要填写Config.java中的地址
     * @param formBody 参数
     * @param classOfT Gson解析类型
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    fun <T> requestSecondUrlBean(secondUrl: String, formBody: FormBody.Builder, classOfT: Class<T>?, success: (T?) -> Unit,
                                 fail: (String?) -> Unit, timeout: () -> Unit, maintained: (String?) -> Unit, isCommonParameter: Boolean = true, headers: Headers? = null) {
        commonRequest(formBody, classOfT, success, fail, timeout, maintained, "", secondUrl, isCommonParameter, headers)
    }

    /**
     * 公共请求
     * @param formBody 参数
     * @param classOfT Gson解析类型
     * @param success 成功的回调
     * @param fail 失败的回调
     * @param timeout 登录超时40000的回调
     * @param maintained 服务器维护45000的回调
     * @param url 请求地址，需要填写全地址 http://www.baidu.com/1/2
     * @param secondUrl 二级请求地址，需要填写Config.java中的地址
     * @param isCommonParameter 是否需要公共参数
     * @param isRetry 是否是重试的方法
     */
    @Synchronized
    private fun <T> commonRequest(formBody: FormBody.Builder, classOfT: Class<T>?,
                                  success: (T?) -> Unit,
                                  fail: (String?) -> Unit,
                                  timeout: () -> Unit,
                                  maintained: (String?) -> Unit,
                                  url: String = "",
                                  secondUrl: String = "",
                                  isCommonParameter: Boolean, headers: Headers?) {
        try {
            var responseData = ""
            val request = if (url.isEmpty()) {// POST请求
                Request.Builder()
                        .url("${
                        _HttpManagerCallBack?._onRequestUrl?.invoke() ?: ""
                        }${
                        if (secondUrl.isEmpty())
                            _HttpManagerCallBack?._onRequstSecondUrl?.invoke() ?: ""
                        else secondUrl
                        }")
                        .post(commonParameter(formBody, isCommonParameter))
                        .headers(commonHeaders(headers))
                        .build()
            } else {// GET请求
                Request.Builder()
                        .url(url)
                        .headers(commonHeaders(headers))
                        .build()
            }
            client?.newCall(request)?.enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    handler?.post {
                        try {
                            if (_HttpManagerCallBack?._onSwitchUrl?.invoke()!!) commonRequest(formBody, classOfT, success, fail, timeout, maintained, url, secondUrl, false, headers)
                            else {
                                Log.d(TAG, "网络异常:${e.toString()}")
                                fail(e.toString())
                            }
                        } catch (e: Exception) {
                            exceptionLog(call, e)
                        }
                    }
                }

                override fun onResponse(call: Call?, response: Response?) {
                    try {
                        if (response?.isSuccessful!!) {
                            val data = response.body()?.string()?.trim()
                            responseData = data!!
                            val fromJsonB = gson?.fromJson(data, BaseStringResult::class.java)
                            if (40000 == fromJsonB?.msg) {
                                if (_HttpManagerCallBack?._onResponse400000 != null) {
                                    _HttpManagerCallBack?._onResponse400000?.invoke()
                                }
                                handler?.post {
                                    timeout()
                                }
                                return
                            }
                            // 45000  msg  // data:{"time":"12:00-23:00"}
                            if (45000 == fromJsonB?.msg) {
                                // 服务器正在维护
                                if (_HttpManagerCallBack?._onResponse450000 != null) {
                                    _HttpManagerCallBack?._onResponse450000?.invoke(fromJsonB.data.toString())
                                }
                                return
                            }

                            // 返回原始数据判断
                            if (classOfT?.name?.equals("java.lang.String")!!) {
                                handler?.post {
                                    success(data as T)
                                }
                            } else {
                                val fromJson = gson?.fromJson(data, classOfT)
                                handler?.post {
                                    success(fromJson)
                                }
                            }
                        } else {
                            if (response.body() != null) {
                                val string = response.body()!!.string()
                                if (string.contains("<head>") && string.contains("<body>") && string.contains("<html>")) {
                                    // 切换线路
                                    if (BuildConfig.DEBUG)
                                        Log.e(TAG, "isSuccessful 为 false,请求失败")
                                    exceptionLog(call, java.lang.Exception("error:code < 200 or code > 300"))
                                    if (_HttpManagerCallBack?._onSwitchUrl?.invoke()!!) commonRequest(formBody, classOfT, success, fail, timeout, maintained, url, secondUrl, false, headers)
                                    else {
                                        fail("error:code < 200 or code > 300")
                                    }
                                }
                            } else {
                                fail("请求失败!")
                            }
                        }
                    } catch (e: Exception) {
                        handler?.post {
                            fail(e.toString())
                        }
                        exceptionLog(call, e, responseData)
                    }
                }
            })
        } catch (e: Exception) {
            handler?.post {
                fail(e.toString())
            }
            Log.d(TAG, "网络异常:$e")
        }
    }

    /**
     * 公共参数
     * @param formBody 参数体
     * @param isCommonParameter 是否需要公共参数
     */
    @Synchronized
    private fun commonParameter(formBody: FormBody.Builder, isCommonParameter: Boolean): FormBody {
        return try {
            if (isCommonParameter) {
                if (_HttpManagerCallBack?._onFormBodyBefore != null) {
                    val hashMap = _HttpManagerCallBack?._onFormBodyBefore?.invoke()
                    if (hashMap != null) {
                        for ((k, v) in hashMap) {
                            formBody.add(k, v)
                        }
                    }
                }
                formBody.build()
            } else {
                formBody.build()
            }
        } catch (e: Exception) {
            Log.d(TAG, "网络异常:$e")
            formBody.build()
        }
    }

    /**
     * 封装公共请求头
     */

    @Synchronized
    private fun commonHeaders(headers: Headers? = null): Headers {
        val build = Headers.Builder()
        if (_HttpManagerCallBack?._onRequestCommonHeader != null) {
            val hashMap = _HttpManagerCallBack?._onRequestCommonHeader?.invoke()
            if (hashMap != null) {
                for ((k, v) in hashMap) {
                    build.add(k, v)
                }
            }
        }
        if (headers != null) {
            for (key in headers.names()) {
                if (key != null) {
                    build.add(key, headers.get(key)!!)
                }
            }
        }
        return build.build()
    }

    /**
     * 异常打印
     */
    private fun exceptionLog(call: Call?, e: Exception, response: String = "") {
        try {
            val request = call?.request()
            if (_HttpManagerCallBack?._onFailDoLog != null) {
                _HttpManagerCallBack?._onFailDoLog!!.invoke(request!!)
            }
            Log.e(TAG, "\n")
            Log.e(TAG, "----------Start----异常-----")
            Log.e(TAG, "| Thread:${Thread.currentThread().name}")
            Log.e(TAG, "| Exception:$e")
            var requestS = request.toString()
            val tag = requestS.indexOf("tag")
            if (tag != -1) {
                requestS = requestS.substring(0, tag - 1) + "}"
            }
            if (BuildConfig.DEBUG)
                Log.e(TAG, "| $requestS")
            val method = request?.method()
            if ("POST" == method) {
                val sb = StringBuilder()
                if (request.body() is FormBody) {
                    val body = request.body() as FormBody
                    for (i in 0 until body.size()) {
                        sb.append(body.encodedName(i) + "=" + body.encodedValue(i) + ",")
                    }
                    if (sb.isNotEmpty()) sb.delete(sb.length - 1, sb.length)
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, "| RequestParams:{" + sb.toString() + "}")
                }
            }
            val headers = request?.headers()
            if (headers != null) {
                val sb = StringBuilder()
                for (key in headers.names()) {
                    if (key != null) {
                        sb.append(key + "=" + headers.get(key) + ",")
                    }
                }
                if (sb.isNotEmpty()) sb.delete(sb.length - 1, sb.length)
                if (BuildConfig.DEBUG)
                    Log.e(TAG, "| RequestHeaders:{" + sb.toString() + "}")
            }

            if (BuildConfig.DEBUG)
                Log.e(TAG, "| Response:$response")
            Log.e(TAG, "----------End----------------")
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }
}

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
    var _onFailDoLog: ((request: Request) -> Unit)? = null

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
    fun onFailDoLog(listener: (request: Request) -> Unit) {
        _onFailDoLog = listener
    }
}

