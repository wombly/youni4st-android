package kz.youni4st.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import kz.youni4st.app.data.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class SalaryMask:VisualTransformation {
    override fun filter(text:AnnotatedString):TransformedText {
        val raw=SalaryInput.digits(text.text)
        return TransformedText(AnnotatedString(SalaryInput.format(raw)),object:OffsetMapping {
            override fun originalToTransformed(offset:Int)=SalaryInput.cursorToFormatted(offset,raw.length)
            override fun transformedToOriginal(offset:Int)=SalaryInput.cursorToOriginal(offset,raw)
        })
    }
}

@Composable
fun ResumeChoice(label:String,value:String,options:List<String>,onChange:(String)->Unit,custom:Boolean=true) {
    var open by rememberSaveable {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,style=MaterialTheme.typography.bodyMedium)
        OutlinedButton({open=true},Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(19.dp),border=BorderStroke(1.dp,Color.Black)) {
            Text(value.ifBlank {t("Выбрать","Таңдау","Choose")},Modifier.weight(1f),color=Ink)
            Icon(Icons.Outlined.ExpandMore,null,tint=Ink)
        }
    }
    if(open) {
        var query by rememberSaveable {mutableStateOf("")}
        val matches=remember(query,options){options.distinct().filter {query.isBlank() || it.contains(query.trim(),true)}}
        AlertDialog(onDismissRequest={open=false},containerColor=CanvasColor,title={Text(label)},text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query,{query=it.take(200)},Modifier.fillMaxWidth(),singleLine=true,
                    label={Text(if(custom)t("Поиск или свой вариант","Іздеу немесе өз нұсқаңыз","Search or your own option") else t("Поиск","Іздеу","Search"))},
                    keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(capitalization=KeyboardCapitalization.Sentences))
                LazyColumn(Modifier.heightIn(min=60.dp,max=300.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(matches,key={it}) {option->
                        OutlinedButton({onChange(option);open=false},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,Color.Black)) {
                            Text(option,Modifier.weight(1f),color=Ink)
                            if(option==value)Icon(Icons.Outlined.Check,null,tint=Ink)
                        }
                    }
                    if(matches.isEmpty())item {Text(t("Нет совпадений","Сәйкестік жоқ","No matches"),color=Muted)}
                }
            }
        },confirmButton={
            if(custom && query.isNotBlank())TextButton({onChange(query.trim());open=false}){Text(t("Использовать свой вариант","Өз нұсқамды қолдану","Use my text"))}
            else TextButton({open=false}){Text(t("Закрыть","Жабу","Close"))}
        },dismissButton={TextButton({onChange("");open=false}){Text(t("Очистить","Тазарту","Clear"))}})
    }
}

@Composable
fun ResumeEmployment(selected:List<String>,onChange:(List<String>)->Unit) {
    var open by rememberSaveable {mutableStateOf(false)}
    val title=t("Тип занятости","Жұмыспен қамту түрі","Employment type")
    val opts=listOf("full_time" to t("Полная занятость","Толық жұмыспен қамту","Full-time"),"part_time" to t("Частичная занятость","Ішінара жұмыспен қамту","Part-time"),"remote" to t("Удалённая работа","Қашықтан жұмыс","Remote"))
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(title,style=MaterialTheme.typography.bodyMedium)
        OutlinedButton({open=true},Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(19.dp),border=BorderStroke(1.dp,Color.Black)) {
            Text(opts.filter {it.first in selected}.joinToString(", "){it.second}.ifBlank {t("Выбрать","Таңдау","Choose")},Modifier.weight(1f),color=Ink)
            Icon(Icons.Outlined.ExpandMore,null,tint=Ink)
        }
    }
    if(open)AlertDialog(onDismissRequest={open=false},title={Text(title)},text={Column {
        opts.forEach {(id,label)->Row(Modifier.fillMaxWidth().clickable {onChange(if(id in selected)selected-id else selected+id)},verticalAlignment=Alignment.CenterVertically) {
            Checkbox(id in selected,{checked->onChange(if(checked)(selected+id).distinct() else selected-id)},colors=CheckboxDefaults.colors(checkedColor=Color.Black,uncheckedColor=Color.Black,checkmarkColor=Color.White))
            Text(label,Modifier.weight(1f))
        }}
    }},confirmButton={TextButton({open=false}){Text(t("Готово","Дайын","Done"))}},dismissButton={TextButton({onChange(emptyList())}){Text(t("Сбросить","Тазарту","Reset"))}})
}

