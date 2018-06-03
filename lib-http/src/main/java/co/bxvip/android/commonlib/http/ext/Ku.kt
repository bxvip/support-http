package co.bxvip.android.commonlib.http.ext

import android.os.Handler
import android.os.Looper
import android.util.Log
import co.bxvip.android.commonlib.http.BuildConfig
import co.bxvip.android.commonlib.http.HttpManager
import co.bxvip.android.commonlib.http.intercepter.CacheInterceptor
import co.bxvip.android.commonlib.http.intercepter.LogInterceptor
import co.bxvip.android.commonlib.http.intercepter.RetryIntercepter
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * <pre>
 *     author: vic
 *     time  : 18-4-18
 *     desc  : 工具类
 * </pre>
 */

class Ku private constructor() {
    companion object {

        val TAG = "HttpForTag"

        @Volatile
        var client: OkHttpClient? = null
        @Volatile
        var threadPool: Executor? = null

        @Volatile
        var mHandler: Handler? = null
        @Volatile
        private var mGson: Gson? = null

        fun getKClient(): OkHttpClient {
            if (client == null) {
                synchronized(Ku::class) {
                    client = OkHttpClient.Builder()
                            .addInterceptor(RetryIntercepter(2))//重试
                            .addInterceptor(LogInterceptor())// 请求打印
                            .addInterceptor(CacheInterceptor())
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .build()
                }
            }
            return client!!
        }

        fun getKThreadPool(): Executor {
            if (threadPool == null) {
                synchronized(Ku::class) {
                    threadPool = Executors.newCachedThreadPool()
                }
            }
            return threadPool!!
        }

        fun getKHander(): Handler {
            if (mHandler == null) {
                synchronized(Ku::class) {
                    mHandler = Handler(Looper.getMainLooper())
                }
            }
            return mHandler!!
        }

        fun post(r: (() -> Unit)) {
            getKHander().post(r)
        }

        fun getKGson(): Gson {
            if (mGson == null) {
                synchronized(Ku::class) {
                    mGson = Gson()
                }
            }
            return mGson!!
        }
    }

    fun cancelCallByTag(tag: String) {
        for (call in getKClient().dispatcher().queuedCalls()) {
            if (call.request().tag() == tag)
                call.cancel()
        }
        for (call in getKClient().dispatcher().runningCalls()) {
            if (call.request().tag() == tag)
                call.cancel()
        }
    }
}

object KLog {
    /**
     * 异常打印
     */
    fun exceptionLog(call: Call?, e: Exception, response: String = "") {
        try {
            val request = call?.request()
            if (HttpManager._HttpManagerCallBack?._onFailDoLog != null) {
                HttpManager._HttpManagerCallBack?._onFailDoLog!!.invoke(request!!, response + e.message)
            }
            Log.e(Ku.TAG, "\n")
            Log.e(Ku.TAG, "----------Start----异常-----")
            Log.e(Ku.TAG, "| Thread:${Thread.currentThread().name}")
            Log.e(Ku.TAG, "| Exception:$e")
            var requestS = request.toString()
            val tag = requestS.indexOf("tag")
            if (tag != -1) {
                requestS = requestS.substring(0, tag - 1) + "}"
            }
            if (BuildConfig.DEBUG)
                Log.e(Ku.TAG, "| $requestS")
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
                        Log.e(Ku.TAG, "| RequestParams:{" + sb.toString() + "}")
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
                    Log.e(Ku.TAG, "| RequestHeaders:{" + sb.toString() + "}")
            }

            if (BuildConfig.DEBUG)
                Log.e(Ku.TAG, "| Response:$response")
            Log.e(Ku.TAG, "----------End----------------")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG)
                Log.d(Ku.TAG, e.toString())
        }
    }
}

object UnifiedErrorUtil {
    fun unifiedError(failCode: (String) -> Unit, e: Throwable) {
        val message = when (e) {
            is UnknownHostException, is TimeoutException -> {
                "服务器开小差,请稍后重试！"
            }
            is ConnectException, is SocketTimeoutException, is SocketException -> {
                "网络连接超时，请检查您的网络状态！"
            }
            is NumberFormatException, is IllegalArgumentException -> {
                "未能请求到数据或者参数错误!"
            }
            else -> {
                "未知异常，稍后重试！"
            }
        }
        failCode.invoke(message)
    }
}