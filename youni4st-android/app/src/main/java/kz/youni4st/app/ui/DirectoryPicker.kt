package kz.youni4st.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A searchable lazy picker remains responsive with several thousand specializations. */
@Composable
fun DirectoryPicker(title:String,value:String,options:List<Pair<String,String>>,onSelected:(String)->Unit) {
    var open by remember {mutableStateOf(false)}
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,Color.Black)) {
        Text(options.find {it.first==value}?.second ?: options.firstOrNull()?.second.orEmpty(),Modifier.weight(1f),color=Ink)
        Icon(Icons.Outlined.ExpandMore,null,tint=Ink)
    }
    if(open) Dialog(onDismissRequest={open=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        var query by remember {mutableStateOf("")}
        val filtered=remember(options,query) {options.filter {query.isBlank() || it.second.contains(query.trim(),ignoreCase=true)}}
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.85f),shape=RoundedCornerShape(24.dp),color=CanvasColor) {
            Column(Modifier.dismissKeyboardOnBackgroundTap().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(title,style=MaterialTheme.typography.titleLarge)
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(t("Поиск","Іздеу","Search"))},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),leadingIcon={Icon(Icons.Outlined.Search,null)})
                LazyColumn(Modifier.dismissKeyboardOnBackgroundTap().weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(filtered,key={it.first}) {option->
                        TextButton(onClick={onSelected(option.first);open=false},modifier=Modifier.fillMaxWidth()) {
                            Text(option.second,Modifier.weight(1f),color=Ink)
                            if(option.first==value) Icon(Icons.Outlined.Check,t("Выбрано","Таңдалды","Selected"),tint=Teal)
                        }
                    }
                    if(filtered.isEmpty()) item {Text(t("Ничего не найдено","Ештеңе табылмады","No matches"))}
                }
                TextButton(onClick={open=false},modifier=Modifier.fillMaxWidth()) {Text(t("Закрыть","Жабу","Close"))}
            }
        }
    }
}
