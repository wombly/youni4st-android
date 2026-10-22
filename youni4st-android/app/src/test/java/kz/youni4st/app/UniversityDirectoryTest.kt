package kz.youni4st.app

import java.io.File
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import kz.youni4st.app.data.*

class UniversityDirectoryTest {
    private val directory=UniversityDirectory(JSONObject(File("src/main/assets/university_search.json").readText()))
    private val data=JSONArray(File("src/main/assets/universities_db.json").readText())
    private val entries=(0 until data.length()).map {data.getJSONObject(it)}.filter {!it.optBoolean("archived")}.map {Entry(it.getString("id"),"universities",it)}
    @Test fun `all cities are unique and Almaty is separate from its neighbouring region`() {
        assertEquals(90,directory.cities.size)
        assertEquals(90,directory.cities.map {it.id}.toSet().size)
        val nearby=directory.nearbyCities("750000000")
        assertEquals("750000000",nearby.first().id)
        assertTrue(nearby.drop(1).all {it.parent=="19"})
        assertFalse(nearby.any {it.id=="710000000"})
        assertTrue(directory.nearbyCities("").isEmpty())
    }
    @Test fun `changing parent clears stale child filters`() {
        val query=UniversityQuery("750000000","195220100","medicine","old-program")
        assertEquals("",query.chooseCity("710000000").nearbyCity)
        assertEquals("",query.chooseField("engineering").specialty)
        assertTrue(directory.children("").isEmpty())
    }
    @Test fun `specialty results are actual offerings not all medical institutions`() {
        val neuro=directory.children("medicine").first {it.label(Language.RU).contains("нейрохирург",true)}
        val query=UniversityQuery(field="medicine",specialty=neuro.id)
        val matches=entries.filter {query.matches(it,"")}
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.all {neuro.id in it.universitySpecialties})
        assertFalse(query.matches(entries.first {it.raw.optInt("epvo_id")==522},""))
        assertTrue(neuro.fullLabel(Language.RU).contains("Резидентура"))
    }
    @Test fun `unknown thresholds remain visible but invalid scores never match`() {
        val entry=entries.first()
        assertTrue(UniversityQuery().matches(entry,"80"))
        assertFalse(UniversityQuery().matches(entry,"141"))
        assertFalse(UniversityQuery(city="nonexistent").matches(entry,""))
    }
    @Test fun `programs and facility claims have valid references and offline logos exist`() {
        assertEquals(159,entries.size)
        val ids=mutableSetOf<String>()
        val specialties=directory.specialties.associateBy {it.id}
        entries.forEach {e->
            assertTrue(ids.add(e.id))
            assertTrue(e.raw.getString("source_url").startsWith("https://epvo.kz/"))
            e.universitySpecialties.forEach {assertNotNull(specialties[it])}
            if(e.raw.optBoolean("has_dormitory")) assertNotNull(safeWebUrl(e.raw.getString("dormitory_source")))
            if(e.raw.optBoolean("has_military_dept")) assertNotNull(safeWebUrl(e.raw.getString("military_source")))
            val logo=e.raw.optString("logo_url")
            if(logo.isNotBlank()) assertTrue(File("src/main/assets/"+logo.removePrefix("file:///android_asset/")).isFile)
        }
    }
    @Test fun `legacy economics and architecture codes retain correct fields`() {
        val expected=mapOf("5B050600" to "economics", "5B042000" to "architecture", "6D042000" to "architecture")
        val found=mutableSetOf<String>()
        entries.forEach { e ->
            val programs=e.raw.getJSONArray("specialties")
            for(i in 0 until programs.length()) {
                val p=programs.getJSONObject(i)
                val code=p.optString("code")
                expected[code]?.let { field ->
                    assertEquals(field,p.getString("field"))
                    found.add(code)
                }
            }
        }
        assertEquals(expected.keys,found)
    }
    @Test fun `primary picker has exactly the requested cities and dependent city stays first`() {
        val names=listOf("Актау","Актобе","Алматы","Астана","Атырау","Жезказган","Караганда","Кокшетау","Костанай","Кызылорда","Павлодар","Петропавловск","Семей","Талдыкорган","Тараз","Туркестан","Уральск","Усть-Каменогорск","Шымкент")
        assertEquals(names,directory.primaryCities.map {it.label(Language.RU)})
        directory.primaryCities.forEach { city ->
            val nearby=directory.nearbyCities(city.id)
            assertEquals(city.id,nearby.first().id)
            assertEquals(nearby.size,nearby.map {it.id}.toSet().size)
            assertTrue(nearby.drop(1).all {it.parent==directory.regionFor(city.id)})
        }
        assertTrue(directory.nearbyCities("750000000").any {it.label(Language.RU)=="Каскелен"})
    }
    @Test fun `new public contacts are dialable and traceable to their sources`() {
        val reviewed=entries.filter {it.raw.getJSONObject("contacts").optString("phone_source").isNotBlank()}
        assertEquals(23,reviewed.size)
        reviewed.forEach { e ->
            val c=e.raw.getJSONObject("contacts")
            assertEquals(11,c.getString("phone").count {it.isDigit()})
            assertNotNull(safeWebUrl(c.getString("phone_source")))
        }
    }
    @Test fun `facility switches intersect with city and program filters`() {
        val dorm=entries.filter {UniversityQuery(dormitory=true).matches(it,"")}
        val military=entries.filter {UniversityQuery(military=true).matches(it,"")}
        val both=entries.filter {UniversityQuery(dormitory=true,military=true).matches(it,"")}
        assertTrue(dorm.isNotEmpty()); assertTrue(military.isNotEmpty()); assertTrue(both.isNotEmpty())
        assertEquals(dorm.toSet().intersect(military.toSet()),both.toSet())
        assertEquals(entries.size,entries.count {UniversityQuery().matches(it,"")})
        val city=both.first().raw.getString("city_key")
        assertEquals(both.filter {it.raw.optString("city_key")==city},entries.filter {UniversityQuery(city=city,dormitory=true,military=true).matches(it,"")})
        assertFalse(UniversityQuery(dormitory=true).matches(Entry("unknown","universities",JSONObject().put("city_key",city)),""))
    }
    @Test fun `geographic labels are translated without changing filter identifiers`() {
        val cyrillic=Regex("[А-Яа-яӘәҒғҚқҢңӨөҰұҮүҺһІі]")
        (directory.cities+directory.regions).forEach {
            assertFalse(it.label(Language.EN),cyrillic.containsMatchIn(it.label(Language.EN)))
            assertTrue(it.label(Language.KZ).isNotBlank())
        }
        assertEquals("Aktau",directory.primaryCities.first().label(Language.EN))
        assertEquals("Ақтау",directory.primaryCities.first().label(Language.KZ))
        assertEquals("Almaty Region",directory.regions.first {it.id=="19"}.label(Language.EN))
        entries.forEach {entry ->
            directory.cities.find {it.id==entry.raw.optString("city_key")}?.let {
                assertEquals(it.label(Language.EN),entry.text("city",Language.EN))
                assertEquals(it.label(Language.KZ),entry.text("city",Language.KZ))
            }
        }
    }
}
