package kz.youni4st.app.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
data class DirectoryOption(val id:String, val name:JSONObject, val parent:String="", val level:JSONObject?=null) {
    fun label(language:Language):String = name.optString(language.name).ifBlank { name.optString("RU") }
    fun fullLabel(language:Language):String = label(language) + (level?.let { " · " + it.optString(language.name).ifBlank {it.optString("RU")} } ?: "")
}
class UniversityDirectory(json:JSONObject) {
    val cities=json.getJSONArray("cities").objects().map { DirectoryOption(it.getString("id"),it.getJSONObject("name"),it.getString("region")) }
    val primaryCities:List<DirectoryOption> get() = listOf("Актау","Актобе","Алматы","Астана","Атырау","Жезказган","Караганда","Кокшетау","Костанай","Кызылорда","Павлодар","Петропавловск","Семей","Талдыкорган","Тараз","Туркестан","Уральск","Усть-Каменогорск","Шымкент").mapNotNull { name -> cities.find { it.label(Language.RU)==name } }
    val regions=json.getJSONArray("regions").objects().map {DirectoryOption(it.getString("id"),it.getJSONObject("name"))}
    val fields=json.getJSONArray("fields").objects().map {DirectoryOption(it.getString("id"),it.getJSONObject("name"))}
    val specialties=json.getJSONArray("specialties").objects().map {DirectoryOption(it.getString("id"),it.getJSONObject("name"),it.getString("field"),it.getJSONObject("level"))}
    /** Republican cities are separate units; their neighbouring region is explicitly named by the UI. */
    fun regionFor(city:String):String = when(city.take(2)) {"75"->"19";"71"->"11";"79"->"61";else->cities.find {it.id==city}?.parent.orEmpty()}
    fun nearbyCities(city:String):List<DirectoryOption> {
        val selected=cities.find {it.id==city} ?: return emptyList()
        return listOf(selected)+cities.filter {it.parent==regionFor(city) && it.id!=city}
    }
    fun children(field:String)=specialties.filter {it.parent==field}
}

data class UniversityQuery(val city:String="",val nearbyCity:String="",val field:String="",val specialty:String="",val dormitory:Boolean=false,val military:Boolean=false) {
    fun chooseCity(value:String)=copy(city=value,nearbyCity="")
    fun chooseField(value:String)=copy(field=value,specialty="")
    fun matches(entry:Entry, score:String, search:String=""):Boolean {
        if(dormitory && !entry.raw.optBoolean("has_dormitory")) return false
        if(military && !entry.raw.optBoolean("has_military_dept")) return false
        if(!CatalogFilter.validScore(score)) return false
        val actualCity=nearbyCity.ifBlank {city}
        if(actualCity.isNotBlank() && entry.raw.optString("city_key")!=actualCity) return false
        if(field.isNotBlank() && field !in entry.universityFields) return false
        if(specialty.isNotBlank() && specialty !in entry.universitySpecialties) return false
        val minimum=entry.raw.opt("min_score") as? Number
        if(score.isNotBlank() && minimum!=null && minimum.toInt()>score.toInt()) return false
        return search.isBlank() || entry.universitySearchText.contains(search.trim(),ignoreCase=true)
    }
}
