@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package kz.youni4st.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.*
import org.json.JSONObject

@Composable
fun EntrySheet(entry: Entry, vm: AppViewModel, onDismiss: () -> Unit, onResume: () -> Unit, onLogin: () -> Unit) {
    val lang=LocalLanguage.current
    var confirmSend by remember(entry.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=CanvasColor) {
        Column(Modifier.fillMaxWidth().heightIn(max=760.dp).verticalScroll(rememberScrollState()).padding(start=22.dp,end=22.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                if(entry.kind in setOf("mentors","universities","jobs","programs")) EntityImage(entry,76.dp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    if(entry.kind=="programs") Text(entry.raw.optString("company"),color=Teal,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Text(entry.title(lang),style=MaterialTheme.typography.titleLarge)
                    if(entry.kind!="programs") Text(entry.subtitle(lang),color=Muted,style=MaterialTheme.typography.bodyMedium)
                    if(entry.kind=="mentors") Text(entry.text("company",lang),color=Teal,fontSize=12.sp)
                }
                FavoriteButton(entry.id in vm.favorites,{vm.toggleFavorite(entry.id)})
            }
            if(entry.kind=="mentors") Text(t("Опыт, который помогает расти","Өсуге көмектесетін тәжірибе","Experience to help you grow"),style=MaterialTheme.typography.titleMedium)
            if(entry.description(lang).isNotBlank()) Text(entry.description(lang).replace("\\n","\n"),style=MaterialTheme.typography.bodyLarge,color=Ink)
            when(entry.kind) {
                "programs" -> if(!entry.raw.optBoolean("archived")) ProgramActions(entry)
                "ielts","grants" -> if(!entry.raw.optBoolean("archived")) ExternalLinkButton(entry.raw.optString("source_url"),t("Официальный источник","Ресми дереккөз","Official source"))
                "mentors" -> {
                    SoftCard {
                        DetailRow(t("Опыт","Тәжірибе","Experience"),entry.text("experience",lang),Icons.Outlined.WorkHistory)
                        DetailRow(t("Язык консультаций","Кеңес тілі","Languages"),entry.text("languages",lang).ifBlank { t("Уточни у наставника","Тәлімгерден нақтыла","Ask the mentor") },Icons.Outlined.Language)
                        DetailRow(t("Площадка","Платформа","Platform"),entry.text("platform",lang).ifBlank { t("Профиль наставника","Тәлімгер профилі","Mentor profile") },Icons.Outlined.Public)
                        DetailRow(t("Стоимость","Бағасы","Price"),entry.text("price",lang),Icons.Outlined.Payments)
                    }
                    Text(t("Стоимость и доступное время уточняй на странице наставника.","Баға мен бос уақытты тәлімгер парақшасынан нақтыла.","Confirm current fees and availability on the mentor’s page."),color=Muted,fontSize=12.sp)
                    ExternalLinkButton(entry.raw.optString("profile_url"),t("Перейти к наставнику","Тәлімгерге өту","Visit mentor"),entry.text("platform",lang))
                }
                "universities" -> {
                    SoftCard {
                        DetailRow(t("Порог из каталога","Каталогтағы шек","Catalog threshold"),if(entry.raw.isNull("min_score")) t("По программе","Бағдарлама бойынша","By program") else "${entry.raw.optInt("min_score")}+",Icons.Outlined.EmojiEvents)
                        DetailRow(t("Стоимость обучения","Оқу ақысы","Tuition"),entry.text("price",lang),Icons.Outlined.Payments)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(entry.raw.optBoolean("has_dormitory")) Badge(t("Общежитие","Жатақхана","Dormitory"),Navy)
                            if(entry.raw.optBoolean("has_military_dept")) Badge(t("Военная кафедра","Әскери кафедра","Military department"),Navy)
                        }
                    }
                    UniversityPrograms(entry)
                    Accordion(t("Официальные сведения","Ресми мәліметтер","Official information"),Icons.Outlined.Info) {
                        Text(entry.text("legal_name",lang))
                        Text(t("Проверено: 16.10.2026","Тексерілді: 16.10.2026","Checked: 16 Oct 2026"),color=Muted)
                        Text(t("Статус лицензии в паспорте: ","Паспорттағы лицензия мәртебесі: ","License status in the passport: ")+entry.raw.optString("license_status"),color=Muted)
                        ExternalLinkButton("https://epvo.kz/passport-universities/${entry.raw.optInt("epvo_id")}",t("Паспорт организации в ЕПВО","ЕПВО ұйым паспорты","EPVO institution passport"),note=false)
                    }
                    Accordion(t("Общежитие и военная кафедра","Жатақхана және әскери кафедра","Housing and military department"),Icons.Outlined.Apartment) {
                        val dorm=entry.raw.optString("dormitory_source")
                        val military=entry.raw.optString("military_source")
                        Text(t("Общежитие: ","Жатақхана: ","Dormitory: ")+if(dorm.isNotBlank()) t("есть; наличие мест уточняется","бар; орындарды нақтылаңыз","available; confirm vacancies") else t("уточняется","нақтыланады","not confirmed"))
                        if(dorm.isNotBlank()) ExternalLinkButton(dorm,t("Информация об общежитии","Жатақхана туралы","Housing information"),note=false)
                        Text(t("Военная кафедра: ","Әскери кафедра: ","Military department: ")+if(military.isNotBlank()) t("есть информация на официальном сайте","ресми сайтта ақпарат бар","information on the official website") else t("уточняется","нақтыланады","not confirmed"))
                        if(military.isNotBlank()) ExternalLinkButton(military,t("Информация о кафедре","Кафедра туралы","Department information"),note=false)
                    }
                    Accordion(t("Контактная информация","Байланыс ақпараты","Contact information"),Icons.Outlined.ContactPhone) {
                        val contacts=entry.raw.optJSONObject("contacts") ?: JSONObject()
                        val address=contacts.localized("address",lang)
                        if(address.isNotBlank()) Text(address)
                        if(contacts.optString("phone").isBlank()) Text(t("Телефон: уточняется","Телефон: нақтыланады","Phone: not confirmed"),color=Muted)
                        if(lang==kz.youni4st.app.data.Language.RU && contacts.optString("phone_role").isNotBlank()) Text(contacts.optString("phone_role"),color=Muted)
                        ContactAction(contacts.optString("phone"),"phone",t("Позвонить","Қоңырау шалу","Call"),Icons.Outlined.Call)
                        ContactAction(contacts.optString("email"),"email",t("Написать","Жазу","Email"),Icons.Outlined.MailOutline)
                        if(contacts.optString("phone_source").isNotBlank()) ExternalLinkButton(contacts.optString("phone_source"),t("Источник контактов","Байланыс дереккөзі","Contact source"),note=false)
                        if(contacts.optString("website").isNotBlank()) ExternalLinkButton(contacts.optString("website"),t("Сайт университета","Университет сайты","University website"),note=false)
                        if(contacts.length()==0 || listOf(contacts.optString("phone"),contacts.optString("email"),contacts.optString("website"),address).all { it.isBlank() }) Text(t("Контакты скоро появятся","Байланыс ақпараты жақында қосылады","Contact details are coming soon"),color=Muted)
                    }
                    Text(t("Баллы и стоимость зависят от программы. Уточняй условия у приёмной комиссии.","Балл мен бағаны қабылдау комиссиясынан нақтыла.","Confirm scores, fees and conditions with admissions."),color=Muted,style=MaterialTheme.typography.bodyMedium)
                }
                "jobs" -> {
                    SoftCard {
                        DetailRow(t("Зарплата","Жалақы","Salary"),entry.text("salary",lang),Icons.Outlined.AccountBalanceWallet)
                        DetailRow(t("Город","Қала","City"),entry.text("city",lang),Icons.Outlined.LocationOn)
                        DetailRow(t("Опыт","Тәжірибе","Experience"),entry.text("experience",lang),Icons.Outlined.WorkHistory)
                    }
                    if(entry.text("requirements",lang).isNotBlank()) {
                        Text(t("Требования","Талаптар","Requirements"),style=MaterialTheme.typography.titleMedium)
                        Text(entry.text("requirements",lang))
                        Text(entry.text("type",lang),color=Teal)
                    }
                    if(entry.raw.optString("source")=="Enbek") {
                        Text("Enbek · "+t("Проверено: ","Тексерілді: ","Checked: ")+entry.raw.optString("checked_at"),color=Muted,fontSize=12.sp)
                        Text(t("Действует до: ","Мерзімі: ","Valid through: ")+entry.raw.optString("valid_through"),color=Muted,fontSize=12.sp)
                        val active=jobIsActive(entry)
                        if(!active) Text(t("Срок объявления истёк.","Хабарландыру мерзімі аяқталды.","This listing has expired."),color=Muted)
                        enbekApplicationUrl(entry)?.let { url ->
                            ExternalLinkButton(url,if(active)t("Откликнуться на Enbek","Enbek арқылы өтінім беру","Apply on Enbek") else t("Открыть источник","Дереккөзді ашу","View source"),note=false)
                        }
                        Text(t("Отклик оформляется на Enbek по условиям объявления. Резюме из Youni4st автоматически не передаётся; его можно сохранить в PDF и поделиться отдельно.","Өтінім Enbek хабарландыруының шарттары бойынша беріледі. Youni4st түйіндемесі автоматты түрде жіберілмейді; оны PDF түрінде сақтап, бөлек бөлісуге болады.","Apply through the original Enbek listing. Your Youni4st resume is not transferred automatically; save it as a PDF to share separately."),color=Muted,fontSize=12.sp)
                    } else if(!entry.raw.optBoolean("archived")) {
                    val configured=safeWebUrl(vm.config.value("applications_endpoint"))!=null
                    val sent=vm.isSent(entry.id)
                    PrimaryButton(if(sent)t("Отправлено","Жіберілді","Sent") else if(vm.applicationBusy==entry.id)t("Отправляем…","Жіберілуде…","Sending…") else t("Отправить","Жіберу","Send"),enabled=!sent && vm.applicationBusy==null && configured) {
                        if(vm.accountUser==null) onLogin() else confirmSend=true
                    }
                    if(!configured) Text(t("Приём откликов на эту вакансию пока недоступен.","Бұл бос орынға өтінім қабылдау әзірге қолжетімсіз.","Applications for this opening are not available yet."),color=Muted,fontSize=12.sp)
                    if(sent) Text(t("Сервис подтвердил получение отклика.","Қызмет өтінімнің қабылданғанын растады.","The service has confirmed receipt of your application."),color=Teal)
                    if(vm.applicationError) Text(t("Отправка не подтверждена. Попробуй ещё раз.","Жіберу расталмады. Қайталап көр.","Delivery was not confirmed. Please try again."),color=MaterialTheme.colorScheme.error)
                    } else Text(t("Архивная вакансия","Мұрағаттағы бос орын","Archived opening"),color=Muted)
                    OutlinedButton(onResume,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(18.dp)) { Text(t("Изменить резюме","Түйіндемені өзгерту","Edit resume")) }
                }
                "news" -> {
                    Text(entry.raw.optString("published_at")+" · "+entry.raw.optString("source"),color=Muted,fontSize=12.sp)
                    ExternalLinkButton(entry.raw.optString("source_url"),t("Читать первоисточник","Түпнұсқаны оқу","Read the source"),entry.raw.optString("source"))
                }
                "unt" -> entry.raw.optJSONObject(lang.name)?.optJSONObject("sections")?.let { sections ->
                    sections.keys().forEach { key -> sections.optJSONObject(key)?.let { section ->
                        Accordion(section.optString("title"),Icons.Outlined.MenuBook) { Text(section.optString("content").replace("\\n","\n")) }
                    } }
                }
            }
            if(entry.kind in setOf("programs","grants","ielts")) Text(t("Перед подачей уточни актуальные условия у организатора.","Өтінім алдында ұйымдастырушыдан шарттарды нақтыла.","Confirm current terms with the organizer before applying."),color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
    }
    if(confirmSend) AlertDialog(onDismissRequest={confirmSend=false},title={Text(t("Отправить резюме?","Түйіндемені жіберу керек пе?","Send your resume?"))},
        text={Text(t("Имя, телефон, город, опыт, образование и навыки будут переданы сервису для этой вакансии. Фото не отправляется.","Аты-жөнің, телефоның, қалаң, тәжірибең мен дағдыларың осы бос орынға жіберіледі. Фото жіберілмейді.","Your name, phone, city, experience, education and skills will be sent for this opening. Your photo is not sent."))},
        confirmButton={TextButton({confirmSend=false;vm.submit(entry)},enabled=vm.resume.name.isNotBlank() && vm.resume.phone.isNotBlank()) {Text(t("Отправить","Жіберу","Send"))}},
        dismissButton={TextButton({confirmSend=false;onResume()}) {Text(t("Проверить резюме","Түйіндемені тексеру","Review resume"))}})
}
@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(38.dp).background(Color(0xFFEAF0E1), RoundedCornerShape(12.dp)), contentAlignment=Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = Teal) }
        Text(label, Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, Modifier.widthIn(max = 185.dp), color = Navy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun sectionTitle(kind: String) = when (kind) {
    "news" -> t("Новости образования", "Білім жаңалықтары", "Education news")
    "mentors" -> t("Наставники", "Тәлімгерлер", "Mentors")
    "programs" -> t("Образовательные программы", "Білім беру бағдарламалары", "Education programs")
    "grants" -> t("Гранты", "Гранттар", "Grants")
    "ielts" -> "IELTS"
    "unt" -> t("ЕНТ 2026", "ҰБТ 2026", "UNT 2026")
    "universities" -> t("Выбрать вуз", "ЖОО таңдау", "Find a university")
    "jobs" -> t("Работа и стажировки", "Жұмыс пен тағылымдама", "Jobs and internships")
    else -> "Youni4st"
}

@Composable
fun ArticlesScreen(vm: AppViewModel, kind: String, onBack: () -> Unit, onEntry: (String) -> Unit, onSaved:()->Unit) {
    val lang = LocalLanguage.current
    Column {
        PageTitle(sectionTitle(kind), onBack)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { if(kind in setOf("programs","ielts","grants","news")) CatalogHero(kind,vm.catalog!!.entries.count {it.kind==kind && it.id in vm.favorites},onSaved) else Text(if (kind=="news") t("Подборка · 14 октября 2026", "Жинақ · 2026 жылғы 14 қазан", "Selected · 14 October 2026") else t("Найди то, что подходит тебе", "Өзіңе сай мүмкіндікті тап", "Find what fits your next step"), color = Muted, style = MaterialTheme.typography.bodyMedium) }
            items(vm.catalog!!.of(kind), key = { it.id }) { entry ->
                SoftCard(Modifier.fillMaxWidth(), { onEntry(entry.id) }) {
                    if(kind in setOf("grants","ielts","news")) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(entry.title(lang),Modifier.weight(1f).padding(top=8.dp),style=MaterialTheme.typography.titleMedium)
                            FavoriteButton(entry.id in vm.favorites,{vm.toggleFavorite(entry.id)})
                        }
                    } else {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            if(kind=="programs") EntityImage(entry,56.dp)
                            else Box(Modifier.size(48.dp).background(Color(0xFFF0F3DD),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {Icon(Icons.Outlined.MenuBook,null,tint=Teal)}
                            Spacer(Modifier.weight(1f))
                            FavoriteButton(entry.id in vm.favorites,{vm.toggleFavorite(entry.id)})
                        }
                        if(kind=="programs") Text(entry.raw.optString("company"),color=Teal,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text(entry.title(lang),style=MaterialTheme.typography.titleMedium)
                    }
                    if (entry.subtitle(lang).isNotBlank()) Text(entry.subtitle(lang), color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(t("Подробнее","Толығырақ","Explore"),Modifier.weight(1f),color=Navy,fontWeight=FontWeight.SemiBold);Icon(Icons.Outlined.ArrowForward,null,tint=Navy) }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, navigate: (String) -> Unit, onEntry: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF235F85))) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White,size.height*.9f,Offset(size.width+size.height*.5f-28.dp.toPx(),size.height*.5f),style=Stroke(2.dp.toPx()))
            }
            Row(Modifier.fillMaxWidth().padding(22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                if(vm.profile.photo.isNotBlank()) coil.compose.AsyncImage(vm.profile.photo,t("Фото профиля","Профиль фотосы","Profile photo"),Modifier.size(76.dp).clip(CircleShape).border(2.dp,Color.White,CircleShape),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
                else InitialAvatar(vm.profile.name.take(1).ifBlank {"Y"},Teal,76.dp,true)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text(vm.registeredUsername.takeIf {it.isNotBlank()}?.let {"@$it"} ?: t("Привет!","Сәлем!","Hello!"),color=Color.White,style=MaterialTheme.typography.titleLarge)
                    Text(t("Твой путь.\nТвои возможности.","Сенің жолың.\nСенің мүмкіндіктерің.","Your path.\nYour possibilities."),color=Color.White.copy(alpha=.8f),fontSize=12.sp)
                }
                IconButton({navigate("edit_profile")},Modifier.background(Color(0xFF235F85),RoundedCornerShape(15.dp)).border(1.dp,Color.White,RoundedCornerShape(15.dp))) {Icon(Icons.Outlined.Edit,t("Изменить профиль","Профильді өзгерту","Edit profile"),tint=Color.White)}
            }
        }
        if(vm.accountUser==null) PrimaryButton(t("Войти или зарегистрироваться","Кіру немесе тіркелу","Sign in or register")) {navigate("login")}
        MenuRow(t("Моё резюме","Менің түйіндемем","My resume"),"${vm.resume.completion}% · PDF",Icons.Outlined.Description) {navigate("resume")}
        MenuRow(t("Избранное — ВУЗы","Таңдаулы — ЖОО","Favorites — Universities"),vm.catalog!!.of("universities").count {it.id in vm.favorites}.toString(),Icons.Outlined.School) {navigate("saved_universities")}
        listOf("mentors" to Icons.Outlined.Groups,"jobs" to Icons.Outlined.WorkOutline,"programs" to Icons.Outlined.MenuBook).forEach {(kind,icon) ->
            MenuRow(t("Избранное — ","Таңдаулы — ","Favorites — ")+sectionTitle(kind),vm.catalog!!.entries.count {it.kind==kind && it.id in vm.favorites}.toString(),icon) {navigate("saved_$kind")}
        }
        if(vm.drafts.isNotEmpty()) MenuRow(t("Сохранённые черновики","Сақталған жобалар","Saved drafts"),t("Из предыдущей версии","Алдыңғы нұсқадан","From the previous version"),Icons.Outlined.FolderOpen) {navigate("drafts")}
        HorizontalDivider(Modifier.padding(vertical=4.dp),color=Color.Black.copy(alpha=.25f))
        MenuRow(t("Поддержка","Қолдау","Support"),t("Мы рядом","Біз жаныңдамыз","Here to help"),Icons.Outlined.SupportAgent) {navigate("support")}
        MenuRow(t("О приложении","Қолданба туралы","About"),"Youni4st · "+kz.youni4st.app.BuildConfig.VERSION_NAME,Icons.Outlined.Info) {navigate("about")}
        CloudSyncStatus(vm)
        if(vm.accountUser!=null) MenuRow(t("Способы входа","Кіру тәсілдері","Sign-in methods"),t("Привязать Google или телефон к этому аккаунту","Осы аккаунтқа Google не телефонды байланыстыру","Link Google or phone to this account"),Icons.Outlined.Lock) {navigate("link_account")}
        if(vm.accountUser!=null) TextButton({vm.signOut()}) {Text(t("Выйти из аккаунта","Аккаунттан шығу","Sign out"))}
    }
}
@Composable
fun SavedScreen(vm: AppViewModel, drafts: Boolean, kind: String = "", onBack: () -> Unit, navigate: (String) -> Unit, onEntry: (String) -> Unit) {
    val lang = LocalLanguage.current
    val ids = if (drafts) vm.drafts else vm.favorites
    val entries = vm.catalog!!.entries.filter { it.id in ids && (kind.isBlank() || it.kind == kind) }
    Column {
        PageTitle(if (drafts) t("Мои черновики", "Менің жобаларым", "My drafts") else t("Избранное", "Таңдаулылар", "Favorites")+if(kind.isNotBlank()) " — "+sectionTitle(kind) else "", onBack)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (entries.isEmpty() && (kind.isNotBlank() || ids.none { it.startsWith("section:") })) item { EmptyState(t("Пока ничего не сохранено", "Әзірге ештеңе сақталмаған", "Nothing saved yet")) }
            items(ids.filter { kind.isBlank() && it.startsWith("section:") }) { id ->
                val kind = id.substringAfter(':')
                MenuRow(sectionTitle(kind), t("Раздел", "Бөлім", "Section"), Icons.Outlined.BookmarkBorder) { navigate(kind) }
            }
            items(entries, key = { it.id }) { entry ->
                SoftCard(Modifier.fillMaxWidth(), { onEntry(entry.id) }) {
                    Text(entry.title(lang), style = MaterialTheme.typography.titleMedium)
                    Text(entry.subtitle(lang), style = MaterialTheme.typography.bodyMedium, color = Muted)
                    if (drafts) Badge(t("Черновик · не отправлен", "Жоба · жіберілмеген", "Draft · not sent"), Navy)
                }
            }
        }
    }
}
