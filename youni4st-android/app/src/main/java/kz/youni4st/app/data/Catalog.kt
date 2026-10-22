package kz.youni4st.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Language { RU, KZ, EN }

data class Entry(val id: String, val kind: String, val raw: JSONObject) {
    val universityFields: Set<String> by lazy { raw.optJSONArray("field_keys")?.let { a -> (0 until a.length()).map {a.getString(it)}.toSet() } ?: emptySet() }
    val universitySpecialties: Set<String> by lazy { raw.optJSONArray("specialties")?.let { a -> (0 until a.length()).map {a.getJSONObject(it).optString("id")}.toSet() } ?: emptySet() }
    val universitySearchText: String by lazy { listOf(raw.opt("name"),raw.opt("legal_name"),raw.opt("city")).joinToString(" ") }
    fun text(key: String, language: Language): String = raw.localized(key, language)
    fun title(language: Language): String = when (kind) {
        "universities", "mentors" -> text("name", language)
        "jobs" -> text("title", language)
        "ielts" -> raw.optString("${language.name}_title")
        else -> raw.optJSONObject(language.name)?.optString("title").orEmpty()
    }
    fun subtitle(language: Language): String = when (kind) {
        "universities" -> text("city", language)
        "mentors" -> text("role", language)
        "jobs" -> text("company", language)
        "ielts" -> text("subtitle", language)
        else -> raw.optJSONObject(language.name)?.let {
            it.optString("shortDesc").ifBlank { it.optString("subtitle").ifBlank { it.optString("provider") } }
        }.orEmpty()
    }
    fun description(language: Language): String = when (kind) {
        "universities", "jobs", "mentors" -> text("description", language)
        "ielts" -> raw.optJSONObject(language.name)?.optString("fullText").orEmpty().ifBlank { readable(raw, language) }
        else -> raw.optJSONObject(language.name)?.optString("fullText").orEmpty()
    }
}

fun JSONObject.localized(key: String, language: Language): String = when (val value = opt(key)) {
    is JSONObject -> value.optString(language.name).ifBlank { value.optString("RU") }
    is String -> value
    else -> ""
}

/** Preserve the original catalog's nested IELTS sections and selected language. */
fun readable(value: Any?, language: Language): String = when (value) {
    is JSONObject -> {
        if (value.has(language.name)) readable(value.opt(language.name), language)
        else value.keys().asSequence().filter { key ->
            key !in setOf("id", "category", "updatedAt", "logoUrl", "url", "RU_title", "KZ_title", "EN_title") &&
                !key.startsWith("RU_") && !key.startsWith("KZ_") && !key.startsWith("EN_")
        }.map { readable(value.opt(it), language) }.filter { it.isNotBlank() }.joinToString("\n\n")
    }
    is JSONArray -> (0 until value.length()).map { readable(value.opt(it), language) }.joinToString("\n\n")
    is String -> value.replace("\\n", "\n")
    is Number -> value.toString()
    else -> ""
}

class Catalog(context: Context) {
    val universities = UniversityDirectory(JSONObject(context.assets.open("university_search.json").bufferedReader().use {it.readText()}))
    val entries: List<Entry> = buildList {
        mapOf("universities" to "universities_db.json", "jobs" to "jobs_db.json",
            "mentors" to "mentors_screen.json", "programs" to "edu_programs_db.json",
            "grants" to "grants_db.json", "ielts" to "ielts_db.json", "unt" to "unt_db.json", "news" to "news_verified.json", "archive" to "catalog_archive.json"
        ).forEach { (kind, file) ->
            val array = JSONArray(context.assets.open(file).bufferedReader().use { it.readText() })
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(Entry(item.optString("id").ifBlank { "$kind:$index" }, if(kind=="archive") item.getString("archive_kind") else kind, item))
            }
        }
    }
    fun of(kind: String) = entries.filter { it.kind == kind && !it.raw.optBoolean("archived") }
}

object CatalogFilter {
    fun validScore(input: String) = input.isBlank() || input.toIntOrNull()?.let { it in 0..140 } == true
    fun matches(score: String, minimum: Int, city: String, chosenCity: String, field: String, chosenField: String): Boolean {
        if (!validScore(score)) return false
        return (score.isBlank() || minimum <= score.toInt()) &&
            (chosenCity.isBlank() || city == chosenCity) && (chosenField.isBlank() || field == chosenField)
    }
}
