package com.ourcx.kuiklystock.base

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun closePage() {
        callNativeMethod(CLOSE_PAGE, null, null)
    }

    fun log(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod(LOG, methodArgs, null)
    }

    fun toast(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod("toast", methodArgs, null)
    }

    fun openPage(url: String, closeCurPage: Boolean = false, closeSamePage: Boolean = false, userData: JSONObject? = null, callbackFn: CallbackFn? = null) {
        val methodArgs = JSONObject()
        methodArgs.put("url", url)
        methodArgs.put("closeCurPage", closeCurPage.toInt())
        methodArgs.put("closeSamePage", closeSamePage.toInt())
        userData?.also {
            methodArgs.put("userData", it)
        }
        callNativeMethod(OPEN_PAGE, methodArgs, callbackFn)
    }

    suspend fun ssoRequest(cmd: String, reqParams: JSONObject): JSONObject? {
        return suspendCoroutine<JSONObject?> { continuation ->
            ssoRequest(cmd, reqParams) {
                continuation.resume(it)
            }
        }
    }

    fun ssoRequest(cmd: String, reqParams: JSONObject, responseCallbackFn: CallbackFn) {
        val methodArgs = JSONObject()
        methodArgs.put("cmd", cmd)
        methodArgs.put("reqParam", reqParams)
        callNativeMethod(SSO_REQUEST, methodArgs, responseCallbackFn)
    }

    fun currentTimeStamp(): Long {
        val timestamp = syncCallNativeMethod(CURRENT_TIMESTAMP, null, null)
        return if (timestamp.isNotEmpty()) timestamp.toLong() else 0
    }

    fun dateFormatter(timeStamp: Long, format: String): String {
        val params = JSONObject()
        params.put("timeStamp", timeStamp)
        params.put("format", format)
        return syncCallNativeMethod(DATE_FORMATTER, params, null)
    }

    fun isOpenAiConfigured(): Boolean {
        return when (syncCallNativeMethod(IS_OPENAI_CONFIGURED, null, null).trim().lowercase()) {
            "true", "1" -> true
            else -> false
        }
    }

    fun openAiProxyUrl(): String = syncCallNativeMethod(GET_OPENAI_PROXY_URL, null, null)

    fun openAiModel(): String = syncCallNativeMethod(GET_OPENAI_MODEL, null, null)

    fun hasOpenAiToken(): Boolean = syncCallNativeMethod(HAS_OPENAI_TOKEN, null, null)
        .trim().lowercase() in setOf("true", "1")

    fun saveOpenAiConfiguration(baseUrl: String, token: String, model: String): Result<Unit> {
        val params = JSONObject()
            .put(OPENAI_PROXY_URL, baseUrl)
            .put(OPENAI_TOKEN, token)
            .put(OPENAI_MODEL, model)
        val response = syncCallNativeMethod(SAVE_OPENAI_CONFIGURATION, params, null)
        return if (response == "true") {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException(response.ifBlank { INVALID_WORK_BUDDY_PROXY_URL }))
        }
    }

    fun clearOpenAiConfiguration() {
        syncCallNativeMethod(CLEAR_OPENAI_CONFIGURATION, null, null)
    }

    fun requestOpenAi(payload: String, callback: (Result<String>) -> Unit) {
        val params = JSONObject().put(OPENAI_PAYLOAD, payload)
        callNativeMethod(REQUEST_OPENAI, params) { response ->
            callback(
                response?.let(::parseNativeResponse)
                    ?: Result.failure(IllegalStateException(OPENAI_EMPTY_RESPONSE_ERROR)),
            )
        }
    }

    fun requestTencentQuotes(codes: List<String>, callback: (Result<String>) -> Unit) {
        val codeArray = JSONArray().apply {
            codes.forEach { put(it) }
        }
        val params = JSONObject().put(TENCENT_STOCK_CODES, codeArray)
        callNativeMethod(REQUEST_TENCENT_QUOTES, params) { response ->
            callback(
                response?.let(::parseNativeResponse)
                    ?: Result.failure(IllegalStateException(TENCENT_STOCK_EMPTY_RESPONSE_ERROR)),
            )
        }
    }

    private fun parseNativeResponse(response: JSONObject): Result<String> =
        runCatching {
            if (!response.has(WORK_BUDDY_SUCCESS)) {
                throw IllegalStateException("Native response is missing the success field")
            }
            if (!response.optBoolean(WORK_BUDDY_SUCCESS, false)) {
                val message = response.optString(RESPONSE_ERROR, DEFAULT_REQUEST_ERROR)
                throw IllegalStateException(message.ifEmpty { DEFAULT_REQUEST_ERROR })
            }
            if (!response.has(WORK_BUDDY_DATA)) {
                throw IllegalStateException("Native response is missing the data field")
            }
            response.optString(WORK_BUDDY_DATA)
        }

    private fun callNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?) {
        toNative(false, methodName, data?.toString(), callbackFn, false)
    }

    private fun syncCallNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?): String {
        return toNative(false, methodName, data?.toString(), callbackFn, true).toString()
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        const val OPEN_PAGE = "openPage"
        const val CLOSE_PAGE = "closePage"
        const val LOG = "log"
        const val SSO_REQUEST = "ssoRequest"
        const val CURRENT_TIMESTAMP = "currentTimestamp"
        const val DATE_FORMATTER = "dateFormatter"
        const val IS_OPENAI_CONFIGURED = "isOpenAiConfigured"
        const val GET_OPENAI_PROXY_URL = "getOpenAiProxyUrl"
        const val GET_OPENAI_MODEL = "getOpenAiModel"
        const val HAS_OPENAI_TOKEN = "hasOpenAiToken"
        const val SAVE_OPENAI_CONFIGURATION = "saveOpenAiConfiguration"
        const val CLEAR_OPENAI_CONFIGURATION = "clearOpenAiConfiguration"
        const val REQUEST_OPENAI = "requestOpenAi"
        const val REQUEST_TENCENT_QUOTES = "requestTencentQuotes"

        private const val OPENAI_PAYLOAD = "payload"
        private const val TENCENT_STOCK_CODES = "codes"
        private const val OPENAI_PROXY_URL = "url"
        private const val OPENAI_TOKEN = "token"
        private const val OPENAI_MODEL = "model"
        private const val WORK_BUDDY_SUCCESS = "success"
        private const val WORK_BUDDY_DATA = "data"
        private const val RESPONSE_ERROR = "error"
        private const val DEFAULT_REQUEST_ERROR = "请求失败"
        private const val OPENAI_EMPTY_RESPONSE_ERROR = "OpenAI 响应为空"
        private const val TENCENT_STOCK_EMPTY_RESPONSE_ERROR = "腾讯行情请求未返回结果"
        private const val INVALID_WORK_BUDDY_PROXY_URL = "请输入 HTTPS 地址，或本地网络 HTTP 地址"
    }
}
