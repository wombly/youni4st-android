package kz.youni4st.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kz.youni4st.app.data.Language

val Navy = Color(0xFF101D40)
val Ink = Color(0xFF252B36)
val Muted = Color(0xFF68717C)
val CanvasColor = Color(0xFFF6F5F0)
val Lime = Color(0xFFDFFA83)
val Dock = Color(0xFF080F0C)
val Gold = Color(0xFFFFD338)
val Teal = Color(0xFF0795AD)
val LocalLanguage = staticCompositionLocalOf { Language.RU }

@Composable
fun t(ru: String, kz: String, en: String): String = when (LocalLanguage.current) {
    Language.RU -> ru; Language.KZ -> kz; Language.EN -> en
}

@Composable
fun YouniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Navy, onPrimary = Color.White,
            secondary = Teal, tertiary = Lime, background = CanvasColor,
            surface = Color.White, onSurface = Ink, onBackground = Ink,
            surfaceVariant = CanvasColor, onSurfaceVariant = Muted, outline = Color(0xFFE8E9EF)),
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        ), content = content
    )
}
