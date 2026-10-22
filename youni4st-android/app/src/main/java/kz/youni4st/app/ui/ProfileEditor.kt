@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package kz.youni4st.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.*
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun ProfileEditor(vm:AppViewModel,onLogin:()->Unit,onDismiss:()->Unit) {
    var draft by rememberSaveable(stateSaver=Saver<UserProfile,String>(save={it.toJson().toString()},restore={UserProfile.fromJson(JSONObject(it))})) {
        mutableStateOf(vm.profile.copy(name=vm.profile.name.ifBlank {vm.accountUser?.displayName.orEmpty()},email=vm.profile.email.ifBlank {vm.accountUser?.email.orEmpty()}))
    }
    val scope=rememberCoroutineScope()
    var saving by remember {mutableStateOf(false)}
    var saveError by remember {mutableStateOf("")}
    var interest by rememberSaveable {mutableStateOf("")}
    var error by rememberSaveable {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    fun dismiss(){if(saving)return;if(draft!=vm.profile)discard=true else onDismiss()}
    val invalidPhone=!PersonalValidation.phone(draft.phone)
    val invalidEmail=draft.email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(draft.email).matches()
    val invalidUsername=!UsernamePolicy.valid(draft.username)
    PersonalSheet(t("Редактировать профиль","Профильді өңдеу","Edit profile"),::dismiss) {
        if(saving) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(t("Сохраняем профиль…","Профиль сақталуда…","Saving your profile…"))
            return@PersonalSheet
        }
        PersonalPhoto(draft.photo,{draft=draft.copy(photo=it)},{busy=it},t("Фото профиля","Профиль фотосы","Profile photo"))
        PersonalField(t("Имя*","Аты *","First name *"),draft.name,{draft=draft.copy(name=it)},Icons.Outlined.PersonOutline,error=error && draft.name.isBlank())
        PersonalField("@username *",draft.username,{draft=draft.copy(username=UsernamePolicy.normalize(it))},Icons.Outlined.AlternateEmail,error=error && invalidUsername)
        Text(accountErrorText("INVALID_USERNAME").substringBefore(" ["),style=MaterialTheme.typography.bodySmall,color=Muted)
        if(vm.accountUser==null) TextButton(onLogin) {Text(t("Войти, чтобы зарегистрировать никнейм","Лақап атты тіркеу үшін кіру","Sign in to register a username"))}
        PersonalField("E-mail",draft.email,{draft=draft.copy(email=it.trim())},Icons.Outlined.Email,KeyboardType.Email,error=error && invalidEmail)
        PersonalField(t("Номер телефона","Телефон нөмірі","Phone number"),draft.phone,{draft=draft.copy(phone=it)},Icons.Outlined.Phone,phone=true,error=error && invalidPhone)
        HorizontalDivider(Modifier.padding(top=10.dp))
        Text(t("Интересы","Қызығушылықтар","Interests"),style=MaterialTheme.typography.titleLarge)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {PersonalField(t("Добавить интерес","Қызығушылық қосу","Add interest"),interest,{interest=it})}
            IconButton({if(interest.isNotBlank()){draft=draft.copy(interests=(draft.interests+interest.trim()).distinct());interest=""}},enabled=interest.isNotBlank()) {Icon(Icons.Outlined.AddCircle,t("Добавить","Қосу","Add"),tint=Navy)}
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {draft.interests.forEach {item ->
            InputChip(selected=false,onClick={draft=draft.copy(interests=draft.interests-item)},label={Text(item)},trailingIcon={Icon(Icons.Outlined.Close,t("Удалить интерес","Қызығушылықты жою","Remove interest"))})
        }}
        if(error) Text(t("Заполни имя и никнейм. Проверь e-mail и телефон, если они указаны.","Атыңды және лақап атыңды толтыр. Пошта мен телефон көрсетілсе, тексер.","Enter your name and username. Check email and phone if provided."),color=MaterialTheme.colorScheme.error)
        if(saveError.isNotBlank()) Text(accountErrorText(saveError),color=MaterialTheme.colorScheme.error)
        PrimaryButton(t("Сохранить","Сақтау","Save"),enabled=!busy && !saving) {
            error=draft.name.isBlank() || invalidPhone || invalidEmail || invalidUsername
            if(!error) {val submitted=draft.copy(name=draft.name.trim());scope.launch {
                saving=true;saveError=""
                try {vm.saveProfile(submitted);onDismiss()}
                catch(e:CancellationException){throw e}
                catch(e:Exception){saveError=accountErrorCode(e)}
                finally {saving=false}
            }}
        }
    }
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text(t("Закрыть без сохранения?","Сақтамай жабу керек пе?","Discard changes?"))},
        confirmButton={TextButton(onDismiss){Text(t("Закрыть","Жабу","Discard"))}},dismissButton={TextButton({discard=false}){Text(t("Продолжить","Жалғастыру","Keep editing"))}})
}
