package kz.youni4st.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.SubcomposeAsyncImage
import kz.youni4st.app.data.*

@Composable
fun EntityImage(entry: Entry, size: Dp = 60.dp) {
    val photo = entry.raw.optString(if (entry.kind == "mentors") "photo_url" else "logo_url")
    val icon = when (entry.kind) { "mentors" -> Icons.Outlined.PersonOutline; "jobs" -> Icons.Outlined.Business; else -> Icons.Outlined.School }
    val modifier = Modifier.size(size).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF0F3F4))
    val fallback: @Composable () -> Unit = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(size * .45f), tint = Navy) } }
    Box(modifier) {
        if (imageModel(photo) == null) fallback() else SubcomposeAsyncImage(imageModel(photo), entry.title(LocalLanguage.current),
            Modifier.fillMaxSize(), contentScale = if (entry.kind == "mentors") ContentScale.Crop else ContentScale.Fit,
            loading = { fallback() }, error = { fallback() })
    }
}

@Composable
fun CountOrb(count: Int, label: String, dark: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "$label: $count" }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).border(4.dp, if (dark) Color.White else Navy.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
            Text(count.toString(), color = if (dark) Color.White else Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, Modifier.padding(top = 8.dp), fontSize = 11.sp, color = if (dark) Color.White.copy(alpha = .75f) else Navy)
    }
}

@Composable
fun Accordion(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE7E7E1))) {
        Column(Modifier.animateContentSize()) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Teal, modifier = Modifier.size(23.dp))
                Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            if (expanded) Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun ExternalLinkButton(url: String, label: String, platform: String = "", note: Boolean = true) {
    val context = LocalContext.current
    val valid = safeWebUrl(url)
    var confirm by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button({ confirm = true }, Modifier.fillMaxWidth().heightIn(min = 54.dp), enabled = valid != null,
            shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
            Icon(Icons.Outlined.OpenInNew, null, Modifier.size(19.dp)); Spacer(Modifier.width(10.dp)); Text(label)
        }
        if (note) Text(if (valid == null) t("Ссылка появится после добавления профиля.", "Профиль сілтемесі кейін қосылады.", "A link will appear when the profile is added.")
            else t("Откроется ${platform.ifBlank { "внешняя площадка" }}. Переход — по твоему выбору; перед вводом данных проверь адрес сайта.", "Сыртқы платформа ашылады. Өтуді өзің шешесің; деректерді енгізбес бұрын мекенжайды тексер.", "Opens an external platform. Continuing is your choice; check the address before entering personal information."), color = Muted, style = MaterialTheme.typography.bodyMedium)
        if (failed) Text(t("Не удалось открыть ссылку", "Сілтеме ашылмады", "Could not open the link"), color = MaterialTheme.colorScheme.error)
    }
    if (confirm && valid != null) AlertDialog(onDismissRequest = { confirm = false }, icon = { Icon(Icons.Outlined.OpenInNew, null) },
        title = { Text(t("Переход на другую площадку", "Сыртқы платформаға өту", "Open external platform")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(Uri.parse(valid).host.orEmpty(), fontWeight = FontWeight.Bold); Text(valid, style = MaterialTheme.typography.bodyMedium); Text(t("Ты можешь остаться в Youni4st. Условия и обработку данных на внешнем сайте определяет его владелец.", "Youni4st-та қалуға болады. Сыртқы сайттың шарттарын оның иесі анықтайды.", "You can stay in Youni4st. The external website has its own terms and privacy practices.")) } },
        confirmButton = { TextButton({ confirm = false; failed = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(valid))) }.isFailure }) { Text(t("Перейти", "Өту", "Continue")) } },
        dismissButton = { TextButton({ confirm = false }) { Text(t("Остаться", "Қалу", "Stay")) } })
}

