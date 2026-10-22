@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package kz.youni4st.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.*

@Composable
fun HomeScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=10.dp).clickable {navigate("profile")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            ProfileAvatar(vm.profile.photo,vm.profile.name,52.dp)
            val firstName=vm.profile.name.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank {vm.accountUser?.displayName?.substringBefore(" ").orEmpty()}.ifBlank {vm.registeredUsername.takeIf {it.isNotBlank()}?.let {"@$it"}.orEmpty()}
            Text(if(firstName.isBlank()) t("Привет!","Сәлем!","Hello!") else t("Привет, $firstName!","Сәлем, $firstName!","Hello, $firstName!"),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
        }
        Surface(onClick = { navigate("chat") }, shape = RoundedCornerShape(26.dp), color = Navy) {
            Row(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Navy, Color(0xFF235F85)))).padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
    Modifier
        .size(48.dp)
        .background(
            Color.White.copy(alpha = .14f),
            RoundedCornerShape(16.dp)
        )
        .border(
            1.5.dp,
            Color.White,
            RoundedCornerShape(16.dp)
        ),
    contentAlignment = Alignment.Center
) {
    Icon(
        Icons.Rounded.AutoAwesome,
        null,
        tint = Color.White
    )
}
                Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(t("Спроси AI-SOKRATUS", "AI-SOKRATUS сұраңыз", "Ask AI‑SOKRATUS"), color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    Text(t("Поможем разобраться с вузами,\nгрантами и будущей профессией", "ЖОО, грант және мамандық таңдауға көмектесеміз", "Make sense of universities,\ngrants and your next step"), color=Color(0xFFD5E4EE), fontSize=12.sp, lineHeight=18.sp)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,tint=Color.White,modifier=Modifier.size(20.dp))
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf(Triple("grants",t("Гранты","Гранттар","Grants"),Icons.Outlined.School),Triple("ielts","IELTS",Icons.Outlined.Language),Triple("news",t("Новости","Жаңалықтар","News"),Icons.Outlined.Newspaper)).forEachIndexed { i, (route,title,icon) ->
                Surface(onClick={navigate(route)},shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,Color.Black)) {
                    Row(Modifier.padding(horizontal=19.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Icon(icon,null,Modifier.size(21.dp),tint=Navy); Text(title,fontWeight=FontWeight.SemiBold,color=Color.Black) }
                }
            }
        }
        val routes=listOf("programs","universities","jobs","mentors")
        val titles=listOf(t("Образовательные программы","Білім беру бағдарламалары","Education programs"),t("Выбрать ВУЗ","ЖОО таңдау","Find a university"),t("Работа и стажировки","Жұмыс пен тағылымдама","Jobs and internships"),t("Найди своего наставника","Тәлімгеріңді тап","Meet your mentor"))
        val subtitles=listOf(t("Навыки, которые открывают двери","Мүмкіндік ашатын дағдылар","Skills that open doors"),t("Место для твоего будущего","Болашағың үшін орын","A place for your future"),t("От интереса к первому опыту","Қызығушылықтан тәжірибеге","Turn interest into experience"),t("Опыт людей, которым есть что рассказать","Тәжірибелі мамандардың кеңесі","Guidance from experience"))
        val colors=listOf(Color(0xFF76A995),Color(0xFFF0C7A9),Color(0xFFD9B65D),Color(0xFF7962A6))
        routes.forEachIndexed { i,route ->
            Surface(onClick={navigate(route)},color=colors[i],shape=RoundedCornerShape(26.dp),border=BorderStroke(1.dp,Color.Black)) {
                Box(Modifier.fillMaxWidth().heightIn(min=168.dp)) {
                    SectionArt(i,Modifier.align(Alignment.BottomEnd).size(155.dp,145.dp))
                    Column(Modifier.fillMaxWidth(.60f).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("0${i+1}",fontSize=10.sp,color=Muted,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                        Text(titles[i],style=MaterialTheme.typography.titleLarge,fontSize=22.sp,lineHeight=23.sp)
                        Text(subtitles[i], fontSize=14.sp, lineHeight=17.sp, color=Color.Black)
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(21.dp),tint=Navy)
                    }
                    FavoriteButton("section:$route" in vm.favorites,{vm.toggleFavorite("section:$route")},Modifier.align(Alignment.TopEnd).padding(3.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
@Composable
fun UniversityScreen(vm: AppViewModel, onSaved: () -> Unit, onEntry: (String) -> Unit) {
    val lang = LocalLanguage.current
    val entries = vm.catalog!!.of("universities")
    var dormitory by rememberSaveable { mutableStateOf(false) }
    var military by rememberSaveable { mutableStateOf(false) }
    var city by rememberSaveable { mutableStateOf("") }
    var field by rememberSaveable { mutableStateOf("") }
    var nearbyCity by rememberSaveable { mutableStateOf("") }
    var specialty by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val directory = vm.catalog!!.universities
    val filtered = remember(entries,dormitory,military,city,nearbyCity,field,specialty,search) {
        val query=UniversityQuery(city,nearbyCity,field,specialty,dormitory,military)
        entries.filter { query.matches(it,"",search) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(
    Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(Color(0xFF235F85))
) {
                Canvas(Modifier.matchParentSize()) {
    drawCircle(
        Color.White,
        size.width * .62f,
        Offset(size.width * 1.25f, size.height * .6f),
        style = Stroke(2.dp.toPx())
    )
}
Row(
    Modifier.padding(23.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Column(
        Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            t("Поиск вузов", "ЖОО іздеу", "Find a university"),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            t(
                "Твоё будущее начинается\nс правильного выбора.",
                "Болашағың дұрыс\nтаңдаудан басталады.",
                "Your future starts\nwith the right choice."
            ),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = .8f)
        )
    }
    CountOrb(
        vm.catalog!!.of("universities").count { it.id in vm.favorites },
        t("Мой список", "Менің тізімім", "My list"),
        dark = true,
        onClick = onSaved
    )
}
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(t("Поиск вуза","ЖОО іздеу","University search"),style=MaterialTheme.typography.titleMedium)
                OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(t("Название вуза","ЖОО атауы","Institution name"))},leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(17.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Black),keyboardOptions=KeyboardOptions(capitalization=androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(t("Город", "Қала", "City"), style = MaterialTheme.typography.titleMedium)
                DirectoryPicker(t("Город","Қала","City"),city,listOf("" to t("Все города","Барлық қалалар","All cities"))+directory.primaryCities.map {it.id to it.label(lang)}) { city=it;nearbyCity="" }
            }
        }
        if(city.isNotBlank()) item {
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                val region=directory.regions.find {it.id==directory.regionFor(city)}?.label(lang).orEmpty()
                Text(t("Город и область","Қала және облыс","City and region"),style=MaterialTheme.typography.titleMedium)
                Text(region,color=Muted,style=MaterialTheme.typography.bodyMedium)
                DirectoryPicker(t("Города региона","Өңір қалалары","Cities in the region"),nearbyCity.ifBlank {city},directory.nearbyCities(city).map {it.id to it.label(lang)}) {nearbyCity=it}
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(t("Специальность", "Мамандығы", "Specialization"), style = MaterialTheme.typography.titleMedium)
                DirectoryPicker(t("Основное направление","Негізгі бағыт","Main field"),field,listOf("" to t("Все направления","Барлық бағыттар","All fields"))+directory.fields.map {it.id to it.label(lang)}) {field=it;specialty=""}
            }
        }
        if(field.isNotBlank()) item {
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(t("Направление подготовки","Даярлау бағыты","Study program"),style=MaterialTheme.typography.titleMedium)
                DirectoryPicker(t("Специальности и уровни","Мамандықтар мен деңгейлер","Specializations and levels"),specialty,listOf("" to t("Все специальности направления","Бағыттың барлық мамандықтары","All specializations"))+directory.children(field).map {it.id to it.fullLabel(lang)}) {specialty=it}
                if(field=="medicine") Text(t("Резидентура — обучение после базового медицинского образования.","Резидентура — базалық медициналық білімнен кейінгі оқу.","Residency requires prior medical education."),color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Column {
                FacilityFilter(t("Общежитие","Жатақхана","Dormitory"),dormitory) {dormitory=it}
                FacilityFilter(t("Военная кафедра","Әскери кафедра","Military department"),military) {military=it}
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Подобрано для тебя", "Саған ұсынылады", "Selected for you"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Text(filtered.size.toString(), color = Muted)
            }
        }
        if (filtered.isEmpty()) item { EmptyState(t("Подходящих вузов пока нет. Попробуй изменить фильтры.", "Сәйкес ЖОО табылмады. Сүзгілерді өзгерт.", "No matches. Try changing your filters."), t("Сбросить", "Тазарту", "Reset")) { dormitory=false; military=false; city = ""; field = ""; nearbyCity="";specialty="";search="" } }
        items(filtered, key = { it.id }) { entry -> UniversityCard(entry, vm, onEntry) }
        item { Text(t("ЕПВО · 16.10.2026. Вузы и организации послевузовского образования. Условия поступления уточняйте по выбранной программе.", "ЕПВО · 16.10.2026. Шарттарды бағдарлама бойынша нақтылаңыз.", "EPVO · 16 Oct 2026. Higher and postgraduate education institutions. Confirm program requirements."), style = MaterialTheme.typography.bodyMedium, color = Muted) }
    }
}

@Composable
fun UniversityCard(entry: Entry, vm: AppViewModel, onEntry: (String) -> Unit) {
    val lang = LocalLanguage.current
    SoftCard(Modifier.fillMaxWidth(), { onEntry(entry.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntityImage(entry, 58.dp)
            Column(Modifier.weight(1f)) {
                Text(entry.title(lang), style = MaterialTheme.typography.titleMedium)
                Text(entry.subtitle(lang), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            FavoriteButton(entry.id in vm.favorites, { vm.toggleFavorite(entry.id) })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if(!entry.raw.isNull("min_score")) Badge("${entry.raw.optInt("min_score")}+ " + t("ЕНТ", "ҰБТ", "UNT"), Navy, Color(0xFFF3F4FA))
            else Badge(t("ЕНТ: по программе","ҰБТ: бағдарлама бойынша","UNT: by program"))
            if(entry.raw.optBoolean("has_dormitory")) Badge(t("Общежитие","Жатақхана","Dormitory"))
            if(entry.raw.optBoolean("has_military_dept")) Badge(t("Военная кафедра","Әскери кафедра","Military department"))
        }
        val programs=entry.raw.optJSONArray("specialties")
        val names=(0 until (programs?.length() ?: 0)).map {programs!!.getJSONObject(it).optJSONObject("name")?.let {n->n.optString(lang.name).ifBlank {n.optString("RU")}}.orEmpty()}.filter {it.isNotBlank()}.distinct()
        if(names.isNotEmpty()) {
            Text(t("Специальности","Мамандықтар","Specializations"),fontWeight=FontWeight.SemiBold)
            names.take(3).forEach {Text("• $it",fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}
            UniversityPrograms(entry)
        } else Text(t("Специальности уточняются","Мамандықтар нақтыланады","Programs not confirmed"),color=Muted)

    }
}

@Composable
fun JobsScreen(vm: AppViewModel, navigate: (String) -> Unit, onEntry: (String) -> Unit) {
    val lang = LocalLanguage.current
    val directory=vm.catalog!!.universities
    var today by remember {mutableStateOf(java.time.LocalDate.now())}
    LaunchedEffect(Unit) {while(true){today=java.time.LocalDate.now();kotlinx.coroutines.delay(60_000)}}
    val allEntries = vm.catalog!!.of("jobs")
    val entries=allEntries.filter {jobIsActive(it,today)}
    var company by rememberSaveable {mutableStateOf("")}
    var city by rememberSaveable { mutableStateOf("") }
    var nearbyCity by rememberSaveable {mutableStateOf("")}
    var category by rememberSaveable { mutableStateOf("") }
    var employment by rememberSaveable {mutableStateOf(listOf<String>())}
    val query=JobQuery(company,city,nearbyCity,category,employment.toSet())
    val filtered = entries.filter {query.matches(it,today)}
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(
    Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(Color(0xFF235F85))
) {
    Canvas(Modifier.matchParentSize()) {
        drawCircle(
            Color.White, size.width * .62f,
            Offset(size.width * 1.25f, size.height * .6f),
            style = Stroke(2.dp.toPx())
        )
    }

    Row(
        Modifier.fillMaxWidth().padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { navigate("resume") },
                color = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    t("Моё резюме", "Түйіндемем", "My resume"),
                    Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    color = Color(0xFF235F85),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                t("Поиск работы", "Жұмыс іздеу", "Find your next step"),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                t(
                    "Ваш проводник\nк вершинам мастерства.",
                    "Шеберлік шыңдарына\nжол көрсетушіңіз.",
                    "Your guide to the peaks of mastery."
                ),
                color = Color.White.copy(alpha = .8f),
                fontSize = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { vm.resume.completion / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .22f),
                    strokeWidth = 5.dp
                )
                Text(
                    "${vm.resume.completion}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                t("Резюме заполнено", "Толтырылғаны", "Resume complete"),
                color = Color.White.copy(alpha = .8f),
                fontSize = 10.sp
            )
            Text(
                "${entries.size} " + t("вакансий", "бос орын", "openings"),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(t("Поиск компании","Компанияны іздеу","Company search"),style=MaterialTheme.typography.titleMedium)
                OutlinedTextField(company,{company=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(t("Название компании","Компания атауы","Company name"))},leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(17.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Black))
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(t("Город","Қала","City"),style=MaterialTheme.typography.titleMedium)
                DirectoryPicker(t("Город","Қала","City"),city,listOf("" to t("Все города","Барлық қалалар","All cities"))+directory.primaryCities.map {it.id to it.label(lang)}) {city=it;nearbyCity=""}
            }
        }
        if(city.isNotBlank()) item {
            val regionKey=directory.regionFor(city)
            val region=directory.regions.firstOrNull {it.id==regionKey}
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(t("Город и область","Қала және облыс","City and region"),style=MaterialTheme.typography.titleMedium)
                Text(region?.label(lang).orEmpty(),color=Muted)
                val cities=directory.nearbyCities(city).map {it.id to it.label(lang)}
                val options=cities+listOf("region:$regionKey" to t("Вся область: ","Бүкіл облыс: ","Whole region: ")+region?.label(lang).orEmpty())
                DirectoryPicker(t("Города региона","Өңір қалалары","Cities in the region"),nearbyCity.ifBlank {city},options){nearbyCity=it}
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(t("Направление","Бағыт","Field"),style=MaterialTheme.typography.titleMedium)
                DirectoryPicker(t("Направление","Бағыт","Field"),category,listOf("" to t("Все направления","Барлық бағыттар","All fields"))+allEntries.distinctBy {it.raw.optString("category_key")}.map {it.raw.optString("category_key") to it.text("category",lang)}){category=it}
            }
        }
        item {
            EmploymentPicker(employment) { employment = it }
        }
        item {Text(t("Найдено: ","Табылды: ","Results: ")+filtered.size,style=MaterialTheme.typography.titleMedium)}
        if (filtered.isEmpty()) item { EmptyState(t("Вакансий по этим фильтрам нет", "Бос орындар табылмады", "No matching openings"), t("Сбросить", "Тазарту", "Reset")) {company="";city="";nearbyCity="";category="";employment=emptyList()} }
        items(filtered, key = { it.id }) { JobCard(it, vm, onEntry) }
        item { Text(t("Enbek · проверено 17.10.2026. Отклик — на портале. Истёкшие объявления скрываются.", "Enbek · 17.10.2026 тексерілді. Өтінім порталда беріледі. Мерзімі өткен хабарландырулар жасырылған.", "Enbek · checked 17 Oct 2026. Apply on the portal. Expired listings are hidden."), color = Muted, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun JobCard(entry: Entry, vm: AppViewModel, onEntry: (String) -> Unit) {
    val lang = LocalLanguage.current
    val company = entry.text("company",lang)
    val initials = company.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    val avatarColor = listOf(Teal, Color(0xFFE8A314), Color(0xFF40A875), Navy, Color(0xFF956DAF))[(entry.id.hashCode() and Int.MAX_VALUE) % 5]
    SoftCard(Modifier.fillMaxWidth(), { onEntry(entry.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntityImage(entry, 58.dp)
            Column(Modifier.weight(1f)) {
                Text(entry.title(lang), style = MaterialTheme.typography.titleMedium)
                Text(company, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Badge(entry.text("city",lang), icon = Icons.Outlined.LocationOn)
            Badge(entry.text("type",lang), icon = Icons.Outlined.Schedule)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.text("salary",lang), Modifier.weight(1f), color = Color(0xFF4977A9), style = MaterialTheme.typography.titleMedium)
            FavoriteButton(entry.id in vm.favorites, { vm.toggleFavorite(entry.id) })
        }
    }
}

@Composable
fun MentorsScreen(vm: AppViewModel, onBack: () -> Unit, onSaved: () -> Unit, onEntry: (String) -> Unit) {
    val lang=LocalLanguage.current
    var field by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    val entries=vm.catalog!!.of("mentors")
    val filtered=entries.filter { (field.isBlank() || it.text("role",Language.RU)==field) && (price.isBlank() || it.text("price",Language.RU).startsWith("Бесплатно")==(price=="free")) }
    Column {
        PageTitle(t("Наставники","Тәлімгерлер","Mentors"),onBack)
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                Box(
    Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(Color(0xFF235F85))
) {
    Canvas(Modifier.matchParentSize()) {
        drawCircle(
            Color.White, size.width * .62f,
            Offset(size.width * 1.25f, size.height * .6f),
            style = Stroke(2.dp.toPx())
        )
    }

    Row(
        Modifier.fillMaxWidth().padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                t(
                    "Поиск опытных наставников",
                    "Тәжірибелі тәлімгерлерді табу",
                    "Find experienced mentors"
                ),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                t(
                    "Карьера начинается с правильного проводника",
                    "Мансап дұрыс тәлімгерден басталады",
                    "A career begins with the right mentor"
                ),
                color = Color.White.copy(alpha = .8f),
                fontSize = 12.sp
            )
        }
        CountOrb(
            entries.count { it.id in vm.favorites },
            t("Избранные", "Таңдаулылар", "Saved"),
            dark = true,
            onClick = onSaved
        )
    }
}
            }
            item {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    SelectField(field,listOf("" to t("Все направления","Барлық бағыттар","All fields"))+entries.distinctBy { it.text("role",Language.RU) }.map { it.text("role",Language.RU) to it.text("role",lang) },{field=it},Modifier.weight(1.3f))
                    SelectField(price,listOf("" to t("Любая цена","Кез келген баға","Any price"),"free" to t("Бесплатно","Тегін","Free"),"paid" to t("Платно","Ақылы","Paid")),{price=it},Modifier.weight(1f))
                }
            }
            items(filtered,key={it.id}) { entry -> MentorCard(entry,vm,onEntry) }
            if(filtered.isEmpty()) item { EmptyState(t("Наставники не найдены","Тәлімгерлер табылмады","No mentors found")) }
        }
    }
}

@Composable
fun MentorCard(entry: Entry, vm: AppViewModel, onEntry: (String)->Unit) {
    val lang=LocalLanguage.current
    SoftCard(Modifier.fillMaxWidth(),{onEntry(entry.id)}) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            EntityImage(entry,72.dp)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(entry.title(lang),style=MaterialTheme.typography.titleMedium)
                Text(entry.subtitle(lang),color=Muted,style=MaterialTheme.typography.bodyMedium)
            }
            FavoriteButton(entry.id in vm.favorites,{vm.toggleFavorite(entry.id)})
        }
        Text(entry.description(lang),color=Muted,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
        HorizontalDivider(color=Color(0xFFEEF0EB))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Badge(entry.text("price",lang),Navy,Color(0xFFF0F5E6),Icons.Outlined.Payments)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,tint=Teal)
        }
    }
}
fun mentorColor(entry: Entry): Color = runCatching { Color(entry.raw.optString("avatar_color").removePrefix("0x").toLong(16)) }.getOrDefault(Navy)

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    unfocusedBorderColor = Color.Transparent, focusedBorderColor = Teal, errorContainerColor = Color.White)

@Composable
private fun FacilityFilter(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value=checked,role=androidx.compose.ui.semantics.Role.Checkbox,onValueChange=onChange).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Checkbox(checked,onCheckedChange=null,colors=CheckboxDefaults.colors(checkedColor=Color.White,uncheckedColor=Color.Black,checkmarkColor=Color.Black))
        Text(label,Modifier.padding(start=10.dp),color=Color.Black)
    }
}

