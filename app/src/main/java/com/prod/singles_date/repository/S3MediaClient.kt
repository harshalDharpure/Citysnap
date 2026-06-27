package com.prod.singles_date.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.prod.singles_date.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class S3MediaClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    suspend fun uploadJpeg(
        kind: String,
        file: File,
        thoughtId: String? = null,
        index: Int? = null,
    ): String {
        val uploadInfo = requestUploadUrl(kind, file.length(), thoughtId, index)
        val uploadUrl = uploadInfo.getString("uploadUrl")
        val publicUrl = uploadInfo.getString("publicUrl")

        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(file.asRequestBody("image/jpeg".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val detail = response.body?.string()?.take(200)?.trim().orEmpty()
                    val code = response.code
                    error(
                        when (code) {
                            403 -> "S3 blocked upload (403). Check bucket name, region, and Lambda S3 permissions."
                            404 -> "S3 bucket not found (404). Fix AWS_S3_BUCKET in Lambda."
                            else -> detail.takeIf { it.isNotBlank() } ?: "S3 upload failed ($code)"
                        }.also { Log.e(TAG, "S3 PUT failed: $it") },
                    )
                }
            }
        }
        return publicUrl
    }

    suspend fun deletePrefix(prefix: String) {
        runCatching {
            getJson(
                path = "/delete-prefix",
                params = mapOf("prefix" to prefix),
            )
        }.onFailure { e ->
            Log.w(TAG, "deletePrefix failed for $prefix", e)
        }
    }

    private suspend fun requestUploadUrl(
        kind: String,
        contentLength: Long,
        thoughtId: String?,
        index: Int?,
    ): JSONObject {
        val params = buildMap<String, String> {
            put("kind", kind)
            put("contentType", "image/jpeg")
            put("contentLength", contentLength.toString())
            if (thoughtId != null) put("thoughtId", thoughtId)
            if (index != null) put("index", index.toString())
        }
        return getJson("/upload-url", params) ?: error("Invalid upload response")
    }

    private suspend fun getJson(path: String, params: Map<String, String>): JSONObject? {
        val token = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
            ?: error("Sign in required")

        return withContext(Dispatchers.IO) {
            val url = requireApiBase().toHttpUrl().newBuilder().apply {
                addPathSegments(path.trimStart('/'))
                params.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()

            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody(null))
                .addHeader("Authorization", "Bearer $token")
                .build()

            Log.d(TAG, "POST $path (auth header set)")

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = parseApiError(raw, response.code)
                    Log.e(TAG, "POST $path failed: $message")
                    error(message)
                }
                if (raw.isBlank()) null else JSONObject(raw)
            }
        }
    }

    companion object {
        private const val TAG = "HoghtS3"

        private fun parseApiError(raw: String, code: Int): String {
            runCatching {
                val json = JSONObject(raw)
                json.optString("error").takeIf { it.isNotBlank() }?.let { return it }
                json.optString("message").takeIf { it.isNotBlank() }?.let { message ->
                    if (message.equals("Internal Server Error", ignoreCase = true)) {
                        return "Upload API misconfigured. In API Gateway set route Authorization to NONE, upload function-upload.zip to Lambda, then try again."
                    }
                    return message
                }
            }
            return raw.takeIf { it.isNotBlank() } ?: "Upload request failed ($code)"
        }

        private fun requireApiBase(): String {
            val base = BuildConfig.S3_API_BASE_URL.trim().trimEnd('/')
            if (base.isBlank()) error("Photo upload is not configured.")
            return base
        }

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
        }
    }
}
