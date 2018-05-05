package co.bxvip.android.commonlib.http.intercepter

import android.util.Log

import java.io.IOException

import co.bxvip.android.commonlib.http.BuildConfig
import okhttp3.FormBody
import okhttp3.Interceptor

/**
 * 网络日志
 * Created by SillySnnall on 2018/3/22.
 */

class LogInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(chain.request())
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val mediaType = response.body()!!.contentType()
        val content = response.body()!!.string()
        if (BuildConfig.DEBUG) Log.d(TAG, "\n")
        if (BuildConfig.DEBUG) Log.d(TAG, "----------Start----------------")
        var requestS = request.toString()
        val tag = requestS.indexOf("tag")
        if (tag != -1) {
            requestS = requestS.substring(0, tag - 2) + "}"
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "| $requestS")
        val method = request.method()
        if ("POST" == method) {
            val sb = StringBuilder()
            if (request.body() is FormBody) {
                val body = request.body() as FormBody?
                for (i in 0 until (body?.size() ?: 0)) {
                    sb.append(body!!.encodedName(i)).append("=").append(body.encodedValue(i)).append(",")
                }
                if (sb.length != 0) sb.delete(sb.length - 1, sb.length)
                if (BuildConfig.DEBUG) Log.d(TAG, "| RequestParams:{" + sb.toString() + "}")
            }
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "| Response:$content")
        Log.d(TAG, "----------End:" + duration + "毫秒----------")
        return response.newBuilder()
                .body(okhttp3.ResponseBody.create(mediaType, content))
                .build()
    }

    companion object {
        var TAG = "LogInterceptor"
    }
}
