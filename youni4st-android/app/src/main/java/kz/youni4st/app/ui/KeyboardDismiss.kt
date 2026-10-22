package kz.youni4st.app.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Unhandled background taps only; inputs and clickable controls consume their own gestures. */
fun Modifier.dismissKeyboardOnBackgroundTap():Modifier = composed {
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    pointerInput(focus,keyboard) {detectTapGestures(onTap={focus.clearFocus();keyboard?.hide()})}
}
