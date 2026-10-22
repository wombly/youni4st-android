package kz.youni4st.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.json.JSONArray
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Firestore REST with Firebase ID tokens: access is enforced by server-side rules. */
class CloudAccountApi(projectId:String,
    private val client:OkHttpClient=OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).build(),
    baseUrl:String="https://firestore.googleapis.com/v1/") {
    private val endpoint=baseUrl.trimEnd('/')+"/projects/$projectId/databases/(default)/documents/user_data/"
    fun read(uid:String,token:String):JSONObject? {
        val req=Request.Builder().url(endpoint+uid).header("Authorization","Bearer $token").build()
        client.newCall(req).execute().use {r ->
            val body=JSONObject(r.body?.string().orEmpty().ifBlank {"{}"})
            if(r.code==404)return null
            if(!r.isSuccessful)throw RegistryException(body.optJSONObject("error")?.optString("status") ?: "UNAVAILABLE")
            return FirestoreValues.decodeFields(body.optJSONObject("fields") ?: JSONObject())
        }
    }
    /** Only touched fields are patched; another device's unrelated edits are retained. */
    fun write(uid:String,token:String,fields:JSONObject,paths:List<String>) {
        val target=(endpoint+uid).toHttpUrl().newBuilder()
        (paths+"schemaVersion").distinct().forEach {target.addQueryParameter("updateMask.fieldPaths",it)}
        fields.put("schemaVersion",1)
        val encoded=JSONObject().put("fields",FirestoreValues.encodeFields(fields)).toString()
        if(encoded.toByteArray(Charsets.UTF_8).size>950_000)throw RegistryException("CLOUD_DATA_TOO_LARGE")
        val req=Request.Builder().url(target.build()).header("Authorization","Bearer $token")
            .patch(encoded.toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use {r ->if(!r.isSuccessful) {
            val body=JSONObject(r.body?.string().orEmpty().ifBlank {"{}"})
            throw RegistryException(body.optJSONObject("error")?.optString("status") ?: "UNAVAILABLE")
        }}
    }
}

object FirestoreValues {
    fun encodeFields(o:JSONObject)=JSONObject().apply {o.keys().forEach {put(it,encode(o.get(it)))}}
    fun decodeFields(o:JSONObject)=JSONObject().apply {o.keys().forEach {put(it,decode(o.getJSONObject(it)))}}
    private fun encode(v:Any?):JSONObject=when(v) {
        null,JSONObject.NULL->JSONObject().put("nullValue","NULL_VALUE")
        is Boolean->JSONObject().put("booleanValue",v)
        is Byte,is Short,is Int,is Long->JSONObject().put("integerValue",v.toString())
        is Number->JSONObject().put("doubleValue",v)
        is JSONObject->JSONObject().put("mapValue",JSONObject().put("fields",encodeFields(v)))
        is JSONArray->JSONObject().put("arrayValue",JSONObject().put("values",JSONArray((0 until v.length()).map {encode(v.get(it))})))
        else->JSONObject().put("stringValue",v.toString())
    }
    private fun decode(v:JSONObject):Any=when {
        v.has("stringValue")->v.getString("stringValue")
        v.has("booleanValue")->v.getBoolean("booleanValue")
        v.has("integerValue")->v.getString("integerValue").toLong()
        v.has("doubleValue")->v.getDouble("doubleValue")
        v.has("mapValue")->decodeFields(v.getJSONObject("mapValue").optJSONObject("fields") ?: JSONObject())
        v.has("arrayValue")->v.getJSONObject("arrayValue").optJSONArray("values")?.let {a->JSONArray((0 until a.length()).map {decode(a.getJSONObject(it))})} ?: JSONArray()
        else->JSONObject.NULL
    }
}

object CloudItems {
    fun hash(id:String)="i"+MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") {"%02x".format(it)}
    fun values(map:JSONObject?):Set<String> = map?.keys()?.asSequence()?.mapNotNull {key ->
        map.optJSONObject(key)?.takeIf {it.optBoolean("selected")}?.optString("id")?.takeIf {it.isNotBlank()}
    }?.toSet() ?: emptySet()
    fun patch(fields:JSONObject,collection:String,id:String,selected:Boolean):String {
        val map=fields.optJSONObject(collection) ?: JSONObject().also {fields.put(collection,it)}
        val hash=hash(id)
        map.put(hash,JSONObject().put("id",id).put("selected",selected))
        return "$collection.$hash"
    }
}
