package kz.youni4st.app

import kz.youni4st.app.data.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class CloudAccountTest {
    @Test fun restoresProfileResumeAndChatFromAnotherDevice() {
        MockWebServer().use {server->
            val state=JSONObject().put("profile",UserProfile(name="Руслан",username="wombly",interests=listOf("IT")).toJson())
                .put("resume",Resume(name="Руслан",salary="100000",work=listOf(ResumeRecord(title="Intern"))).toJson())
                .put("chat",JSONArray().put(JSONObject().put("role","user").put("content","Как выбрать вуз?")))
            server.enqueue(MockResponse().setBody(JSONObject().put("fields",FirestoreValues.encodeFields(state)).toString()))
            val restored=CloudAccountApi("demo-test",OkHttpClient(),server.url("/").toString()).read("alice","id-token")!!
            assertEquals("Руслан",UserProfile.fromJson(restored.getJSONObject("profile")).name)
            assertEquals("Intern",Resume.fromJson(restored.getJSONObject("resume")).work.single().title)
            assertEquals("Как выбрать вуз?",restored.getJSONArray("chat").getJSONObject(0).getString("content"))
            val request=server.takeRequest();assertEquals("Bearer id-token",request.getHeader("Authorization"));assertTrue(request.path!!.endsWith("/user_data/alice"))
        }
    }
    @Test fun writesOnlyTouchedFavoriteNotTheWholeSet() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("{}"))
            val fields=JSONObject();val path=CloudItems.patch(fields,"favorites","universities:5",false)
            CloudAccountApi("demo-test",OkHttpClient(),server.url("/").toString()).write("alice","token",fields,listOf(path))
            val request=server.takeRequest()
            assertEquals("PATCH",request.method)
            assertEquals(listOf(path,"schemaVersion"),request.requestUrl!!.queryParameterValues("updateMask.fieldPaths"))
            assertTrue(path.matches(Regex("favorites\\.i[0-9a-f]{64}")))
            val body=FirestoreValues.decodeFields(JSONObject(request.body.readUtf8()).getJSONObject("fields"))
            assertEquals(1,body.getJSONObject("favorites").length())
            assertFalse(body.getJSONObject("favorites").getJSONObject(CloudItems.hash("universities:5")).getBoolean("selected"))
            assertFalse(body.has("resume"))
        }
    }
    @Test fun twoDevicesFavoritesMergeWithoutDroppingPendingLocalEdits() {
        val remote=JSONObject().put(CloudItems.hash("uni:a"),JSONObject().put("id","uni:a").put("selected",true))
        val local=setOf("uni:b")
        assertEquals(setOf("uni:a","uni:b"),CloudMerge.items(remote,local,setOf("favorites|uni:b"),"favorites"))
        assertEquals(emptySet<String>(),CloudMerge.items(remote,emptySet(),setOf("favorites|uni:a"),"favorites"))
    }
    @Test fun deletionTombstoneIsNotRestoredFromOldLocalCache() {
        val remote=JSONObject().put(CloudItems.hash("uni:a"),JSONObject().put("id","uni:a").put("selected",false))
        assertEquals(emptySet<String>(),CloudMerge.items(remote,setOf("uni:a"),emptySet(),"favorites"))
    }
    @Test fun missingRulesAreReportedAndNotTreatedAsEmptyCloud() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":{"status":"PERMISSION_DENIED"}}"""))
            try {CloudAccountApi("demo-test",OkHttpClient(),server.url("/").toString()).read("alice","token");fail("Must fail closed")}
            catch(e:RegistryException){assertEquals("PERMISSION_DENIED",e.code)}
        }
    }
    @Test fun oversizedCloudCopyCannotBeReportedAsSaved() {
        MockWebServer().use {server->
            val data=JSONObject().put("profile",JSONObject().put("name","x".repeat(960_000)))
            try {CloudAccountApi("demo-test",OkHttpClient(),server.url("/").toString()).write("alice","token",data,listOf("profile"));fail("Must reject oversized document")}
            catch(e:RegistryException){assertEquals("CLOUD_DATA_TOO_LARGE",e.code)}
            assertEquals(0,server.requestCount)
        }
    }
    @Test fun typedEmptyCollectionsAndNullRoundTrip() {
        val source=JSONObject().put("empty",JSONArray()).put("object",JSONObject()).put("null",JSONObject.NULL).put("flag",false).put("n",3)
        val restored=FirestoreValues.decodeFields(FirestoreValues.encodeFields(source))
        assertEquals(0,restored.getJSONArray("empty").length());assertEquals(0,restored.getJSONObject("object").length())
        assertTrue(restored.isNull("null"));assertFalse(restored.getBoolean("flag"));assertEquals(3,restored.getInt("n"))
    }
    @Test fun logoAndTextDurationIsTwoSeconds() {assertEquals(2000,StartupSettings.INTRO_DURATION_MS)}
    @Test fun signInDoesNotReplayTheSameReturnAnimation() {
        val gate=IntroReplayGate()
        assertFalse(gate.consume(0));assertTrue(gate.consume(1));assertFalse(gate.consume(1));assertTrue(gate.consume(2))
    }
    @Test fun rotationResetsTheActivityReturnCounterWithoutPlayingAnIntro() {
        val gate=IntroReplayGate()
        assertTrue(gate.consume(3));assertFalse(gate.consume(0));assertTrue(gate.consume(1))
    }
}
