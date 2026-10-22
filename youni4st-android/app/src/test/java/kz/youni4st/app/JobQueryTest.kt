package kz.youni4st.app

import java.io.File
import java.time.LocalDate
import kz.youni4st.app.data.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class JobQueryTest {
    private val today=LocalDate.of(2026,10,17)
    private val jobs=JSONArray(File("src/main/assets/jobs_db.json").readText()).let { a -> (0 until a.length()).map { val j=a.getJSONObject(it);Entry(j.getString("id"),"jobs",j) } }
    @Test fun `twenty current sourced openings have localized details and valid tags`() {
        assertEquals(20,jobs.size);assertEquals(20,jobs.map{it.id}.toSet().size)
        val cities=JSONObject(File("src/main/assets/university_search.json").readText()).getJSONArray("cities")
        val keys=(0 until cities.length()).map{cities.getJSONObject(it).getString("id")}.toSet()
        jobs.forEach { e ->
            assertTrue(jobIsActive(e,today));assertNotNull(enbekApplicationUrl(e));assertTrue(e.raw.getString("city_key") in keys)
            Language.values().forEach { l -> listOf("title","description","requirements","company","salary","category","type").forEach {assertTrue("${e.id} $it $l",e.text(it,l).isNotBlank())} }
            val tags=e.raw.getJSONArray("employment_keys");assertTrue(tags.length()>0)
            (0 until tags.length()).forEach {assertTrue(tags.getString(it) in setOf("remote","full_time","part_time","internship"))}
        }
    }
    @Test fun `employment is OR while company city and category are AND`() {
        val remote=jobs.filter{JobQuery(employment=setOf("remote")).matches(it,today)}
        val part=jobs.filter{JobQuery(employment=setOf("part_time")).matches(it,today)}
        assertEquals((remote+part).map{it.id}.toSet(),jobs.filter{JobQuery(employment=setOf("remote","part_time")).matches(it,today)}.map{it.id}.toSet())
        val plain=Entry("plain","jobs",JSONObject().put("company",JSONObject().put("RU","Acme").put("EN","Acme").put("KZ","Acme")))
        assertFalse(JobQuery(company="EN").matches(plain,today))
        val e=jobs.first();val q=JobQuery(company="  "+e.text("company",Language.EN).lowercase()+"  ",city=e.raw.getString("city_key"),category=e.raw.getString("category_key"))
        assertTrue(q.matches(e,today));assertFalse(q.copy(city="impossible").matches(e,today));assertFalse(q.copy(category="impossible").matches(e,today))
    }
    @Test fun `child selection resets and region matching works`() {
        val e=jobs.first();val region=e.raw.getString("region_key")
        assertTrue(JobQuery(city="ignored",nearbyCity="region:$region").matches(e,today))
        assertEquals("",JobQuery(nearbyCity="old").chooseCity("new").nearbyCity)
    }
    @Test fun `expiry inclusive archived and invalid dates excluded`() {
        val raw=JSONObject(jobs.first().raw.toString()).put("valid_through","2026-10-17");val e=Entry("test","jobs",raw)
        assertTrue(jobIsActive(e,today));assertFalse(jobIsActive(e,today.plusDays(1)))
        raw.put("valid_through","invalid");assertFalse(jobIsActive(e,today))
        raw.put("valid_through","2027-01-01").put("archived",true);assertFalse(jobIsActive(e,today))
    }
    @Test fun `online sales is not remote and senior developer is not internship`() {
        assertFalse(JobQuery(employment=setOf("remote")).matches(jobs.first{it.id=="enbek:6145077"},today))
        assertFalse(JobQuery(employment=setOf("internship")).matches(jobs.first{it.id=="enbek:6143683"},today))
        assertEquals(2,jobs.count{JobQuery(employment=setOf("remote")).matches(it,today)})
    }
    @Test fun `external destination restricted to real Enbek vacancy paths`() {
        listOf("https://enbek.kz.evil.test/ru/vacancy/x~123","https://example.org/ru/vacancy/x~123","https://enbek.kz/ru/login","javascript:alert(1)").forEach { url ->
            assertNull(enbekApplicationUrl(Entry("x","jobs",JSONObject().put("source","Enbek").put("source_url",url))))
        }
    }
}
