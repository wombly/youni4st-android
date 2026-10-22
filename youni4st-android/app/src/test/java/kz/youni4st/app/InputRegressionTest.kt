package kz.youni4st.app

import kz.youni4st.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class InputRegressionTest {
    @Test fun `only explicit Kaspi hosts are payment destinations`() {
        assertNotNull(safeKaspiUrl("https://pay.kaspi.kz/pay/test"))
        assertNull(safeKaspiUrl("https://kaspi.kz.example.com/pay"))
        assertNull(safeKaspiUrl("https://example.com/kaspi.kz"))
        assertNull(safeKaspiUrl("https://someone@kaspi.kz/pay"))
        assertNull(safeKaspiUrl("http://kaspi.kz/pay"))
        assertNull(safeKaspiUrl(""))
    }
    @Test fun `country code is not fed back during incremental editing`() {
        var stored=""
        val national="7012345678"
        national.forEachIndexed {index,c ->
            val fieldValue=phoneDigits(stored)+c
            stored="+7"+phoneDigits(fieldValue)
            assertEquals(national.take(index+1),phoneDigits(stored))
        }
        assertEquals("+77012345678",stored)
        while(phoneDigits(stored).isNotEmpty()) {
            val shorter=phoneDigits(stored).dropLast(1)
            stored=if(shorter.isEmpty()) "" else "+7"+phoneDigits(shorter)
            assertEquals(shorter,phoneDigits(stored))
        }
        assertEquals("",stored)
    }
    @Test fun `international paste and partial prefix preserve national digits`() {
        assertEquals("7012345678",phoneDigits("+7 (701) 234-56-78"))
        assertEquals("7012345678",phoneDigits("8 (701) 234-56-78"))
        assertEquals("7",phoneDigits("+77"))
        assertEquals("",phoneDigits("+7"))
        assertEquals("777",phoneDigits("777"))
    }
    @Test fun `birthday numeric input inserts separators and validates real dates`() {
        assertEquals("25.08.2005",BirthdayInput.format("25082005"))
        assertEquals("25.08.2005",BirthdayInput.format("25.08.2005"))
        assertEquals("25.0",BirthdayInput.format("250"))
        assertEquals("25",BirthdayInput.format("25"))
        assertEquals("25.08.2005",BirthdayInput.format("25082005123"))
        val today=LocalDate.of(2026,10,14)
        assertTrue(PersonalValidation.birthday(BirthdayInput.format("29022004"),today))
        assertFalse(PersonalValidation.birthday(BirthdayInput.format("29022005"),today))
        assertFalse(PersonalValidation.birthday(BirthdayInput.format("15102026"),today))
    }
}
