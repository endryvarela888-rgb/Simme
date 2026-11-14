package com.simmer.app

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.HttpException
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private interface SimmerEndpoints {
    @GET suspend fun get(@Url path: String, @Header("Authorization") authorization: String? = null): ResponseBody
    @POST suspend fun post(@Url path: String, @Body body: RequestBody, @Header("Authorization") authorization: String? = null): ResponseBody
    @DELETE suspend fun delete(@Url path: String, @Body body: RequestBody? = null, @Header("Authorization") authorization: String? = null): ResponseBody?
}

private val simmerEndpoints: SimmerEndpoints by lazy {
    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).build().create(SimmerEndpoints::class.java)
}

/** Retrofit transport shared by auth and recipe screens. */
suspend fun simmerRequest(path: String, token: String?, body: JSONObject?, method: String): String {
    val auth = token?.let { "Bearer $it" }
    return try {
        when (method) {
            "DELETE" -> simmerEndpoints.delete(path, body?.toString()?.toRequestBody("application/json".toMediaType()), auth)?.string().orEmpty()
            "POST" -> simmerEndpoints.post(path, (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()), auth).string()
            else -> simmerEndpoints.get(path, auth).string()
        }
    } catch (error: HttpException) {
        val responseText = error.response()?.errorBody()?.string().orEmpty()
        if (error.code() == 401 && path != "auth/refresh/" && !AppSessionRefresh.value.isNullOrBlank()) {
            val refreshed = simmerRequest("auth/refresh/", null, JSONObject().put("refresh", AppSessionRefresh.value), "POST")
            val json = JSONObject(refreshed)
            val access = json.optString("access").takeIf(String::isNotBlank)
            if (access != null) {
                AppSessionRefresh.onRefreshed(access, json.optString("refresh").takeIf(String::isNotBlank))
                return simmerRequest(path, access, body, method)
            }
        }
        val errorJson = runCatching { JSONObject(responseText) }.getOrNull()
        val detail = errorJson?.optString("detail")?.takeIf(String::isNotBlank)
        val fieldError = errorJson?.keys()?.asSequence()?.mapNotNull { field ->
            val value = errorJson.opt(field)
            val message = when (value) {
                is org.json.JSONArray -> value.optString(0)
                is String -> value
                else -> ""
            }
            message.takeIf(String::isNotBlank)?.let { "$field: $it" }
        }?.firstOrNull()
        throw IllegalStateException(detail ?: fieldError ?: "Request failed (${error.code()})")
    }
}

/** Small bridge avoids coupling the Retrofit service to Compose state. */
object AppSessionRefresh {
    var value: String? = null
    var onRefreshed: (String, String?) -> Unit = { _, _ -> }
}
