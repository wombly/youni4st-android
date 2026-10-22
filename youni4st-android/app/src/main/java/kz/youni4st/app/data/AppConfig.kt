package kz.youni4st.app.data

import android.content.Context
import org.json.JSONObject
import java.net.URI

class AppConfig(context: Context) {
    val raw = JSONObject(context.assets.open("app_config.json").bufferedReader().use { it.readText() })
    fun value(key: String) = raw.optString(key).trim()
    fun group(group: String, key: String) = raw.optJSONObject(group)?.optString(key)?.trim().orEmpty()
}

/** Only explicit HTTPS web destinations; no javascript, credentials or local file access. */
fun safeWebUrl(value: String): String? = runCatching {
    val uri = URI(value.trim())
    if (uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null) uri.toASCIIString() else null
}.getOrNull()

fun imageModel(value: String): String? = when {
    value.startsWith("images/") && !value.contains("..") && !value.contains('\\') -> "file:///android_asset/$value"
    else -> safeWebUrl(value)
}

fun phoneDigits(input: String): String {
    val digits = input.filter(Char::isDigit)
    return (if (input.trimStart().startsWith("+7") || (digits.length > 10 && (digits.startsWith("7") || digits.startsWith("8")))) digits.drop(1) else digits).take(10)
}

/** Merchant links must be explicitly supplied; a course URL is never a payment URL. */
fun safeKaspiUrl(value:String):String? = safeWebUrl(value)?.takeIf {
    val host=URI(it).host.lowercase()
    host=="kaspi.kz" || host.endsWith(".kaspi.kz")
}

/** Reject an extra keystroke rather than treating the first national digit as a country code. */
fun editPhoneDigits(previous: String, proposed: String): String {
    val digits=proposed.filter { it in '0'..'9' }
    if (digits.length<=10 && !proposed.trimStart().startsWith("+7")) return digits
    val explicitCountry=proposed.trimStart().startsWith("+7")
    val pastedWholeNumber=previous.isEmpty() && digits.length==11 && digits.first() in "78"
    return if(explicitCountry || pastedWholeNumber) phoneDigits(proposed) else previous
}
