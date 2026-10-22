package kz.youni4st.app

import kz.youni4st.app.data.*
import org.junit.Assert.*
import org.junit.Test

class SubmissionAndConfigTest {
    @Test fun `local success or a draft never counts as delivered`() {
        listOf("{}", "{\"success\":true}", "{\"status\":\"draft\",\"application_id\":\"a1\"}",
            "{\"status\":\"sent\"}", "{\"status\":\"sent\",\"application_id\":null}",
            "{\"status\":\"sent\",\"application_id\":7}", "not json").forEach {
            assertTrue(it, runCatching { deliveryReceipt(it) }.isFailure)
        }
        assertEquals("a1", deliveryReceipt("{\"status\":\"received\",\"application_id\":\"a1\"}"))
        assertEquals("a2", deliveryReceipt("{\"status\":\"sent\",\"application_id\":\"a2\"}"))
    }
    @Test fun `links cannot open scripts files or misleading credentials`() {
        listOf("", "javascript:alert(1)", "file:///private", "http://example.com", "https://good.com@evil.com", "https://").forEach {
            assertNull(it, safeWebUrl(it))
        }
        assertEquals("https://example.com/profile?a=1", safeWebUrl(" https://example.com/profile?a=1 "))
        assertEquals("file:///android_asset/images/logo.png", imageModel("images/logo.png"))
        assertNull(imageModel("images/../app_config.json"))
    }
    @Test fun `phone paste supports national and international formats`() {
        assertEquals("7011234567", phoneDigits("+7 (701) 123-45-67"))
        assertEquals("7011234567", phoneDigits("8 701 123 45 67"))
        assertEquals("7011234567", phoneDigits("7011234567"))
        assertEquals("701", phoneDigits("701"))
    }
}
