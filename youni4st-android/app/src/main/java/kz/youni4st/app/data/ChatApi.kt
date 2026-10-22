package kz.youni4st.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)

class ChatApi(private val baseUrl: String, private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS).readTimeout(70, TimeUnit.SECONDS)
    .callTimeout(90, TimeUnit.SECONDS).build()) {
    suspend fun reply(message: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val previous = JSONArray().apply {
            history.takeLast(10).forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
        }
        val body = JSONObject().put("message", message).put("history", previous)
        val request = Request.Builder().url("${baseUrl.trimEnd('/')}/api/chat")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val answer = JSONObject(response.body?.string() ?: "{}").optString("reply").trim()
            if (answer.isBlank()) throw IOException("Empty reply")
            answer
        }
    }
}
