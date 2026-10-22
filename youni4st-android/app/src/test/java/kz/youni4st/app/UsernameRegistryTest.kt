package kz.youni4st.app

import kz.youni4st.app.data.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UsernameRegistryTest {
    @Test fun namesAreCanonicalAndCannotContainPaths() {
        assertEquals("wombly",UsernamePolicy.normalize(" @Wombly "))
        listOf("wombly","x_1","a".repeat(24)).forEach {assertTrue(UsernamePolicy.valid(it))}
        listOf("a","12user","user/name","user.name","руслан","a".repeat(25)).forEach {assertFalse(UsernamePolicy.valid(it))}
    }
    @Test fun firstClaimIsOneAtomicCommitWithCreatePreconditions() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
            server.enqueue(MockResponse().setBody("{}"))
            val result=UsernameRegistry("demo-test",OkHttpClient(),server.url("/v1/").toString()).claim("user1","@Wombly","test-token")
            assertEquals("wombly",result)
            val get=server.takeRequest();assertTrue(get.path!!.endsWith("/handles/user1"))
            val post=server.takeRequest();assertEquals("Bearer test-token",post.getHeader("Authorization"))
            assertTrue(post.path!!.endsWith("/documents:commit"))
            val writes=JSONObject(post.body.readUtf8()).getJSONArray("writes")
            assertEquals(2,writes.length())
            for(i in 0..1) assertFalse(writes.getJSONObject(i).getJSONObject("currentDocument").getBoolean("exists"))
        }
    }
    @Test fun renameGuardsOldVersionAndDeletesOldClaimInSameCommit() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""{"fields":{"username":{"stringValue":"oldname"}},"updateTime":"2026-10-22T00:00:00Z"}"""))
            server.enqueue(MockResponse().setBody("{}"))
            UsernameRegistry("demo-test",OkHttpClient(),server.url("/").toString()).claim("u1","newname","token")
            server.takeRequest()
            val writes=JSONObject(server.takeRequest().body.readUtf8()).getJSONArray("writes")
            assertEquals(3,writes.length())
            assertEquals("2026-10-22T00:00:00Z",writes.getJSONObject(1).getJSONObject("currentDocument").getString("updateTime"))
            assertTrue(writes.getJSONObject(2).getString("delete").endsWith("/usernames/oldname"))
        }
    }
    @Test fun duplicateDoesNotReturnSuccess() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
            server.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":{"status":"ALREADY_EXISTS"}}"""))
            try {UsernameRegistry("demo-test",OkHttpClient(),server.url("/").toString()).claim("u2","wombly","token");fail("Must reject duplicate")}
            catch(e:RegistryException){assertEquals("ALREADY_EXISTS",e.code)}
        }
    }
    @Test fun missingRulesFailClosed() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":{"status":"PERMISSION_DENIED"}}"""))
            try {UsernameRegistry("demo-test",OkHttpClient(),server.url("/").toString()).claim("u2","wombly","token");fail("Must not claim locally")}
            catch(e:RegistryException){assertEquals("PERMISSION_DENIED",e.code)}
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun retryAfterLostResponseIsIdempotent() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""{"fields":{"username":{"stringValue":"wombly"}},"updateTime":"now"}"""))
            assertEquals("wombly",UsernameRegistry("demo-test",OkHttpClient(),server.url("/").toString()).claim("u1","WOMBLY","token"))
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun overflowCannotShiftNationalNumber() {
        for(number in listOf("7771234567","8701234567","7011234567")) {
            assertEquals(number,editPhoneDigits(number,number+"9"))
            assertEquals(number,editPhoneDigits(number,number.take(3)+"9"+number.drop(3)))
        }
    }
    @Test fun phonePasteDeleteAndReplacementRemainPossible() {
        assertEquals("7011234567",editPhoneDigits("","+7 (701) 123-45-67"))
        assertEquals("7011234567",editPhoneDigits("","87011234567"))
        assertEquals("777123456",editPhoneDigits("7771234567","777123456"))
        assertEquals("7779234567",editPhoneDigits("7771234567","7779234567"))
        assertEquals("",editPhoneDigits("777",""))
    }
}
