package kz.youni4st.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.BuildConfig
import kz.youni4st.app.R
import kz.youni4st.app.data.Language

@Composable
fun ContactAction(value:String,kind:String,label:String,icon:ImageVector) {
    val context=LocalContext.current
    var error by remember {mutableStateOf(false)}
    val valid=when(kind) {"email"->android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();else-> value.filter(Char::isDigit).length in 7..15}
    OutlinedButton({
        val intent=if(kind=="email") Intent(Intent.ACTION_SENDTO,Uri.fromParts("mailto",value,null)) else Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",value.filter {it.isDigit() || it=='+'},null))
        error=runCatching {context.startActivity(intent)}.isFailure
    },enabled=valid,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(16.dp)) {Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(if(valid) "$label · $value" else label)}
    if(error) Text(t("Нет приложения для этого действия","Бұл әрекетке арналған қолданба жоқ","No app available for this action"),color=MaterialTheme.colorScheme.error)
}

@Composable
fun SettingsScreen(vm:AppViewModel,onBack:()->Unit,navigate:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(t("Язык и помощь","Тіл және көмек","Language & help"),onBack)
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            SoftCard {
                Language.entries.forEach { language ->
                    Row(Modifier.fillMaxWidth().clickable {vm.changeLanguage(language)}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(vm.language==language,{vm.changeLanguage(language)})
                        Text(when(language){Language.RU->"Русский";Language.KZ->"Қазақша";Language.EN->"English"})
                    }
                }
            }
            MenuRow(t("Поддержка","Қолдау","Support"),t("Контакты и ответы","Байланыс және жауаптар","Contacts and answers"),Icons.Outlined.SupportAgent) {navigate("support")}
            MenuRow(t("О приложении","Қолданба туралы","About"),"Youni4st · ${BuildConfig.VERSION_NAME}",Icons.Outlined.Info) {navigate("about")}
        }
    }
}

@Composable
fun SupportScreen(vm:AppViewModel,onBack:()->Unit,navigate:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(t("Поддержка","Қолдау","Support"),onBack)
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(t("Поможем разобраться","Түсінуге көмектесеміз","Let’s figure it out"),style=MaterialTheme.typography.headlineMedium)
            Text(t("Быстрая связь","Жылдам байланыс","Get in touch"),style=MaterialTheme.typography.titleMedium)
            SoftCard {
                ExternalLinkButton(vm.config.group("support","whatsapp"),"WhatsApp",note=false)
                ExternalLinkButton(vm.config.group("support","telegram"),"Telegram",note=false)
                ContactAction(vm.config.group("support","phone"),"phone",t("Позвонить","Қоңырау шалу","Call"),Icons.Outlined.Call)
                ContactAction(vm.config.group("support","email"),"email","E-mail",Icons.Outlined.MailOutline)
                if(listOf("whatsapp","telegram","phone","email").all{vm.config.group("support",it).isBlank()}) Text(t("Контакты поддержки скоро появятся. Пока загляни в ответы ниже.","Қолдау байланыстары жақында қосылады. Төмендегі жауаптарды қара.","Support contacts are coming soon. Browse the answers below."),color=Muted)
            }
            PrimaryButton(t("Спросить AI Сократуса","AI Сократустан сұра","Ask AI Sokratus")) {navigate("chat")}
            Text(t("Частые вопросы","Жиі қойылатын сұрақтар","Common questions"),style=MaterialTheme.typography.titleLarge)
            Accordion(t("Где уточнить данные о вузе?","ЖОО деректерін қайдан нақтылауға болады?","Where can I verify university details?"),Icons.Outlined.School) {Text(t("Открой карточку вуза и раздел «Контактная информация». Условия поступления, стоимость и проходные баллы уточняй у приёмной комиссии.","ЖОО карточкасындағы байланыс бөлімін ашып, қабылдау комиссиясына хабарлас.","Open the university’s contact details. Confirm admission requirements, fees and scores with admissions."))}
            Accordion(t("Как сохранить резюме?","Түйіндемені қалай сақтауға болады?","How do I save my resume?"),Icons.Outlined.Description) {Text(t("В профиле открой «Моё резюме». «Сохранить» оставит данные в приложении, «Скачать резюме · PDF» сохранит документ в выбранную папку.","Профильден түйіндемені аш. «Сақтау» деректерді қолданбада, ал PDF түймесі файлды қалтада сақтайды.","Open My resume. Save keeps your data in the app; Download resume · PDF exports a document to your chosen folder."))}
            Accordion(t("Почему AI не отвечает?","AI неге жауап бермейді?","Why is AI not responding?"),Icons.Outlined.AutoAwesome) {Text(t("Проверь соединение и повтори запрос. Ответ также зависит от доступности сервера и сервиса искусственного интеллекта.","Интернетті тексеріп, қайталап көр. Жауап сервердің қолжетімділігіне де байланысты.","Check your connection and retry. Replies also depend on the backend and AI service availability."))}
        }
    }
}

