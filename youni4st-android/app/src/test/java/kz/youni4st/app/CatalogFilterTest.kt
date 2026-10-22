package kz.youni4st.app

import kz.youni4st.app.data.CatalogFilter
import kz.youni4st.app.data.Resume
import kz.youni4st.app.data.Entry
import kz.youni4st.app.data.Language
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CatalogFilterTest {
    @Test fun `score boundaries include zero and 140 and reject invalid input`() {
        listOf("", "0", "140").forEach { assertTrue(CatalogFilter.validScore(it)) }
        listOf("-1", "141", "abc", "99999999999999999").forEach { assertFalse(CatalogFilter.validScore(it)) }
    }
    @Test fun `score city and subject filters intersect`() {
        assertTrue(CatalogFilter.matches("95", 95, "astana", "astana", "it", "it"))
        assertFalse(CatalogFilter.matches("94", 95, "astana", "astana", "it", "it"))
        assertFalse(CatalogFilter.matches("140", 95, "astana", "almaty", "it", "it"))
        assertFalse(CatalogFilter.matches("140", 95, "astana", "astana", "it", "medicine"))
        assertTrue(CatalogFilter.matches("", 95, "astana", "", "it", ""))
    }
    @Test fun `resume round trip keeps unicode and multiline content`() {
        val resume = Resume(name = "Әлия Омарова", phone = "+7 701 123 45 67", city = "Алматы", experience = "Стажёр\nПервый проект", skills = "Kotlin, дизайн")
        assertEquals(resume, Resume.fromJson(JSONObject(resume.toJson().toString())))
        assertEquals(62, resume.completion)
    }
    @Test fun `catalog uses requested language and Russian fallback`() {
        val entry = Entry("jobs:0", "jobs", JSONObject("""{"title":{"RU":"Работа","KZ":"Жұмыс"},"company":"Test"}"""))
        assertEquals("Жұмыс", entry.title(Language.KZ))
        assertEquals("Работа", entry.title(Language.EN))
        assertEquals("Test", entry.subtitle(Language.KZ))
    }
}
