package kz.youni4st.app.data

import android.content.Context
import org.json.JSONObject

class ResumeReference(context:Context) {
    val raw=JSONObject(context.assets.open("resume_options.json").bufferedReader().use {it.readText()})
    val industries=raw.getJSONArray("industries").let {a->(0 until a.length()).map {a.getJSONObject(it)}}
    fun professions(lang:Language)=raw.getJSONArray("professions").let {a->(0 until a.length()).map {i->a.getJSONObject(i).optString(lang.name)}}
    fun industry(value:String,lang:Language)=industries.firstOrNull {it.optString("id")==value}?.localized("name",lang) ?: value
    fun children(id:String,lang:Language)=industries.firstOrNull {it.optString("id")==id}?.getJSONArray("children")?.let {a->(0 until a.length()).map {a.getJSONObject(it).optString(lang.name)}} ?: emptyList()
    companion object {
        fun level(code:String,lang:Language):String {
            val key=if(code in listOf("native","Родной","Ана тілі","Native"))"native" else code.substringBefore(" ").uppercase()
            val text=when(key) {
                "native"->listOf("Родной","Ана тілі","Native")
                "A1"->listOf("Начальный","Бастапқы","Beginner")
                "A2"->listOf("Элементарный","Қарапайым","Elementary")
                "B1"->listOf("Средний","Орта","Intermediate")
                "B2"->listOf("Выше среднего","Ортадан жоғары","Upper-intermediate")
                "C1"->listOf("Продвинутый","Жоғары","Advanced")
                "C2"->listOf("В совершенстве","Еркін меңгеру","Proficient")
                else->return code
            }[lang.ordinal]
            return if(key=="native")text else "$key — $text"
        }
        fun employment(keys:List<String>,lang:Language)=keys.mapNotNull {key->when(key){
            "full_time"->listOf("Полная занятость","Толық жұмыспен қамту","Full-time")[lang.ordinal]
            "part_time"->listOf("Частичная занятость","Ішінара жұмыспен қамту","Part-time")[lang.ordinal]
            "remote"->listOf("Удалённая работа","Қашықтан жұмыс","Remote")[lang.ordinal]
            else->null
        }}.joinToString(" · ")
    }
}
