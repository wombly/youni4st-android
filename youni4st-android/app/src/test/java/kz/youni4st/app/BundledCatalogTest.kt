package kz.youni4st.app

import java.io.File
import kz.youni4st.app.data.Entry
import kz.youni4st.app.data.Language
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class BundledCatalogTest {
    @Test fun `bundled content has readable titles in all three languages`() {
        val files = mapOf("universities" to "universities_db.json", "jobs" to "jobs_db.json",
            "mentors" to "mentors_screen.json", "programs" to "edu_programs_db.json",
            "grants" to "grants_db.json", "ielts" to "ielts_db.json", "unt" to "unt_db.json", "news" to "news_verified.json")
        files.forEach { (kind, file) ->
            val entries = JSONArray(File("src/main/assets/$file").readText())
            assertTrue("$file must contain entries", entries.length() > 0)
            for (i in 0 until entries.length()) {
                val entry = Entry("$kind:$i", kind, entries.getJSONObject(i))
                Language.entries.forEach { lang ->
                    assertTrue("Missing title: ${entry.id} $lang", entry.title(lang).isNotBlank())
                    assertTrue("Missing description: ${entry.id} $lang", entry.description(lang).isNotBlank())
                }
                if (kind == "universities" && !entry.raw.isNull("min_score")) assertTrue(entry.raw.getInt("min_score") in 0..140)
            }
        }
    }
    @Test fun `news has twenty unique dated primary sources and stable ids`() {
        val array = JSONArray(File("src/main/assets/news_verified.json").readText())
        assertEquals(20,array.length())
        val ids = mutableSetOf<String>()
        val sources = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val item=array.getJSONObject(i)
            assertTrue(ids.add(item.getString("id")))
            val url=item.getString("source_url")
            assertNotNull(kz.youni4st.app.data.safeWebUrl(url))
            assertTrue(sources.add(url))
            assertTrue(item.getString("published_at").matches(Regex("2026(-\\d{2}-\\d{2})?")))
            assertEquals("2026-10-14",item.getString("checked_at"))
        }
    }
}
