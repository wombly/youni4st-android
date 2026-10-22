package kz.youni4st.app

import java.io.File
import kz.youni4st.app.data.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class UpdatedCatalogTest {
    private fun read(name:String)=JSONArray(File("src/main/assets/$name").readText())
    @Test fun `twenty courses have ten providers two courses each and bundled logos`() {
        val data=read("edu_programs_db.json")
        assertEquals(20,data.length())
        val counts=mutableMapOf<String,Int>()
        val ids=mutableSetOf<String>()
        for(i in 0 until data.length()) {
            val row=data.getJSONObject(i)
            assertTrue(ids.add(row.getString("id")))
            val company=row.getString("company_id")
            counts[company]=(counts[company] ?: 0)+1
            assertNotNull(safeWebUrl(row.getString("source_url")))
            assertTrue(File("src/main/assets/"+row.getString("logo_url")).isFile)
            assertEquals("2026-10-14",row.getString("checked_at"))
            assertEquals("",row.getString("kaspi_url"))
        }
        assertEquals(10,counts.size)
        assertTrue(counts.values.all {it==2})
    }
    @Test fun `archive identifiers do not collide with current catalog`() {
        val ids=mutableSetOf<String>()
        listOf("edu_programs_db.json","ielts_db.json","grants_db.json","catalog_archive.json").forEach {file ->
            val data=read(file)
            for(i in 0 until data.length())assertTrue(ids.add(data.getJSONObject(i).getString("id")))
        }
        val archived=read("catalog_archive.json")
        assertTrue(archived.length()>0)
        for(i in 0 until archived.length()) {
            val row=archived.getJSONObject(i)
            assertTrue(row.getBoolean("archived"))
            assertFalse(row.has("kaspi_url"))
            assertTrue(row.getJSONObject("RU").getString("fullText").contains("Архивная"))
        }
    }
}
