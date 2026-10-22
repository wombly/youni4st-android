package kz.youni4st.app
import org.junit.Assert.*
import org.junit.Test
class StartupSettingsTest {
 @Test fun shortBackgroundDoesNotRestart() { assertFalse(StartupSettings.shouldReplay(0)); assertFalse(StartupSettings.shouldReplay(StartupSettings.RESTART_AFTER_BACKGROUND_MS-1)) }
 @Test fun timeoutBoundaryRestarts() { assertTrue(StartupSettings.shouldReplay(StartupSettings.RESTART_AFTER_BACKGROUND_MS)); assertTrue(StartupSettings.shouldReplay(StartupSettings.RESTART_AFTER_BACKGROUND_MS+1)) }
}