/** A compact multi-selection field; employment filtering continues to use OR. */
@Composable
private fun EmploymentPicker(selected:List<String>,onChange:(List<String>)->Unit) {
    var open by rememberSaveable {mutableStateOf(false)}
    val title=t("Тип занятости","Жұмыспен қамту түрі","Employment type")
    val options=listOf(
        "internship" to t("Стажировка","Тағылымдама","Internship"),
        "full_time" to t("Полный рабочий день","Толық жұмыс күні","Full-time"),
        "part_time" to t("Неполный рабочий день","Толық емес жұмыс күні","Part-time"),
        "remote" to t("Удалённо","Қашықтан","Remote")
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick={open=true},
            modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),
            shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,Color.Black)
        ) {
            Text(if(selected.isEmpty())t("Все варианты","Барлық нұсқалар","All types")
                else options.filter {it.first in selected}.joinToString(", "){it.second},
                Modifier.weight(1f),color=Ink)
            Icon(Icons.Outlined.ExpandMore,t("Открыть список","Тізімді ашу","Open list"),tint=Ink)
        }
    }
    if(open) AlertDialog(
        onDismissRequest={open=false},containerColor=CanvasColor,shape=RoundedCornerShape(24.dp),
        title={Text(title)},
        text={
            Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(t("Можно выбрать несколько вариантов","Бірнеше нұсқаны таңдауға болады","Select one or more options"),color=Muted)
                options.forEach {(key,label)->
                    FacilityFilter(label,key in selected) {checked->
                        onChange(if(checked)(selected+key).distinct() else selected-key)
                    }
                }
            }
        },
        confirmButton={TextButton({open=false}){Text(t("Готово","Дайын","Done"))}},
        dismissButton={TextButton({onChange(emptyList())}){Text(t("Сбросить","Тазарту","Reset"))}}
    )
}
