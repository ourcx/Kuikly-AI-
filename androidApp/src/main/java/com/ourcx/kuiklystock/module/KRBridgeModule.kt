package com.ourcx.kuiklystock.module

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ourcx.kuiklystock.BuildConfig
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.ourcx.kuiklystock.KRApplication
import com.ourcx.kuiklystock.KuiklyRenderActivity
import org.json.JSONObject
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class KRBridgeModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "closePage" -> activity?.finish()
            "openPage" -> {
                val paramJSON = JSONObject(params ?: "{}")
                val pageName = paramJSON.optString("pageName", paramJSON.optString("url", ""))
                val pageData = paramJSON.optJSONObject("pageData") ?: JSONObject()
                val ctx = activity ?: KRApplication.application
                KuiklyRenderActivity.start(ctx, pageName, pageData)
            }
            "toast" -> {
                val paramJSON = JSONObject(params ?: "{}")
                Toast.makeText(KRApplication.application, paramJSON.optString("content"), Toast.LENGTH_SHORT).show()
            }
            "log" -> {
                val paramJSON = JSONObject(params ?: "{}")
                Log.i("KuiklyRender", paramJSON.optString("content"))
            }
            "copyToPasteboard" -> {
                val paramJSON = JSONObject(params ?: "{}")
                val content = paramJSON.optString("content")
                val clipboard = KRApplication.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("kuikly", content))
            }
            "currentTimestamp" -> (System.currentTimeMillis()).toString()
            "dateFormatter" -> {
                val paramJSON = JSONObject(params ?: "{}")
                val data = Date(paramJSON.optLong("timeStamp"))
                SimpleDateFormat(paramJSON.optString("format")).format(data)
            }
            "isWorkBuddyConfigured" -> workBuddyProxyUrl()?.let { true } ?: false
            "requestWorkBuddy" -> {
                requestWorkBuddy(params, callback)
                null
            }
            else -> callback?.invoke(mapOf("code" to -1, "message" to "Method not found"))
        }
    }

    private fun requestWorkBuddy(params: String?, callback: KuiklyRenderCallback?) {
        val proxyUrl = workBuddyProxyUrl()
        if (proxyUrl == null) {
            callbackOnMainThread(callback, failureResponse("WorkBuddy HTTPS proxy is not configured"))
            return
        }

        val payload = runCatching {
            JSONObject(params ?: "{}").optString(WORK_BUDDY_PAYLOAD).takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("WorkBuddy request payload is missing")
        }.getOrElse { error ->
            callbackOnMainThread(callback, failureResponse(error.message ?: "Invalid WorkBuddy request"))
            return
        }

        workBuddyExecutor.execute {
            val response = runCatching { postWorkBuddyRequest(proxyUrl, payload) }
                .fold(
                    onSuccess = { successResponse(it) },
                    onFailure = { failureResponse(readableNetworkError(it)) },
                )
            callbackOnMainThread(callback, response)
        }
    }

    private fun postWorkBuddyRequest(proxyUrl: URL, payload: String): String {
        val connection = proxyUrl.openConnection() as HttpsURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val detail = readLimited(connection.errorStream, MAX_ERROR_BODY_CHARS)
                throw WorkBuddyHttpException(statusCode, sanitizeErrorDetail(detail))
            }
            val responseBody = readLimited(connection.inputStream, MAX_SUCCESS_BODY_CHARS + 1)
            if (responseBody.length > MAX_SUCCESS_BODY_CHARS) {
                throw IllegalStateException("WorkBuddy proxy response is too large")
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun callbackOnMainThread(callback: KuiklyRenderCallback?, response: Map<String, Any>) {
        if (callback == null) return
        mainHandler.post { callback.invoke(response) }
    }

    private fun successResponse(data: String): Map<String, Any> =
        mapOf(WORK_BUDDY_SUCCESS to true, WORK_BUDDY_DATA to data)

    private fun failureResponse(error: String): Map<String, Any> =
        mapOf(WORK_BUDDY_SUCCESS to false, WORK_BUDDY_ERROR to error)

    private fun readableNetworkError(error: Throwable): String = when (error) {
        is WorkBuddyHttpException -> buildString {
            append("WorkBuddy proxy returned HTTP ")
            append(error.statusCode)
            if (error.safeDetail.isNotEmpty()) append(": ${error.safeDetail}")
        }
        is SocketTimeoutException -> "WorkBuddy proxy request timed out"
        else -> "Unable to reach WorkBuddy proxy"
    }

    private fun workBuddyProxyUrl(): URL? = runCatching {
        URL(BuildConfig.WORKBUDDY_PROXY_URL.trim()).takeIf {
            it.protocol.equals("https", ignoreCase = true) && !it.host.isNullOrBlank()
        }
    }.getOrNull()

    private fun readLimited(stream: InputStream?, maxChars: Int): String {
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(512)
            while (result.length < maxChars) {
                val count = reader.read(buffer, 0, minOf(buffer.size, maxChars - result.length))
                if (count < 0) break
                result.append(buffer, 0, count)
            }
            result.toString()
        }
    }

    private fun sanitizeErrorDetail(rawDetail: String): String {
        if (rawDetail.isBlank()) return ""
        val message = runCatching {
            val json = JSONObject(rawDetail)
            sequenceOf("message", "error", "detail")
                .map { json.optString(it) }
                .firstOrNull { it.isNotBlank() }
        }.getOrNull() ?: rawDetail
        return message
            .replace(BEARER_PATTERN, "Bearer <REDACTED>")
            .replace(SECRET_PATTERN) { match -> "${match.groupValues[1]}=<REDACTED>" }
            .replace(WHITESPACE_PATTERN, " ")
            .trim()
            .take(MAX_SAFE_ERROR_CHARS)
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"

        private const val WORK_BUDDY_PAYLOAD = "payload"
        private const val WORK_BUDDY_SUCCESS = "success"
        private const val WORK_BUDDY_DATA = "data"
        private const val WORK_BUDDY_ERROR = "error"
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        private const val MAX_ERROR_BODY_CHARS = 4_096
        private const val MAX_SUCCESS_BODY_CHARS = 1_000_000
        private const val MAX_SAFE_ERROR_CHARS = 300

        private val workBuddyExecutor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
        private val BEARER_PATTERN = Regex("(?i)Bearer\\s+[^\\s,;]+")
        private val SECRET_PATTERN = Regex(
            """(?i)"?(authorization|access[_-]?token|refresh[_-]?token|token|api[_-]?key|secret|password)"?\s*[:=]\s*"?[^\s,;"]+""",
        )
        private val WHITESPACE_PATTERN = Regex("\\s+")
    }
}

private class WorkBuddyHttpException(
    val statusCode: Int,
    val safeDetail: String,
) : Exception()
