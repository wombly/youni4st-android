package kz.youni4st.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Resume(
    val name: String = "", val phone: String = "", val salary: String = "",
    val city: String = "", val experience: String = "", val education: String = "",
    val skills: String = "", val photo: String = "",
    val position: String = "", val birthday: String = "", val gender: String = "",
    val educationLevel: String = "", val telegram: String = "", val about: String = "",
    val work: List<ResumeRecord> = emptyList(), val studies: List<ResumeRecord> = emptyList(),
    val languages: List<ResumeRecord> = emptyList(), val interests: List<String> = emptyList(), val email: String = "",
    val skillItems: List<String> = emptyList(), val employmentTypes: List<String> = emptyList()
) {
    val allSkills:List<String> get() = (if(skillItems.isNotEmpty())skillItems else skills.split(Regex("[\\n;,]+"))).map(String::trim).filter(String::isNotBlank).distinct()
    val completion: Int get() = listOf(name, phone, salary, city, experience.ifBlank { work.joinToString() }, education.ifBlank { studies.joinToString() }, allSkills.joinToString().ifBlank { languages.joinToString() }, photo).count { it.isNotBlank() } * 100 / 8
    fun toJson() = JSONObject().put("name", name).put("phone", phone).put("salary", salary)
        .put("city", city).put("experience", experience).put("education", education).put("skills", skills).put("photo", photo)
        .put("position",position).put("birthday",birthday).put("gender",gender).put("educationLevel",educationLevel).put("telegram",telegram).put("about",about)
        .put("work",JSONArray(work.map { it.toJson() })).put("studies",JSONArray(studies.map { it.toJson() }))
        .put("languages",JSONArray(languages.map { it.toJson() })).put("interests",JSONArray(interests)).put("email",email).put("skillItems",JSONArray(skillItems)).put("employmentTypes",JSONArray(employmentTypes))
    companion object {
        fun fromJson(o: JSONObject) = Resume(o.optString("name"), o.optString("phone"), o.optString("salary"),
            o.optString("city"), o.optString("experience"), o.optString("education"), o.optString("skills"), o.optString("photo"),
            o.optString("position"),o.optString("birthday"),o.optString("gender"),o.optString("educationLevel"),o.optString("telegram"),o.optString("about"),
            o.records("work"),o.records("studies"),o.records("languages"),o.strings("interests"),o.optString("email"),o.strings("skillItems"),o.strings("employmentTypes"))
    }
}

