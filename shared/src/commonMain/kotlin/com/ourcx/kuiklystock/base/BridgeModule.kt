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

    fun isWorkBuddyConfigured(): Boolean {
        return when (syncCallNativeMethod(IS_WORK_BUDDY_CONFIGURED, null, null).trim().lowercase()) {
            "true", "1" -> true
            else -> false
        }
    }

    fun workBuddyProxyUrl(): String =
        syncCallNativeMethod(GET_WORK_BUDDY_PROXY_URL, null, null)

    fun saveWorkBuddyProxyUrl(url: String): Result<Unit> {
        val params = JSONObject().put(WORK_BUDDY_PROXY_URL, url)
        val response = syncCallNativeMethod(SAVE_WORK_BUDDY_PROXY_URL, params, null)
        return if (response == "true") {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException(response.ifBlank { INVALID_WORK_BUDDY_PROXY_URL }))
        }
    }

    fun clearWorkBuddyProxyUrl() {
        syncCallNativeMethod(CLEAR_WORK_BUDDY_PROXY_URL, null, null)
    }

    fun requestWorkBuddy(payload: String, callback: (Result<String>) -> Unit) {
        val params = JSONObject().put(WORK_BUDDY_PAYLOAD, payload)
        callNativeMethod(REQUEST_WORK_BUDDY, params) { response ->
            callback(
                response?.let(::parseWorkBuddyResponse)
                    ?: Result.failure(IllegalStateException(WORK_BUDDY_EMPTY_RESPONSE_ERROR)),
            )
        }
    }

    private fun parseWorkBuddyResponse(response: JSONObject): Result<String> =
        runCatching {
            if (!response.has(WORK_BUDDY_SUCCESS)) {
                throw IllegalStateException("WorkBuddy response is missing the success field")
            }
            if (!response.optBoolean(WORK_BUDDY_SUCCESS, false)) {
                val message = response.optString(WORK_BUDDY_ERROR, WORK_BUDDY_DEFAULT_ERROR)
                throw IllegalStateException(message.ifEmpty { WORK_BUDDY_DEFAULT_ERROR })
            }
            if (!response.has(WORK_BUDDY_DATA)) {
                throw IllegalStateException("WorkBuddy response is missing the data field")
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
        const val IS_WORK_BUDDY_CONFIGURED = "isWorkBuddyConfigured"
        const val GET_WORK_BUDDY_PROXY_URL = "getWorkBuddyProxyUrl"
        const val SAVE_WORK_BUDDY_PROXY_URL = "saveWorkBuddyProxyUrl"
        const val CLEAR_WORK_BUDDY_PROXY_URL = "clearWorkBuddyProxyUrl"
        const val REQUEST_WORK_BUDDY = "requestWorkBuddy"

        private const val WORK_BUDDY_PAYLOAD = "payload"
        private const val WORK_BUDDY_PROXY_URL = "url"
        private const val WORK_BUDDY_SUCCESS = "success"
        private const val WORK_BUDDY_DATA = "data"
        private const val WORK_BUDDY_ERROR = "error"
        private const val WORK_BUDDY_DEFAULT_ERROR = "WorkBuddy request failed"
        private const val WORK_BUDDY_EMPTY_RESPONSE_ERROR = "WorkBuddy response is empty"
        private const val INVALID_WORK_BUDDY_PROXY_URL = "请输入有效的 HTTPS 服务地址"
    }
}
