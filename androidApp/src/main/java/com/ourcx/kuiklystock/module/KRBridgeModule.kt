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
import java.nio.charset.Charset
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
            "isOpenAiConfigured" -> openAiProxyUrl()?.let { true } ?: false
            "getOpenAiProxyUrl" -> openAiProxyUrl()?.toString().orEmpty()
            "getOpenAiModel" -> BuildConfig.OPENAI_MODEL
            "saveOpenAiProxyUrl" -> saveOpenAiProxyUrl(params)
            "clearOpenAiProxyUrl" -> {
                preferences.edit().remove(PREF_OPENAI_PROXY_URL).apply()
                true
            }
            "requestOpenAi" -> {
                requestOpenAi(params, callback)
                null
            }
            "requestTencentQuotes" -> {
                requestTencentQuotes(params, callback)
                null
            }
            else -> callback?.invoke(mapOf("code" to -1, "message" to "Method not found"))
        }
    }

    private fun requestOpenAi(params: String?, callback: KuiklyRenderCallback?) {
        val proxyUrl = openAiProxyUrl()
        if (proxyUrl == null) {
            callbackOnMainThread(callback, failureResponse("OpenAI HTTPS 代理尚未配置"))
            return
        }

        val payload = runCatching {
            JSONObject(params ?: "{}").optString(OPENAI_PAYLOAD).takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("OpenAI 请求内容为空")
        }.getOrElse { error ->
            callbackOnMainThread(callback, failureResponse(error.message ?: "OpenAI 请求无效"))
            return
        }

        networkExecutor.execute {
            val response = runCatching { postOpenAiRequest(proxyUrl, payload) }
                .fold(
                    onSuccess = { successResponse(it) },
                    onFailure = { failureResponse(readableOpenAiError(it)) },
                )
            callbackOnMainThread(callback, response)
        }
    }

    private fun requestTencentQuotes(params: String?, callback: KuiklyRenderCallback?) {
        val codes = runCatching {
            val values = JSONObject(params ?: "{}").getJSONArray(TENCENT_STOCK_CODES)
            buildList {
                for (index in 0 until values.length()) {
                    val code = values.optString(index)
                    if (STOCK_CODE_PATTERN.matches(code)) add(code)
                }
            }.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("股票代码不能为空")
        }.getOrElse { error ->
            callbackOnMainThread(callback, failureResponse(error.message ?: "股票代码无效"))
            return
        }

        networkExecutor.execute {
            val response = runCatching { getTencentQuotes(codes) }
                .fold(
                    onSuccess = { successResponse(it) },
                    onFailure = { failureResponse(readableTencentError(it)) },
                )
            callbackOnMainThread(callback, response)
        }
    }

    private fun getTencentQuotes(codes: List<String>): String {
        val connection = URL("$TENCENT_STOCK_ENDPOINT${codes.joinToString(",")}")
            .openConnection() as HttpsURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = STOCK_READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "text/plain")
            connection.setRequestProperty("User-Agent", TENCENT_USER_AGENT)
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) throw TencentStockHttpException(statusCode)
            connection.inputStream.bufferedReader(TENCENT_CHARSET).use { reader ->
                reader.readText().takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("腾讯行情返回为空")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun saveOpenAiProxyUrl(params: String?): Any {
        val rawUrl = runCatching { JSONObject(params ?: "{}").optString(OPENAI_PROXY_URL) }
            .getOrDefault("")
            .trim()
        val validatedUrl = validateProxyUrl(rawUrl)
            ?: return "请输入有效的 HTTPS 服务地址"
        preferences.edit().putString(PREF_OPENAI_PROXY_URL, validatedUrl.toString()).apply()
        return true
    }

    private fun postOpenAiRequest(proxyUrl: URL, payload: String): String {
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
                throw OpenAiHttpException(statusCode, sanitizeErrorDetail(detail))
            }
            val responseBody = readLimited(connection.inputStream, MAX_SUCCESS_BODY_CHARS + 1)
            if (responseBody.length > MAX_SUCCESS_BODY_CHARS) {
                throw IllegalStateException("OpenAI 代理响应过大")
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

    private fun readableOpenAiError(error: Throwable): String = when (error) {
        is OpenAiHttpException -> buildString {
            append("OpenAI 代理返回 HTTP ")
            append(error.statusCode)
            if (error.safeDetail.isNotEmpty()) append(": ${error.safeDetail}")
        }
        is SocketTimeoutException -> "OpenAI 请求超时，请重试"
        else -> "暂时无法连接 OpenAI 代理，请重试"
    }

    private fun readableTencentError(error: Throwable): String = when (error) {
        is TencentStockHttpException -> "腾讯行情服务返回 HTTP ${error.statusCode}"
        is SocketTimeoutException -> "腾讯行情请求超时，请重试"
        else -> "暂时无法连接腾讯行情服务，请重试"
    }

    private fun openAiProxyUrl(): URL? {
        val runtimeUrl = preferences.getString(PREF_OPENAI_PROXY_URL, null).orEmpty()
        return validateProxyUrl(runtimeUrl) ?: validateProxyUrl(BuildConfig.OPENAI_PROXY_URL)
    }

    private fun validateProxyUrl(rawUrl: String): URL? {
        val candidate = rawUrl.trim()
        if (candidate.isEmpty() || candidate.contains('\\')) return null
        return runCatching { URL(candidate) }.getOrNull()?.takeIf { url ->
            url.protocol.equals("https", ignoreCase = true) &&
                url.host.isNotBlank() &&
                url.userInfo == null &&
                url.query == null &&
                url.ref == null
        }
    }

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

        private const val OPENAI_PAYLOAD = "payload"
        private const val OPENAI_PROXY_URL = "url"
        private const val WORK_BUDDY_SUCCESS = "success"
        private const val WORK_BUDDY_DATA = "data"
        private const val WORK_BUDDY_ERROR = "error"
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        private const val STOCK_READ_TIMEOUT_MILLIS = 10_000
        private const val MAX_ERROR_BODY_CHARS = 4_096
        private const val MAX_SUCCESS_BODY_CHARS = 1_000_000
        private const val MAX_SAFE_ERROR_CHARS = 300
        private const val PREFS_NAME = "research_service"
        private const val PREF_OPENAI_PROXY_URL = "openai_proxy_url"
        private const val TENCENT_STOCK_CODES = "codes"
        private const val TENCENT_STOCK_ENDPOINT = "https://qt.gtimg.cn/q="
        private const val TENCENT_USER_AGENT = "Mozilla/5.0 KuiklyStock/1.0"

        private val networkExecutor = Executors.newFixedThreadPool(2)
        private val mainHandler = Handler(Looper.getMainLooper())
        private val TENCENT_CHARSET = Charset.forName("GB18030")
        private val STOCK_CODE_PATTERN = Regex("^(sh|sz|hk|us)[A-Za-z0-9.]{1,12}$")
        private val BEARER_PATTERN = Regex("(?i)Bearer\\s+[^\\s,;]+")
        private val SECRET_PATTERN = Regex(
            """(?i)"?(authorization|access[_-]?token|refresh[_-]?token|token|api[_-]?key|secret|password)"?\s*[:=]\s*"?[^\s,;"]+""",
        )
        private val WHITESPACE_PATTERN = Regex("\\s+")
    }

    private val preferences by lazy {
        KRApplication.application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

private class OpenAiHttpException(
    val statusCode: Int,
    val safeDetail: String,
) : Exception()

private class TencentStockHttpException(val statusCode: Int) : Exception()