class LocalStore(private val prefs:android.content.SharedPreferences) {
    constructor(context:Context):this(context.getSharedPreferences("youni4st_v1",Context.MODE_PRIVATE))
    var activeUid:String?=null
        private set
    private var tracking=true
    private fun key(name:String)=activeUid?.let {"account:$it:$name"} ?: name
    var welcomed:Boolean
        get()=prefs.getBoolean("welcomed",false)
        set(value){prefs.edit().putBoolean("welcomed",value).apply()}
    fun activate(uid:String?) {
        if(uid!=null && !prefs.getBoolean("account:$uid:initialized",false)) {
            val owner=prefs.getString("profile_migrated_uid",null)
            val migrate=owner==null || owner==uid
            val editor=prefs.edit().putBoolean("account:$uid:initialized",true)
            if(owner==null)editor.putString("profile_migrated_uid",uid)
            if(migrate) {
                for(name in listOf("resume","chat","language")) prefs.getString(name,null)?.let {editor.putString("account:$uid:$name",it)}
                for(name in listOf("favorites","drafts"))editor.putStringSet("account:$uid:$name",prefs.getStringSet(name,emptySet())!!.toSet())
                val receipts=runCatching {JSONObject(prefs.getString("receipts","{}")!!)}.getOrDefault(JSONObject())
                val owned=JSONObject();receipts.keys().forEach {if(it.startsWith("$uid|"))owned.put(it,receipts.get(it))}
                editor.putString("account:$uid:receipts",owned.toString())
            }
            val legacyProfile=prefs.getString("profile:$uid",null) ?: if(migrate)prefs.getString("profile",null) else null
            if(legacyProfile!=null)editor.putString("account:$uid:profile",legacyProfile)
            editor.apply()
        }
        activeUid=uid
    }
    fun withoutTracking(block:()->Unit) {val old=tracking;tracking=false;try{block()}finally{tracking=old}}
    private fun revision(editor:android.content.SharedPreferences.Editor,changes:List<String>) {
        val uid=activeUid ?: return
        if(!tracking || changes.isEmpty())return
        val dirty=JSONObject(prefs.getString("account:$uid:dirty","{}")!!)
        val next=prefs.getLong("account:$uid:revision",0)+1
        changes.forEach {dirty.put(it,next)}
        editor.putLong("account:$uid:revision",next).putString("account:$uid:dirty",dirty.toString())
    }
    private fun string(name:String,value:String,section:String=name) {
        if(prefs.getString(key(name),null)==value)return
        val e=prefs.edit().putString(key(name),value);revision(e,listOf(section));e.apply()
    }
    private fun set(name:String,value:Set<String>) {
        val previous=prefs.getStringSet(key(name),emptySet())!!.toSet()
        if(previous==value)return
        val changes=((previous-value)+(value-previous)).map {"$name|$it"}
        val e=prefs.edit().putStringSet(key(name),value.toSet());revision(e,changes);e.apply()
    }
    fun mark(section:String){val e=prefs.edit();revision(e,listOf(section));e.apply()}
    fun pending(uid:String):Map<String,Long> {
        val o=JSONObject(prefs.getString("account:$uid:dirty","{}")!!)
        return o.keys().asSequence().associateWith {o.getLong(it)}
    }
    fun acknowledge(uid:String,sent:Map<String,Long>) {
        val o=JSONObject(prefs.getString("account:$uid:dirty","{}")!!)
        sent.forEach {(k,v)->if(o.optLong(k,-1)==v)o.remove(k)}
        prefs.edit().putString("account:$uid:dirty",o.toString()).apply()
    }
    fun hasCloud(uid:String)=prefs.getBoolean("account:$uid:has_cloud",false)
    fun markCloud(uid:String){prefs.edit().putBoolean("account:$uid:has_cloud",true).apply()}
    fun verifiedUsername(uid:String)=prefs.getString("account:$uid:verified_username","").orEmpty()
    fun rememberUsername(uid:String,name:String){prefs.edit().putString("account:$uid:verified_username",name).apply()}
    var receipts:JSONObject
        get()=runCatching {JSONObject(prefs.getString(key("receipts"),"{}")!!)}.getOrDefault(JSONObject())
        set(value){string("receipts",value.toString())}
    fun requestId(key:String):String {
        val name="request:$key"
        return prefs.getString(name,null) ?: java.util.UUID.randomUUID().toString().also {prefs.edit().putString(name,it).apply()}
    }
    var language:Language
        get()=runCatching {Language.valueOf(prefs.getString(key("language"),"RU")!!)}.getOrDefault(Language.RU)
        set(value){string("language",value.name,"settings")}
    var favorites:Set<String>
        get()=prefs.getStringSet(key("favorites"),emptySet())!!.toSet()
        set(value){set("favorites",value)}
    var drafts:Set<String>
        get()=prefs.getStringSet(key("drafts"),emptySet())!!.toSet()
        set(value){set("drafts",value)}
    var resume:Resume
        get()=runCatching {Resume.fromJson(JSONObject(prefs.getString(key("resume"),"{}")!!))}.getOrDefault(Resume())
        set(value){string("resume",value.toJson().toString())}
    var profile:UserProfile
        get()=runCatching {prefs.getString(key("profile"),null)?.let {UserProfile.fromJson(JSONObject(it))} ?: UserProfile(name=resume.name,phone=resume.phone,photo=resume.photo)}.getOrDefault(UserProfile())
        set(value){string("profile",value.toJson().toString())}
    fun accountProfile(uid:String):UserProfile {check(activeUid==uid);return profile}
    fun saveAccountProfile(uid:String,value:UserProfile){check(activeUid==uid);withoutTracking {profile=value}}
    var chat:List<ChatMessage>
        get()=runCatching {
            val a=JSONArray(prefs.getString(key("chat"),"[]")!!)
            (0 until a.length()).map {a.getJSONObject(it).let {o->ChatMessage(o.getString("role"),o.getString("content"))}}
        }.getOrDefault(emptyList())
        set(value){string("chat",JSONArray(value.takeLast(40).map {JSONObject().put("role",it.role).put("content",it.content.take(8000))}).toString())}
}
