package kz.youni4st.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kz.youni4st.app.R

@Composable
fun BrandHeader(vm: kz.youni4st.app.AppViewModel, navigate: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Color.White)
        .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Image(painterResource(R.drawable.brand_logo), "Youni4st", Modifier.size(178.dp, 36.dp).clip(RoundedCornerShape(3.dp)), contentScale = ContentScale.Crop)
        Box {
            IconButton({menu=true},Modifier.size(52.dp)) { Icon(Icons.Outlined.Language, t("Язык", "Тіл", "Language"), tint = Ink, modifier = Modifier.size(29.dp)) }
            DropdownMenu(menu,{menu=false},modifier=Modifier.width(248.dp),containerColor=Color(0xFFF7F1FB),shape=RoundedCornerShape(18.dp)) {
                listOf(kz.youni4st.app.data.Language.RU to "RU · Русский",kz.youni4st.app.data.Language.KZ to "KZ · Қазақша",kz.youni4st.app.data.Language.EN to "EN · English").forEach { (language,label) ->
                    DropdownMenuItem(text={Text(label)},onClick={vm.changeLanguage(language);menu=false},trailingIcon={if(vm.language==language)Icon(Icons.Outlined.Check,null,tint=Teal)})
                }
                HorizontalDivider()
                DropdownMenuItem(text={Text(t("Поддержка","Қолдау","Support"))},leadingIcon={Icon(Icons.Outlined.HelpOutline,null)},onClick={menu=false;navigate("support")})
                DropdownMenuItem(text={Text(t("О приложении","Қолданба туралы","About"))},leadingIcon={Icon(Icons.Outlined.Info,null)},onClick={menu=false;navigate("about")})
            }
        }
    }
}

@Composable
fun PageTitle(title: String, onBack: () -> Unit, action: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад", "Артқа", "Back")) }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        action()
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color.Black)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = enabled,
        shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp)) {
        Text(text)
    }
}

@Composable
fun SelectField(value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(17.dp), color = Color.White, border=BorderStroke(1.dp,Color.Black)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == value }?.second ?: options.firstOrNull()?.second.orEmpty(),
                    Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Outlined.ExpandMore, null, Modifier.padding(start = 6.dp).size(19.dp))
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).widthIn(max = 330.dp)) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelected(key); expanded = false },
                    trailingIcon = { if (key == value) Icon(Icons.Outlined.Check, null, tint = Teal) })
            }
        }
    }
}

@Composable
fun FavoriteButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick, modifier) {
        Icon(if (selected) Icons.Rounded.Star else Icons.Outlined.StarOutline,
            if (selected) t("Убрать из избранного", "Таңдаулылардан жою", "Remove favorite") else t("В избранное", "Таңдаулыға қосу", "Save favorite"),
            tint = if (selected) Color(0xFFAC9500) else Muted)
    }
}

@Composable
fun Badge(text: String, color: Color = Muted, background: Color = CanvasColor, icon: ImageVector? = null) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(background).border(1.dp,Color.Black,RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(13.dp), tint = color)
        Text(text, fontSize = 11.sp, color = color)
    }
}

@Composable
fun InitialAvatar(text: String, color: Color = Navy, size: Dp = 52.dp, round: Boolean = false) {
    Box(Modifier.size(size).clip(if (round) CircleShape else RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(text, fontSize = if (size > 55.dp) 28.sp else 21.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(36.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
        if (action != null) OutlinedButton(onAction) { Text(action) }
    }
}

@Composable
fun MenuRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(CanvasColor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Navy) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted)
        }
    }
}

/** Local vector artwork: no dependency on the Mini App's unavailable image CDN. */
@Composable
fun CampusIllustration(variant: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 400f; val sy = size.height / 180f
        withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
            drawRect(Color(if (variant == 1) 0xFFD6E5E5 else 0xFFE8E2D5), size = Size(400f,180f))
            val blues = listOf(Color(0xFFB0C9CF), Color(0xFF88AEB8), Color(0xFFD4DFDA))
            for (i in 0..5) {
                val h = 92f + (i % 3) * 20
                drawRect(blues[i % 3], Offset(i * 73f - 5, 0f), Size(65f, h))
                for (j in 0..2) for(k in 0..3) {
                    drawRect(Color.White.copy(alpha = .5f), Offset(i * 73f + 3 + j * 18, 10f + k * 24), Size(12f, 17f))
                }
                drawLine(Color.White.copy(alpha = .35f), Offset(i*73f,0f), Offset(i*73f+60,100f), 2f)
            }
            drawRect(Color(0xFFC8D3C3), Offset(0f,123f), Size(400f,57f))
            drawPath(Path().apply { moveTo(195f,75f); lineTo(273f,35f); lineTo(351f,75f); close() }, Color(0xFFEDEBDE))
            drawRect(Color(0xFFEDEBDE), Offset(208f,75f), Size(130f,68f))
            for(i in 0..3) drawRect(Color(0xFF7594A0), Offset(220f+i*29,79f), Size(12f,51f))
            drawRect(Color(0xFFEBB48F), Offset(202f,139f), Size(143f,6f))
            // Students and mentors, positioned around the foreground text plaque.
            fun person(x: Float, y: Float, shirt: Color, skin: Color, hair: Color, scale: Float = 1f) {
                withTransform({ translate(x,y); scale(scale,scale,Offset.Zero) }) {
                    drawRoundRect(shirt, Offset(-18f,15f), Size(36f,52f), CornerRadius(15f,15f))
                    drawCircle(hair, 14f, Offset(0f,0f))
                    drawOval(skin, Offset(-10f,-6f), Size(20f,24f))
                    drawArc(hair, 180f, 180f, true, Offset(-13f,-13f), Size(26f,21f))
                    drawLine(shirt, Offset(-13f,30f), Offset(-27f,52f), 10f, StrokeCap.Round)
                    drawLine(shirt, Offset(14f,30f), Offset(26f,44f), 10f, StrokeCap.Round)
                    drawLine(Color(0xFF40566B), Offset(-9f,66f), Offset(-11f,95f), 11f)
                    drawLine(Color(0xFF40566B), Offset(9f,66f), Offset(13f,95f), 11f)
                    drawRoundRect(Color(0xFFEEE8DC), Offset(-8f,31f), Size(24f,19f), CornerRadius(2f,2f))
                }
            }
            person(323f,72f,Color(0xFFE7A36B),Color(0xFFDCA27C),Color(0xFF644738),1.2f)
            person(270f,102f,Color(0xFFF3F2E9),Color(0xFFC58E68),Color(0xFF303F43),.9f)
            person(92f,83f,Color(0xFF6E9AAB),Color(0xFFAA7151),Color(0xFF3B3830),.85f)
            if (variant == 2) {
                drawRect(Color(0xFFE9D4B7), Offset(211f,144f), Size(184f,7f))
                drawRect(Color(0xFF4C6C7F), Offset(230f,119f), Size(40f,27f))
                drawLine(Color(0xFF677B81), Offset(239f,153f), Offset(237f,180f), 4f)
            }
            for(i in 0..3) drawOval(Color(0xFF719284),Offset(9f+i*6,100f-i*8),Size(8f,33f))
            drawRect(Color(0xFFC89570),Offset(10f,130f),Size(23f,25f))
        }
    }
}
