package kz.youni4st.app

import kotlinx.coroutines.runBlocking
import kz.youni4st.app.data.ChatApi
import kz.youni4st.app.data.ChatMessage
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ChatApiTest {
    @Test fun `chat uses backend contract and limits history to ten messages`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"reply":"Привет, Әлия!"}"""))
            server.start()
            val api = ChatApi(server.url("/").toString())
            val result = api.reply("Какие вузы?", (0..14).map { ChatMessage(if (it % 2 == 0) "user" else "assistant", "message $it") })
            assertEquals("Привет, Әлия!", result)
            val request = server.takeRequest()
            assertEquals("/api/chat", request.path)
            assertEquals("POST", request.method)
            val body = JSONObject(request.body.readUtf8())
            assertEquals("Какие вузы?", body.getString("message"))
            assertEquals(10, body.getJSONArray("history").length())
            assertEquals("message 5", body.getJSONArray("history").getJSONObject(0).getString("content"))
            assertFalse(body.toString().contains("telegram_id"))
        }
    }
    @Test fun `server failures and empty replies never become fake AI answers`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(500).setBody("private server error"))
            server.enqueue(MockResponse().setBody("""{"reply":""}"""))
            server.start()
            val api = ChatApi(server.url("/").toString())
            repeat(2) {
                try { api.reply("test", emptyList()); fail("Expected failure") }
                catch (e: IOException) { assertFalse(e.message.orEmpty().contains("private server error")) }
            }
        }
    }
}
