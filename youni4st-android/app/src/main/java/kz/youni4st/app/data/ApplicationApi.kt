package kz.youni4st.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApplicationApi(private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).followRedirects(false).build()) {
    suspend fun send(endpoint: String, token: String, jobId: String, resume: Resume, requestId: String): String = withContext(Dispatchers.IO) {
        val url = safeWebUrl(endpoint) ?: throw IOException("Endpoint not configured")
        // A private file URI is never sent as a photograph. Photo upload needs a separate protected endpoint.
        val body = JSONObject().put("job_id", jobId).put("resume", resume.toJson().apply { remove("photo") })
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token")
            .header("Idempotency-Key", requestId).post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            deliveryReceipt(response.body?.string() ?: "{}")
        }
    }
}

internal fun deliveryReceipt(body: String): String {
    val result = JSONObject(body)
    val id = (result.opt("application_id") as? String)?.trim().orEmpty()
    if (id.isBlank() || result.optString("status") !in setOf("received", "sent")) throw IOException("Missing delivery acknowledgement")
    return id
}
