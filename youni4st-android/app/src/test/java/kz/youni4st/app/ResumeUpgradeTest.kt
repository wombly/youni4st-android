package kz.youni4st.app

import kz.youni4st.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ResumeUpgradeTest {
    @Test fun salaryFormattingAndCursorPositions() {
        assertEquals("100.000",SalaryInput.format("100000"))
        assertEquals("1.000.000",SalaryInput.format("1 000 000 ₸"))
        for(n in 0..12) {
            val raw="123456789012".take(n)
            for(i in 0..n) {
                val pos=SalaryInput.cursorToFormatted(i,n)
                assertTrue(pos in 0..SalaryInput.format(raw).length)
                assertEquals(i,SalaryInput.cursorToOriginal(pos,raw))
            }
        }
    }
    @Test fun parallelJobsAreNotCountedTwice() {
        val rows=listOf(ResumeRecord(startMonth="2022-01",endMonth="2022-12"),ResumeRecord(startMonth="2022-06",endMonth="2023-02"))
        assertEquals(14L,ResumeDates.experienceMonths(rows,YearMonth.of(2026,10)))
        assertEquals(1L,ResumeDates.experienceMonths(listOf(ResumeRecord(startMonth="2022-03",endMonth="2022-03"))))
        assertEquals(3L,ResumeDates.experienceMonths(listOf(ResumeRecord(startMonth="2026-08",ongoing=true)),YearMonth.of(2026,10)))
    }
    @Test fun invalidAndFutureIntervalsAreExcluded() {
        val today=YearMonth.of(2026,10)
        assertFalse(ResumeDates.validRange(ResumeRecord(startMonth="2026-11",ongoing=true),today))
        assertFalse(ResumeDates.validRange(ResumeRecord(startMonth="2024-12",endMonth="2024-01"),today))
        assertFalse(ResumeDates.validRange(ResumeRecord(period="2020 - 2024"),today))
        assertEquals(0L,ResumeDates.experienceMonths(listOf(ResumeRecord(period="old text")),today))
    }
    @Test fun ageUsesTheBirthdayRatherThanYearSubtraction() {
        assertEquals(25,ResumeDates.age("12.10.2000",LocalDate.of(2026,10,11)))
        assertEquals(26,ResumeDates.age("12.10.2000",LocalDate.of(2026,10,12)))
        assertNull(ResumeDates.age("31.02.2000"))
        assertEquals("12 октября",ResumeDates.birthday("12.10.2000",Language.RU))
    }
    @Test fun oldResumeAndNewFieldsRoundTripWithoutLosingContent() {
        val old=Resume.fromJson(JSONObject().put("skills","Kotlin; Excel\nFigma").put("work",org.json.JSONArray().put(JSONObject().put("title","Директор").put("period","2020-2024"))))
        assertEquals(listOf("Kotlin","Excel","Figma"),old.allSkills)
        assertEquals("2020-2024",old.work.first().period)
        val updated=old.copy(skillItems=listOf("Командная работа","SQL"),employmentTypes=listOf("full_time","remote"),work=listOf(ResumeRecord(title="Developer",industry="industry_1",specialization="Web",startMonth="2020-01",endMonth="2022-03",student=true)))
        assertEquals(updated,Resume.fromJson(updated.toJson()))
    }
    @Test fun feedbackIsBriefAndFadesBeforeIntroFinishes() {
        assertEquals(280L,StartupHaptics.timings.sum())
        assertEquals(280L,StartupHaptics.fallbackTimings.sum())
        assertEquals(StartupHaptics.timings.size,StartupHaptics.amplitudes.size)
        assertTrue(StartupHaptics.amplitudes.drop(1).zipWithNext().all {(a,b)->a>=b})
        assertTrue(StartupSettings.INTRO_DURATION_MS>=StartupHaptics.timings.sum())
    }
}