@Composable
fun SkillsEditor(items:List<String>,onChange:(List<String>)->Unit) {
    var open by rememberSaveable {mutableStateOf(false)}
    var editIndex by rememberSaveable {mutableIntStateOf(-1)}
    var text by rememberSaveable {mutableStateOf("")}
    SoftCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(t("Навыки","Дағдылар","Skills"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton({editIndex=-1;text="";open=true}){Icon(Icons.Outlined.AddCircleOutline,t("Добавить навык","Дағды қосу","Add skill"),tint=Navy)}
        }
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            if(items.isEmpty())Text(t("Нажми + и добавь первый навык","+ басып, алғашқы дағдыны қос","Tap + to add your first skill"),color=Muted)
            items.forEachIndexed {i,skill->
                Surface(onClick={editIndex=i;text=skill;open=true},shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,Color.Black),color=Color.White) {
                    Row(Modifier.fillMaxWidth().padding(start=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(skill,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                        IconButton({onChange(items.filterIndexed {index,_->index!=i})}){Icon(Icons.Outlined.Close,t("Удалить навык","Дағдыны жою","Remove skill"))}
                    }
                }
            }
        }
    }
    if(open) PersonalSheet(t("Навык","Дағды","Skill"),{open=false}) {
        PersonalField(t("Название навыка","Дағды атауы","Skill name"),text,{text=it})
        PrimaryButton(if(editIndex>=0)t("Сохранить","Сақтау","Save") else t("Добавить","Қосу","Add"),enabled=text.isNotBlank()) {
            val updated=items.toMutableList()
            if(editIndex in updated.indices) updated[editIndex]=text.trim() else updated.add(text.trim())
            onChange(updated.filter(String::isNotBlank).distinct());open=false
        }
    }
}

@Composable
fun MonthField(label:String,value:String,onChange:(String)->Unit) {
    val lang=LocalLanguage.current
    var open by rememberSaveable {mutableStateOf(false)}
    OutlinedButton({open=true},Modifier.fillMaxWidth(),shape=RoundedCornerShape(19.dp),border=BorderStroke(1.dp,Color.Black)) {
        Text(label+": "+value.takeIf {it.isNotBlank()}?.let {ResumeDates.label(it,lang)}.orEmpty().ifBlank {t("Выбрать","Таңдау","Choose")},Modifier.weight(1f),color=Ink)
        Icon(Icons.Outlined.CalendarMonth,null,tint=Ink)
    }
    if(open) {
        val initial=ResumeDates.month(value) ?: YearMonth.now()
        var month by rememberSaveable {mutableIntStateOf(initial.monthValue)}
        var year by rememberSaveable {mutableIntStateOf(initial.year)}
        val locale=Locale.forLanguageTag(when(lang){Language.RU->"ru";Language.KZ->"kk";Language.EN->"en"})
        val months=(1..12).map {YearMonth.of(2000,it).format(DateTimeFormatter.ofPattern("LLLL",locale))}
        AlertDialog(onDismissRequest={open=false},title={Text(label)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ResumeChoice(t("Месяц","Ай","Month"),months[month-1],months,{month=months.indexOf(it).takeIf {v->v>=0}?.plus(1) ?: month},custom=false)
            ResumeChoice(t("Год","Жыл","Year"),year.toString(),(YearMonth.now().year downTo 1900).map(Int::toString),{year=it.toIntOrNull() ?: year},custom=false)
        }},confirmButton={TextButton({onChange(YearMonth.of(year,month).toString());open=false}){Text(t("Готово","Дайын","Done"))}},dismissButton={TextButton({open=false}){Text(t("Отмена","Бас тарту","Cancel"))}})
    }
}
