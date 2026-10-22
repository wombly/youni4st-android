package kz.youni4st.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.Resume
import org.json.JSONObject
import java.io.File

@Composable
fun ChatScreen(vm: AppViewModel, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var clearDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var voiceError by rememberSaveable {mutableStateOf(false)}
    var voiceBusy by rememberSaveable {mutableStateOf(false)}
    val speechLanguage=when(LocalLanguage.current){kz.youni4st.app.data.Language.RU->"ru-RU";kz.youni4st.app.data.Language.KZ->"kk-KZ";else->"en-US"}
    val voicePrompt=t("Задай вопрос Сократусу","Сократусқа сұрақ қой","Ask Sokratus a question")
    val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
        voiceBusy=false
        if(result.resultCode==android.app.Activity.RESULT_OK) {
            val words=result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(words.isNotBlank()) input=listOf(input.trim(),words.trim()).filter {it.isNotEmpty()}.joinToString(" ").take(2000)
            else voiceError=true
        }
    }
    fun send() { if (input.isNotBlank() && !vm.chatBusy) { vm.send(input); input = "" } }
    LaunchedEffect(vm.messages.size, vm.chatBusy, vm.chatFailed) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Column(Modifier.dismissKeyboardOnBackgroundTap().fillMaxSize().background(FormBackground).imePadding()) {
        Surface(color = Color.White) {
            PageTitle(t("AI Сократус", "AI - SOKRATUS", "AI - SOKRATUS"), onBack) {
                IconButton({ clearDialog = true }, enabled = vm.messages.isNotEmpty() && !vm.chatBusy) {
                    Icon(Icons.Outlined.DeleteOutline, t("Очистить чат", "Чатты тазарту", "Clear chat"), tint = if (vm.messages.isNotEmpty()) Color(0xFFC97B7B) else Muted)
                }
            }
        }
        LazyColumn(Modifier.dismissKeyboardOnBackgroundTap().weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Surface(shape=RoundedCornerShape(28.dp),color=Navy) {
                    Column(Modifier.dismissKeyboardOnBackgroundTap().fillMaxWidth().background(Brush.linearGradient(listOf(Navy,Color(0xFF236E8C)))).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp).background(Color.White.copy(alpha=.16f),CircleShape),contentAlignment=Alignment.Center) {Icon(Icons.Outlined.AutoAwesome,null,tint=Color.White)}
                            Column {Text("S O K R A T U S",color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(t("Навигатор в сфере образования","Білімге бастар жол","Your Education Navigator"),color=Color(0xFFCAE3EE),fontSize=12.sp)}
                        }
                        Text(t("Твой ИИ-ассистент.","Сенің ЖИ-көмекшің.","Your AI-assistant."),color=Color.White,style=MaterialTheme.typography.headlineSmall)
                        Text(t("Учёба, гранты и первый шаг в карьере.","Оқу, гранттар және мансаптың алғашқы қадамы.","Study, grants and your first career move."),color=Color(0xFFD4E5EE),fontSize=12.sp)
                    }
                }
            }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(t("Как выбрать вуз?","ЖОО қалай таңдаймын?","How do I choose a university?"),t("Как подготовиться к стажировке?","Тағылымдамаға қалай дайындаламын?","How do I prepare for an internship?"),t("Какие бывают гранты?","Қандай гранттар бар?","What grants are available?")).forEach {prompt ->
                        Surface(onClick={vm.send(prompt)},enabled=!vm.chatBusy,shape=RoundedCornerShape(18.dp),color=Color.White,border=BorderStroke(1.dp,Color(0xFFDDE5EC))) {
                            Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically) {Text(prompt,Modifier.weight(1f),fontSize=14.sp);Icon(Icons.Outlined.NorthEast,null,tint=Teal,modifier=Modifier.size(18.dp))}
                        }
                    }
                }
            }
            item {
                ChatBubble(t("Привет, я твой друг SOKRATUS!. Не парься, я помогу тебе найти себя, а также я могу найти ВУЗ, работу и т.д. Короче, задавай вопрос и там разберемся.", "Сәлем, мен сенің досың SOKRATUS-пын!. Уайымдама, өз жолыңды табуға көмектесемін. Оған қоса, саған университет, жұмыс және тағы басқа нәрселерді табуға көмектесе аламын. Қысқасы, сұрағыңды қоя бер, бірге шешеміз!", "Yo, I'm your buddy SOKRATUS!. Don't stress, I'll help you figure yourself out. I can also help you find a university, a job, and all that stuff. Anyway, just ask me anything and we'll figure it out."), false)
            }
            itemsIndexed(vm.messages) { _, message -> ChatBubble(message.content, message.role == "user") }
            if (vm.chatBusy) item {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Teal)
                    Text(t("Сократус думает…", "Сократус ойлануда…", "Sokratus is thinking…"), color = Muted, fontSize = 13.sp)
                }
            }
            if (vm.chatFailed) item {
                SoftCard {
                    Text(t("Сервер пока не ответил. Проверь интернет и попробуй ещё раз. История сохранена.", "Сервер жауап бермеді. Интернетті тексеріп, қайталап көр. Тарих сақталды.", "The server did not respond. Check your connection and try again. Your history is saved."), color = Muted, style = MaterialTheme.typography.bodyMedium)
                    TextButton({ vm.messages.lastOrNull()?.let { vm.send(it.content, retry = true) } }) { Text(t("Повторить запрос", "Сұрауды қайталау", "Try again")) }
                }
            }
        }
        Surface(color = Color.White) {
            Column(Modifier.dismissKeyboardOnBackgroundTap().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextField(input, { input = it.take(2000) }, Modifier.weight(1f), maxLines = 5,
                        placeholder = { Text(t("Напиши вопрос…", "Сұрақ жаз…", "Ask a question…"), fontSize = 14.sp) },
                        shape = RoundedCornerShape(26.dp), colors = TextFieldDefaults.colors(focusedContainerColor = CanvasColor, unfocusedContainerColor = CanvasColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }))
                    IconButton({
                        voiceError=false
                        val intent=android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,speechLanguage)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,voicePrompt)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,1)
                        try {voiceBusy=true;voice.launch(intent)} catch(_:android.content.ActivityNotFoundException){voiceBusy=false;voiceError=true} catch(_:SecurityException){voiceBusy=false;voiceError=true}
                    },enabled=!voiceBusy && !vm.chatBusy && input.length<2000,modifier=Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Mic,t("Продиктовать вопрос","Сұрақты дауыспен енгізу","Dictate a question"),tint=Teal)
                    }
                    IconButton({ send() }, enabled = input.isNotBlank() && !vm.chatBusy, modifier = Modifier.size(52.dp).background(Navy, CircleShape)) {
                        Icon(Icons.AutoMirrored.Outlined.Send, t("Отправить", "Жіберу", "Send"), tint = if (input.isNotBlank() && !vm.chatBusy) Color.White else Color(0xFF8190A8))
                    }
                }
                if(voiceError) Text(t("Голосовой ввод недоступен. Проверь разрешение микрофона и службу распознавания речи на телефоне.","Дауыспен енгізу қолжетімсіз. Микрофон рұқсатын және сөйлеуді тану қызметін тексер.","Voice input is unavailable. Check microphone permission and your phone’s speech recognition service."),color=MaterialTheme.colorScheme.error,fontSize=12.sp)
                Text(t("Ответы AI могут содержать ошибки. Уточняй важные условия в вузе.", "ЖИ жауаптарында қателер болуы мүмкін. Маңызды шарттарды ЖОО-дан нақтыла.", "AI can make mistakes. Confirm important details with the university."), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
    if (clearDialog) AlertDialog(onDismissRequest = { clearDialog = false }, title = { Text(t("Очистить историю?", "Тарихты тазарту керек пе?", "Clear conversation?")) },
        text = { Text(t("Сообщения будут удалены с этого телефона.", "Хабарлар осы телефоннан жойылады.", "Messages will be removed from this phone.")) },
        confirmButton = { TextButton({ vm.clearChat(); clearDialog = false }) { Text(t("Очистить", "Тазарту", "Clear")) } },
        dismissButton = { TextButton({ clearDialog = false }) { Text(t("Отмена", "Бас тарту", "Cancel")) } })
}

@Composable
private fun ChatBubble(text: String, user: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(Modifier.widthIn(max = 380.dp).fillMaxWidth(if (user) .87f else .96f), color = if (user) Navy else Color.White,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = if (user) 5.dp else 22.dp, bottomStart = if (user) 22.dp else 5.dp)) {
            Text(text, Modifier.padding(16.dp), color = if (user) Color.White else Ink, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

