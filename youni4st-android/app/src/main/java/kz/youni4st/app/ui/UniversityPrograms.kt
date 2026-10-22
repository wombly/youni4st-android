package kz.youni4st.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.youni4st.app.data.*

@Composable
fun UniversityPrograms(entry:Entry) {
    val lang=LocalLanguage.current
    var open by remember {mutableStateOf(false)}
    val programs=remember(entry.id) {entry.raw.optJSONArray("specialties")?.let {a->(0 until a.length()).map {a.getJSONObject(it)}} ?: emptyList()}
    OutlinedButton({open=true},Modifier.fillMaxWidth().heightIn(min=56.dp),border=BorderStroke(1.dp,Color.Black)) {Text(t("Специальности и программы","Мамандықтар мен бағдарламалар","Specializations and programs")+" · ${programs.size}")}
    if(open) Dialog({open=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        var query by remember {mutableStateOf("")}
        val filtered=remember(programs,query,lang) {programs.filter {it.localized("name",lang).contains(query,ignoreCase=true) || it.optString("code").contains(query,ignoreCase=true)}}
        Surface(Modifier.fillMaxWidth(.95f).fillMaxHeight(.9f),shape=MaterialTheme.shapes.large,color=CanvasColor) {
            Column(Modifier.dismissKeyboardOnBackgroundTap().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(t("Программы обучения","Оқу бағдарламалары","Study programs"),style=MaterialTheme.typography.titleLarge)
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(t("Название или код","Атауы немесе коды","Name or code"))})
                LazyColumn(Modifier.dismissKeyboardOnBackgroundTap().weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(filtered) {program->SoftCard {
                        Text(program.localized("name",lang),fontWeight=FontWeight.SemiBold)
                        Text(listOf(program.optString("code"),program.localized("level",lang)).filter {it.isNotBlank()}.joinToString(" · "),color=Teal)
                        if(program.optBoolean("licensed_only")) Text(t("Направление из лицензии. Конкретные программы уточните у вуза.","Лицензиядағы бағыт. Бағдарламаларды ЖОО-дан нақтылаңыз.","Licensed field. Ask the institution for specific programs."),color=Muted)
                    }}
                    if(filtered.isEmpty()) item {Text(t("Программы не найдены. Уточните список в приёмной комиссии.","Бағдарламалар табылмады. Қабылдау комиссиясынан нақтылаңыз.","No programs found. Contact admissions for the current list."))}
                }
                TextButton({open=false},Modifier.fillMaxWidth()) {Text(t("Закрыть","Жабу","Close"))}
            }
        }
    }
}
