package kz.youni4st.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object UsernamePolicy {
    fun normalize(value:String)=value.trim().removePrefix("@").lowercase(Locale.ROOT)
    fun valid(value:String)=Regex("[a-z][a-z0-9_]{2,23}").matches(normalize(value))
}

class RegistryException(val code:String):Exception(code)

/** Firebase ID tokens are checked by Firestore Security Rules, not by this client.
 * Run on Dispatchers.IO. The atomic commit and exists=false prevent duplicate claims.
 */
class UsernameRegistry(
    projectId:String,
    private val client:OkHttpClient=OkHttpClient.Builder().callTimeout(20,TimeUnit.SECONDS).build(),
    baseUrl:String="https://firestore.googleapis.com/v1/"
) {
    private val documents="projects/$projectId/databases/(default)/documents"
    private val endpoint=baseUrl.trimEnd('/')+"/"+documents
    data class Registration(val username:String,val updateTime:String)

    fun lookup(uid:String,token:String):Registration? {
        val request=Request.Builder().url("$endpoint/handles/$uid").header("Authorization","Bearer $token").build()
        client.newCall(request).execute().use {response ->
            val body=JSONObject(response.body?.string().orEmpty().ifBlank {"{}"})
            if(response.code==404) return null
            if(!response.isSuccessful) throw RegistryException(body.optJSONObject("error")?.optString("status") ?: "UNAVAILABLE")
            return Registration(body.getJSONObject("fields").getJSONObject("username").getString("stringValue"),body.getString("updateTime"))
        }
    }

    fun claim(uid:String,value:String,token:String):String {
        val username=UsernamePolicy.normalize(value)
        if(!UsernamePolicy.valid(username)) throw RegistryException("INVALID_USERNAME")
        val old=lookup(uid,token)
        if(old?.username==username) return username // also recovers a lost successful response
        val writes=JSONArray()
        writes.put(JSONObject().put("update",document("usernames/$username","uid",uid))
            .put("currentDocument",JSONObject().put("exists",false)))
        writes.put(JSONObject().put("update",document("handles/$uid","username",username))
            .put("currentDocument",if(old==null) JSONObject().put("exists",false) else JSONObject().put("updateTime",old.updateTime)))
        if(old!=null) writes.put(JSONObject().put("delete","$documents/usernames/${old.username}")
            .put("currentDocument",JSONObject().put("exists",true)))
        val request=Request.Builder().url("$endpoint:commit").header("Authorization","Bearer $token")
            .post(JSONObject().put("writes",writes).toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use {response ->
            if(!response.isSuccessful) {
                val body=JSONObject(response.body?.string().orEmpty().ifBlank {"{}"})
                throw RegistryException(body.optJSONObject("error")?.optString("status") ?: "UNAVAILABLE")
            }
        }
        return username
    }
    private fun document(path:String,key:String,value:String)=JSONObject().put("name","$documents/$path")
        .put("fields",JSONObject().put(key,JSONObject().put("stringValue",value)))
}
