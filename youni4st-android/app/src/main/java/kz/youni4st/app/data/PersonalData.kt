package kz.youni4st.app.data

import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class ResumeRecord(
    val title:String="",val subtitle:String="",val period:String="",val details:String="",val ongoing:Boolean=false,
    val industry:String="",val specialization:String="",val startMonth:String="",val endMonth:String="",val student:Boolean=false
) {
    fun toJson()=JSONObject().put("title",title).put("subtitle",subtitle).put("period",period).put("details",details).put("ongoing",ongoing)
        .put("industry",industry).put("specialization",specialization).put("startMonth",startMonth).put("endMonth",endMonth).put("student",student)
    fun summary()=listOf(title,subtitle,period,details).filter {it.isNotBlank()}.joinToString("\n")
    companion object {fun fromJson(o:JSONObject)=ResumeRecord(o.optString("title"),o.optString("subtitle"),o.optString("period"),o.optString("details"),o.optBoolean("ongoing"),o.optString("industry"),o.optString("specialization"),o.optString("startMonth"),o.optString("endMonth"),o.optBoolean("student"))}
}
internal fun JSONObject.records(key:String):List<ResumeRecord> = optJSONArray(key)?.let { a -> (0 until a.length()).map { ResumeRecord.fromJson(a.getJSONObject(it)) } } ?: emptyList()
internal fun JSONObject.strings(key:String):List<String> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
data class UserProfile(val name:String="",val username:String="",val email:String="",val phone:String="",val score:String="",val interests:List<String> = emptyList(),val photo:String="") {
    fun toJson()=JSONObject().put("name",name).put("username",username).put("email",email).put("phone",phone).put("score",score).put("interests",JSONArray(interests)).put("photo",photo)
    companion object { fun fromJson(o:JSONObject)=UserProfile(o.optString("name"),o.optString("username"),o.optString("email"),o.optString("phone"),o.optString("score"),o.strings("interests"),o.optString("photo")) }
}
object PersonalValidation {
    val dateFormat:DateTimeFormatter=DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
    fun birthday(value:String,today:LocalDate=LocalDate.now()):Boolean = value.isBlank() || runCatching {
        val date=LocalDate.parse(value,dateFormat)
        date.year>=1900 && !date.isAfter(today)
    }.getOrDefault(false)
    fun phone(value:String):Boolean = value.isBlank() || value.filter(Char::isDigit).let { it.length==11 && it.startsWith("7") }
    fun formattedPhone(value:String):String {
        val d=phoneDigits(value)
        return if(d.isBlank()) "" else "+7("+d.take(3)+")"+d.drop(3).take(3)+"-"+d.drop(6).take(2)+"-"+d.drop(8).take(2)
    }
}
