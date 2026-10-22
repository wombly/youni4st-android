package kz.youni4st.app

import kz.youni4st.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PersonalDataTest {
    @Test fun `older resume keeps all data and gets empty new sections`() {
        val old=JSONObject("""{"name":"Руслан","experience":"Стажёр\nПроект","education":"Университет","skills":"Kotlin","phone":"87011234567","photo":"file:///private/photo"}""")
        val result=Resume.fromJson(old)
        assertEquals("Стажёр\nПроект",result.experience)
        assertEquals("Университет",result.education)
        assertEquals("file:///private/photo",result.photo)
        assertTrue(result.work.isEmpty())
        assertEquals(result,Resume.fromJson(result.toJson()))
    }
    @Test fun `multiple records survive process restart and independent editing`() {
        val original=Resume(work=listOf(ResumeRecord("Разработчик","A","2020–2022","Kotlin"),ResumeRecord("Дизайнер","B","2023",ongoing=true)),
            studies=listOf(ResumeRecord("UIB","Management","2024","Bachelor"),ResumeRecord("AITU",ongoing=true)),
            languages=listOf(ResumeRecord("English","B2"),ResumeRecord("Қазақша","C2")),interests=listOf("Футбол","Дизайн"),birthday="29.02.2004",about="О себе")
        val restored=Resume.fromJson(JSONObject(original.toJson().toString()))
        assertEquals(original,restored)
        val edited=restored.copy(work=restored.work.toMutableList().apply {this[1]=this[1].copy(title="Lead")})
        assertEquals("Разработчик",edited.work[0].title)
        assertEquals("Lead",edited.work[1].title)
        assertEquals(original.studies,edited.studies)
        assertEquals(1,edited.copy(work=edited.work.drop(1)).work.size)
    }
    @Test fun `profile is serialized separately from resume`() {
        val resume=Resume(name="Имя в резюме",phone="+77011234567")
        val profile=UserProfile(name="Руслан",username="ruslan",email="r@example.com",phone="+77011234567",score="125",interests=listOf("IT"),photo="file:///photo")
        assertEquals(profile,UserProfile.fromJson(JSONObject(profile.toJson().toString())))
        assertEquals("Имя в резюме",resume.name)
    }
    @Test fun `birthday validates leap years range and future dates`() {
        val today=LocalDate.of(2026,10,14)
        listOf("","29.02.2004","14.10.2026","01.01.1900").forEach {assertTrue(it,PersonalValidation.birthday(it,today))}
        listOf("29.02.2003","31.04.2000","00.00.0000","15.10.2026","01.01.1899","1.1.2000").forEach {assertFalse(it,PersonalValidation.birthday(it,today))}
    }
    @Test fun `phone accepts complete plus seven number and formats pasted national number`() {
        assertTrue(PersonalValidation.phone("+77011234567"))
        assertFalse(PersonalValidation.phone("+7701123"))
        assertFalse(PersonalValidation.phone("+17011234567"))
        assertEquals("+7(701)123-45-67",PersonalValidation.formattedPhone("87011234567"))
    }
}
