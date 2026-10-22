package kz.youni4st.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.UsernamePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UsernameGate(vm:AppViewModel) {
    var name by rememberSaveable(vm.accountUser?.uid) {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    val scope=rememberCoroutineScope()
    AlertDialog(onDismissRequest={},properties=DialogProperties(dismissOnBackPress=false,dismissOnClickOutside=false),
        title={Text(t("Твой никнейм","Сенің лақап атың","Your username"))},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(t("Выбери уникальный @username — он появится в профиле. Имя закрепляется за твоим аккаунтом в базе.","Бірегей @username таңда — ол профильде көрсетіліп, базада аккаунтыңа тіркеледі.","Choose a unique @username for your profile. It is reserved for your account in the database."))
            if(vm.usernameBusy || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(vm.usernameLoaded) {
                OutlinedTextField(name,{name=UsernamePolicy.normalize(it)},label={Text("@username *")},singleLine=true,enabled=!busy,
                    modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(
                        capitalization=androidx.compose.ui.text.input.KeyboardCapitalization.None,
                        keyboardType=androidx.compose.ui.text.input.KeyboardType.Ascii))
                Text(t("3–24 символа: латинские буквы, цифры и _. Начни с буквы.","3–24 таңба: латын әріптері, сандар және _. Әріптен баста.","3–24 characters: Latin letters, digits and _. Start with a letter."),style=MaterialTheme.typography.bodySmall)
            }
            val issue=error.ifBlank {vm.usernameError}
            if(issue.isNotBlank()) Text(accountErrorText(issue),color=MaterialTheme.colorScheme.error)
        }},
        confirmButton={
            if(!vm.usernameLoaded) TextButton({vm.refreshUsername()},enabled=!vm.usernameBusy) {Text(t("Повторить","Қайталау","Retry"))}
            else TextButton({scope.launch {
                busy=true;error=""
                try {vm.reserveUsername(name)} catch(e:CancellationException){throw e}
                catch(e:Exception){error=accountErrorCode(e)} finally {busy=false}
            }},enabled=!busy && !vm.usernameBusy && UsernamePolicy.valid(name)) {Text(t("Сохранить","Сақтау","Save"))}
        },dismissButton={TextButton({vm.signOut()},enabled=!busy) {Text(t("Выйти из аккаунта","Аккаунттан шығу","Sign out"))}})
}