@Composable
fun AboutScreen(vm:AppViewModel,onBack:()->Unit,navigate:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(t("О приложении","Қолданба туралы","About"),onBack)
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
            Image(painterResource(R.drawable.brand_logo),"Youni4st",Modifier.width(220.dp).height(48.dp).align(Alignment.CenterHorizontally),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
            Text("Youni4st ${BuildConfig.VERSION_NAME}",color=Muted,modifier=Modifier.align(Alignment.CenterHorizontally))
            Text(t("Твой персональный навигатор в мире образования и карьеры.","Білім мен мансап әлеміндегі жеке навигаторың.","Your personal guide to education and career opportunities."),style=MaterialTheme.typography.headlineMedium)
            Text(t("Мы помогаем выбрать вуз, найти образовательную программу, первый опыт и наставника. Решения остаются за тобой — мы делаем возможности понятнее.","ЖОО, білім бағдарламасы, алғашқы тәжірибе және тәлімгер табуға көмектесеміз. Таңдау өзіңде.","Find a university, a learning program, work experience and a mentor. We make the options clearer; the decisions are yours."),color=Muted)
            Text(t("Мы в соцсетях","Біз әлеуметтік желілерде","Follow our story"),style=MaterialTheme.typography.titleMedium)
            listOf("instagram" to "Instagram","tiktok" to "TikTok","telegram" to "Telegram").forEach {(key,label)->ExternalLinkButton(vm.config.group("social",key),label,note=false)}
            MenuRow(t("Политика конфиденциальности","Құпиялық саясаты","Privacy Policy"),t("Как используются данные","Деректерді пайдалану","How data is used"),Icons.Outlined.PrivacyTip) {navigate("privacy")}
            MenuRow(t("Условия использования","Пайдалану шарттары","Terms of Use"),t("Правила сервиса","Қызмет ережелері","Using the service"),Icons.Outlined.Description) {navigate("terms")}
            Text("© 2026 Youni4st",color=Muted,fontSize=12.sp)
        }
    }
}

@Composable
fun LegalScreen(vm:AppViewModel,privacy:Boolean,onBack:()->Unit) {
    val context=LocalContext.current
    val data=remember {org.json.JSONObject(context.assets.open("legal.json").bufferedReader().use{it.readText()})}
    val kind=if(privacy)"privacy" else "terms"
    val lang=LocalLanguage.current.name
    val content=data.getJSONObject(kind).optString(lang).ifBlank{data.getJSONObject(kind).getString("RU")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle(if(privacy)t("Конфиденциальность","Құпиялық","Privacy") else t("Условия","Шарттар","Terms"),onBack)
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(t("Информация для тестовой версии","Тест нұсқасына арналған ақпарат","Information for the test version"),color=Teal,fontWeight=FontWeight.SemiBold)
            Text(content,style=MaterialTheme.typography.bodyLarge)
            val url=vm.config.value(if(privacy)"privacy_url" else "terms_url")
            if(url.isNotBlank()) ExternalLinkButton(url,t("Открыть полный документ","Толық құжатты ашу","Open full document"))
        }
    }
}
