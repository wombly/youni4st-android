package kz.youni4st.app.data

import java.time.LocalDate
import java.net.URI

/** Employment selections use OR; independent filters use AND. */
data class JobQuery(val company:String="",val city:String="",val nearbyCity:String="",val category:String="",val employment:Set<String> = emptySet()) {
    fun chooseCity(value:String)=copy(city=value,nearbyCity="")
    fun matches(entry:Entry,today:LocalDate=LocalDate.now()):Boolean {
        if(!jobIsActive(entry,today)) return false
        val chosen=nearbyCity.ifBlank {city}
        if(chosen.startsWith("region:")) {
            if(entry.raw.optString("region_key")!=chosen.removePrefix("region:")) return false
        } else if(chosen.isNotBlank() && entry.raw.optString("city_key")!=chosen) return false
        if(category.isNotBlank() && entry.raw.optString("category_key")!=category) return false
        if(company.isNotBlank() && Language.entries.none { entry.text("company",it).contains(company.trim(),ignoreCase=true) }) return false
        val tags=entry.raw.optJSONArray("employment_keys")
        return employment.isEmpty() || (0 until (tags?.length() ?: 0)).any {tags!!.optString(it) in employment}
    }
}
fun jobIsActive(entry:Entry,today:LocalDate=LocalDate.now()):Boolean {
    if(entry.raw.optBoolean("archived")) return false
    val date=entry.raw.optString("valid_through")
    return date.isBlank() || runCatching { !LocalDate.parse(date).isBefore(today) }.getOrDefault(false)
}
fun enbekApplicationUrl(entry:Entry):String? {
    if(entry.kind!="jobs" || entry.raw.optString("source")!="Enbek") return null
    return safeWebUrl(entry.raw.optString("source_url"))?.takeIf {
        val uri=URI(it)
        uri.host.lowercase() in setOf("enbek.kz","www.enbek.kz") && uri.path.startsWith("/ru/vacancy/") && uri.path.substringAfterLast('~').toLongOrNull()!=null
    }
}