/** Four distinct, scalable editorial illustrations; assets can be replaced without remote downloads. */
@Composable
fun SectionArt(kind: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 180f, size.height / 160f, Offset.Zero) }) {
            val white = Color(0xFFFAFCF4); val blue = Color(0xFF294F69); val teal = Color(0xFF44A7AD)
            drawOval(Navy.copy(alpha = .08f), Offset(20f, 132f), Size(145f, 17f))
            drawCircle(white.copy(alpha = .65f), 56f, Offset(100f, 74f))
            when (kind) {
                0 -> {
                    rotate(-12f, Offset(94f,90f)) {
                        drawRoundRect(blue, Offset(42f,48f), Size(99f,82f), CornerRadius(9f))
                        drawRoundRect(white, Offset(47f,40f), Size(90f,82f), CornerRadius(7f))
                        drawRect(teal, Offset(47f,40f), Size(15f,82f))
                        for(i in 0..3) drawRoundRect(blue.copy(alpha = .3f), Offset(73f,65f+i*12),Size(44f-i*5,4f),CornerRadius(2f))
                    }
                    drawCircle(Lime,23f,Offset(139f,112f)); drawLine(Navy,Offset(130f,112f),Offset(137f,120f),3f,StrokeCap.Round); drawLine(Navy,Offset(137f,120f),Offset(151f,101f),3f,StrokeCap.Round)
                }
                1 -> {
                    drawPath(Path().apply { moveTo(22f,65f); lineTo(92f,25f); lineTo(163f,65f); close() }, blue)
                    drawRoundRect(white,Offset(34f,69f),Size(118f,61f),CornerRadius(3f))
                    for(i in 0..3) drawRoundRect(teal,Offset(45f+i*28,77f),Size(12f,45f),CornerRadius(3f))
                    drawRoundRect(blue,Offset(25f,130f),Size(136f,8f),CornerRadius(3f))
                    drawCircle(Lime,10f,Offset(93f,51f))
                }
                2 -> {
                    drawRoundRect(blue,Offset(70f,37f),Size(44f,32f),CornerRadius(9f),style=Stroke(7f))
                    drawRoundRect(blue,Offset(29f,61f),Size(126f,75f),CornerRadius(14f))
                    drawRoundRect(teal,Offset(29f,57f),Size(126f,39f),CornerRadius(13f))
                    drawRoundRect(Lime,Offset(85f,86f),Size(16f,20f),CornerRadius(4f))
                    drawCircle(white,18f,Offset(145f,47f)); drawLine(teal,Offset(145f,36f),Offset(145f,58f),3f); drawLine(teal,Offset(134f,47f),Offset(156f,47f),3f)
                }
                else -> {
                    drawRoundRect(blue,Offset(25f,31f),Size(94f,60f),CornerRadius(18f))
                    drawPath(Path().apply { moveTo(38f,83f);lineTo(38f,103f);lineTo(63f,86f);close() },blue)
                    drawRoundRect(teal,Offset(70f,86f),Size(88f,46f),CornerRadius(15f))
                    for(i in 0..2) drawCircle(Lime,5f,Offset(49f+i*22f,60f))
                    drawLine(white,Offset(88f,103f),Offset(140f,103f),4f,StrokeCap.Round)
                    drawLine(white,Offset(88f,116f),Offset(122f,116f),4f,StrokeCap.Round)
                }
            }
            drawCircle(teal.copy(alpha=.5f),4f,Offset(20f,38f)); drawCircle(blue.copy(alpha=.3f),3f,Offset(162f,94f))
        }
    }
}


@Composable
fun HeroArc(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        clipRect(left=size.width*.94f) {
            drawCircle(Color.White.copy(alpha=.65f),size.height*.85f,Offset(size.width+size.height*.77f,size.height*.22f),style=Stroke(2.dp.toPx()))
            drawCircle(Color.White.copy(alpha=.18f),size.height*.94f,Offset(size.width+size.height*.77f,size.height*.22f),style=Stroke(1.dp.toPx()))
        }
    }
}

@Composable
fun ProfileAvatar(photo:String,name:String,size:Dp) {
    if(photo.isBlank()) Box(Modifier.border(2.dp,Color.Black,CircleShape).padding(2.dp)) { InitialAvatar(name.take(1).ifBlank {"Y"},Teal,size-4.dp,true) }
    else coil.compose.AsyncImage(photo,t("Фото профиля","Профиль фотосы","Profile photo"),Modifier.size(size).clip(CircleShape).border(2.dp,Color.Black,CircleShape),contentScale=ContentScale.Crop)
}
