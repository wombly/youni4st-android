package kz.youni4st.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.*
import org.json.JSONObject

@Composable
fun ResumeScreen(vm:AppViewModel,onBack:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val language=LocalLanguage.current
    var draft by rememberSaveable(stateSaver=Saver<Resume,String>(save={it.toJson().toString()},restore={Resume.fromJson(JSONObject(it))})) {mutableStateOf(vm.resume)}
    var discard by rememberSaveable {mutableStateOf(false)}
    var photoBusy by remember {mutableStateOf(false)}
    var exportBusy by remember {mutableStateOf(false)}
    var exportChecked by rememberSaveable {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    var errors by rememberSaveable {mutableStateOf(false)}
    var editing by rememberSaveable {mutableStateOf("")}
    var index by rememberSaveable {mutableIntStateOf(-1)}
    var closeAfterExport by rememberSaveable {mutableStateOf(false)}
    var pdfSnapshot by remember {mutableStateOf<Resume?>(null)}
    val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if(uri!=null) scope.launch {
            exportBusy=true
            try {
                val snapshot=pdfSnapshot ?: draft
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use {ResumePdf.write(context,snapshot,language,it)} ?: error("No output") }
                status=when(language){Language.RU->"Резюме и PDF сохранены";Language.KZ->"Түйіндеме мен PDF сақталды";Language.EN->"Resume and PDF saved"}
            } catch(e:CancellationException){throw e} catch(_:Exception){status=when(language){Language.RU->"Резюме сохранено. Не удалось записать PDF — попробуй ещё раз.";Language.KZ->"Түйіндеме сақталды. PDF жазылмады.";Language.EN->"Resume saved. Could not write PDF; please retry."}}
            finally {exportBusy=false;if(closeAfterExport){closeAfterExport=false;onBack()}}
        } else if(closeAfterExport) {closeAfterExport=false;onBack()}
    }
    val savedText=t("Резюме сохранено","Түйіндеме сақталды","Resume saved")
    fun save(exit:Boolean=false) {
        errors=!PersonalValidation.phone(draft.phone) || !PersonalValidation.birthday(draft.birthday)
        if(errors)return
        vm.saveResume(draft);status=savedText
        if(exportChecked) {closeAfterExport=exit;pdfSnapshot=draft;pdf.launch("Youni4st-Resume.pdf")}
        else if(exit) onBack()
    }
    fun back(){if(photoBusy || exportBusy)return;if(draft!=vm.resume)discard=true else onBack()}
    BackHandler {back()}
    Column(Modifier.dismissKeyboardOnBackgroundTap().fillMaxSize().background(FormBackground).imePadding()) {
        Surface(color=Color.White) {PageTitle(t("Резюме","Түйіндеме","Resume"),::back) {TextButton({back()},enabled=!photoBusy && !exportBusy) {Text(t("Закрыть","Жабу","Close"))}}}
        Column(Modifier.dismissKeyboardOnBackgroundTap().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            PersonalPhoto(draft.photo,{draft=draft.copy(photo=it)},{photoBusy=it})
            SoftCard {
                Text(t("Основная информация","Негізгі ақпарат","Basic information"),style=MaterialTheme.typography.titleLarge)
                PersonalField(t("ФИО","Аты-жөні","Full name"),draft.name,{draft=draft.copy(name=it)},Icons.Outlined.PersonOutline)
                PersonalField(t("Желаемая должность","Қалаған лауазым","Desired position"),draft.position,{draft=draft.copy(position=it)},Icons.Outlined.WorkOutline)
                BirthdayField(draft.birthday){draft=draft.copy(birthday=it)}
                ChoiceField(t("Пол","Жынысы","Gender"),draft.gender,listOf(t("Мужской","Ер","Male"),t("Женский","Әйел","Female"),t("Не указывать","Көрсетпеу","Prefer not to say")),{draft=draft.copy(gender=it)},Icons.Outlined.PeopleOutline)
                PersonalField(t("Номер телефона","Телефон нөмірі","Phone number"),draft.phone,{draft=draft.copy(phone=it);errors=false},Icons.Outlined.PhoneAndroid,phone=true,error=errors && !PersonalValidation.phone(draft.phone))
                PersonalField("E-mail",draft.email,{draft=draft.copy(email=it.trim())},Icons.Outlined.Email,KeyboardType.Email)
                PersonalField(t("Город проживания","Тұратын қала","City"),draft.city,{draft=draft.copy(city=it)},Icons.Outlined.LocationCity)
                PersonalField(t("Желаемая зарплата, ₸","Қалаған жалақы, ₸","Expected salary, ₸"),draft.salary,{draft=draft.copy(salary=it)},Icons.Outlined.Payments,KeyboardType.Number,salary=true)
                ResumeEmployment(draft.employmentTypes){draft=draft.copy(employmentTypes=it)}
            }
            // Legacy free text remains editable; updating never discards an older resume.
            if(draft.experience.isNotBlank()) SoftCard {PersonalField(t("Ранее сохранённый опыт","Бұрын сақталған тәжірибе","Previously saved experience"),draft.experience,{draft=draft.copy(experience=it)},multiline=true)}
            SoftCard {
                Text(t("Общий опыт работы","Жалпы жұмыс өтілі","Total work experience"),style=MaterialTheme.typography.titleMedium)
                Text(ResumeDates.duration(ResumeDates.experienceMonths(draft.work),language),style=MaterialTheme.typography.titleLarge)
                Text(t("Считается по указанным месяцам. Параллельные работы не суммируются дважды.","Көрсетілген айлар бойынша есептеледі. Қатар атқарылған жұмыстар екі рет қосылмайды.","Calculated from calendar months. Overlapping jobs are counted once."),color=Muted)
            }
            RecordSection(t("Опыт работы","Жұмыс тәжірибесі","Work experience"),draft.work,{index=-1;editing="work"},{index=it;editing="work"},{draft=draft.copy(work=draft.work.filterIndexed {i,_->i!=it})},t("Работаю сейчас","Қазір жұмыс істеймін","Currently employed"))
            if(draft.education.isNotBlank()) SoftCard {PersonalField(t("Ранее сохранённое образование","Бұрын сақталған білім","Previously saved education"),draft.education,{draft=draft.copy(education=it)},multiline=true)}
            RecordSection(t("Название университета","Университет атауы","Education"),draft.studies,{index=-1;editing="studies"},{index=it;editing="studies"},{draft=draft.copy(studies=draft.studies.filterIndexed {i,_->i!=it})})
            RecordSection(t("Знание языков","Тілдерді білу","Languages"),draft.languages,{index=-1;editing="languages"},{index=it;editing="languages"},{draft=draft.copy(languages=draft.languages.filterIndexed {i,_->i!=it})})
            RecordSection(t("Интересы","Қызығушылықтар","Interests"),draft.interests.map {ResumeRecord(it)},{index=-1;editing="interests"},{index=it;editing="interests"},{draft=draft.copy(interests=draft.interests.filterIndexed {i,_->i!=it})})
            SkillsEditor(draft.allSkills){draft=draft.copy(skills="",skillItems=it)}
            SoftCard {Text(t("Обо мне","Өзім туралы","About me"),style=MaterialTheme.typography.titleLarge);PersonalField(t("Расскажи о себе","Өзің туралы айт","Tell us about yourself"),draft.about,{draft=draft.copy(about=it)},multiline=true)}
            Row(Modifier.fillMaxWidth().clickable {exportChecked=!exportChecked},verticalAlignment=Alignment.CenterVertically) {
                Checkbox(exportChecked,{exportChecked=it});Text(t("Сохранить и скачать в PDF","Сақтау және PDF жүктеу","Save and download PDF"),Modifier.weight(1f))
            }
            if(errors) Text(t("Проверь дату рождения и введи телефон полностью: +7 и 10 цифр.","Туған күнді және телефонды тексер: +7 және 10 сан.","Check your date of birth and enter a complete phone number: +7 and 10 digits."),color=MaterialTheme.colorScheme.error)
            PrimaryButton(if(exportBusy)t("Создаём PDF…","PDF жасалуда…","Creating PDF…") else t("Сохранить резюме","Түйіндемені сақтау","Save resume"),enabled=!photoBusy && !exportBusy) {save()}
            if(status.isNotBlank()) Text(status,color=Teal)
            Text(t("Резюме сохраняется на этом телефоне.","Түйіндеме осы телефонда сақталады.","Your resume is saved on this phone."),color=Muted,style=MaterialTheme.typography.bodySmall)
        }
    }
    if(editing.isNotBlank()) {
        val source=when(editing){"work"->draft.work;"studies"->draft.studies;"languages"->draft.languages;else->draft.interests.map {ResumeRecord(it)}}
        key(editing,index) { RecordEditor(vm,editing,source.getOrNull(index) ?: ResumeRecord(),index>=0,{editing=""}) {record ->
            val updated=source.toMutableList().apply {if(index in indices)set(index,record) else add(record)}
            draft=when(editing){"work"->draft.copy(work=updated);"studies"->draft.copy(studies=updated);"languages"->draft.copy(languages=updated);else->draft.copy(interests=updated.map {it.title})}
            editing=""
        } }
    }
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text(t("Сохранить изменения?","Өзгерістерді сақтау керек пе?","Save changes?"))},
        confirmButton={TextButton({discard=false;save(exit=true)},enabled=!photoBusy && !exportBusy){Text(t("Сохранить","Сақтау","Save"))}},
        dismissButton={TextButton({discard=false;onBack()}){Text(t("Закрыть без сохранения","Сақтамай жабу","Close without saving"))}})
}

@Composable
private fun RecordSection(title:String,records:List<ResumeRecord>,add:()->Unit,edit:(Int)->Unit,remove:(Int)->Unit,flagLabel:String=t("В настоящее время обучаюсь","Қазір оқып жатырмын","Currently studying")) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(add) {Icon(Icons.Outlined.AddCircleOutline,t("Добавить","Қосу","Add"),tint=Navy)}
        }
        if(records.isEmpty()) Text(t("Добавь первую запись","Алғашқы жазбаны қос","Add your first entry"),color=Muted,style=MaterialTheme.typography.bodyMedium)
        records.forEachIndexed {i,record ->
            Row(Modifier.fillMaxWidth().border(1.dp,Color.Black,androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.dismissKeyboardOnBackgroundTap().weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(record.title,style=MaterialTheme.typography.titleMedium)
                    listOf(if(record.subtitle.substringBefore(" ") in listOf("A1","A2","B1","B2","C1","C2","native"))ResumeReference.level(record.subtitle,LocalLanguage.current) else record.subtitle,ResumeDates.period(record,LocalLanguage.current),record.details).filter {it.isNotBlank()}.forEach {Text(it,color=Muted,style=MaterialTheme.typography.bodyMedium)}
                    if(record.ongoing) Text(flagLabel,color=Teal,style=MaterialTheme.typography.bodySmall)
                }
                IconButton({edit(i)}) {Icon(Icons.Outlined.Edit,t("Изменить запись","Жазбаны өзгерту","Edit entry"),Modifier.size(21.dp))}
                IconButton({remove(i)}) {Icon(Icons.Outlined.DeleteOutline,t("Удалить запись","Жазбаны жою","Delete entry"),Modifier.size(21.dp))}
            }
            if(i<records.lastIndex) HorizontalDivider(color=FormBackground)
        }
    }
}

@Composable
private fun RecordEditor(vm:AppViewModel,kind:String,initial:ResumeRecord,existing:Boolean,dismiss:()->Unit,save:(ResumeRecord)->Unit) {
    val context=LocalContext.current
    val lang=LocalLanguage.current
    val refs=remember(context){ResumeReference(context)}
    var record by rememberSaveable(stateSaver=Saver<ResumeRecord,String>(save={it.toJson().toString()},restore={ResumeRecord.fromJson(JSONObject(it))})) {
        mutableStateOf(if(kind=="work" && initial.startMonth.isBlank() && initial.ongoing)initial.copy(student=true,ongoing=false) else initial)
    }
    var invalid by rememberSaveable {mutableStateOf(false)}
    val title=when(kind){"work"->t("Опыт работы","Жұмыс тәжірибесі","Work experience");"studies"->t("Образование","Білім","Education");"languages"->t("Знание языков","Тілдерді білу","Languages");else->t("Интересы","Қызығушылықтар","Interests")}
    PersonalSheet(title,dismiss) {
        when(kind) {
            "work"->{
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(record.student,{record=record.copy(student=it)});Text(t("Студент","Студент","Student"))}
                ResumeChoice(t("Должность","Лауазым","Position"),record.title,refs.professions(lang),{record=record.copy(title=it);invalid=false})
                PersonalField(t("Компания","Компания","Company"),record.subtitle,{record=record.copy(subtitle=it)},Icons.Outlined.Business)
                ResumeChoice(t("Деятельность компании","Компания қызметі","Company industry"),refs.industry(record.industry,lang),refs.industries.map {it.localized("name",lang)},{v->
                    record=record.copy(industry=refs.industries.firstOrNull {it.localized("name",lang)==v}?.optString("id") ?: v,specialization="")
                })
                if(record.industry.isNotBlank())ResumeChoice(t("Поднаправление","Ішкі бағыт","Subsector"),record.specialization,refs.children(record.industry,lang),{record=record.copy(specialization=it)})
                MonthField(t("Начало работы","Жұмыс басталған ай","Start month"),record.startMonth){record=record.copy(startMonth=it);invalid=false}
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(record.ongoing,{record=record.copy(ongoing=it);invalid=false});Text(t("Работаю по настоящее время","Қазіргі уақытқа дейін жұмыс істеймін","I currently work here"))}
                if(!record.ongoing)MonthField(t("Завершение работы","Жұмыс аяқталған ай","End month"),record.endMonth){record=record.copy(endMonth=it);invalid=false}
                if(record.period.isNotBlank() && record.startMonth.isBlank()) {
                    Text(t("Ранее сохранённый период: ","Бұрын сақталған кезең: ","Previously saved period: ")+record.period,color=Muted)
                    Text(t("Выберите месяцы для расчёта стажа. Старый текст останется до замены.","Өтілді есептеу үшін айларды таңдаңыз. Ескі мәтін ауыстырылғанша сақталады.","Choose months to calculate experience. The old text is preserved until replaced."),color=Muted)
                }
                PersonalField(t("Обязанности и достижения","Міндеттер мен жетістіктер","Responsibilities and achievements"),record.details,{record=record.copy(details=it)},Icons.Outlined.Description,multiline=true)
            }
            "studies"->{
                ResumeChoice(t("ВУЗ","ЖОО","University"),record.title,vm.catalog!!.of("universities").map {it.title(lang)}.distinct().sorted(),{record=record.copy(title=it);invalid=false})
                ResumeChoice(t("Специальность / профессия","Мамандық / кәсіп","Field of study / profession"),record.subtitle,refs.professions(lang),{record=record.copy(subtitle=it)})
                ResumeChoice(t("Степень","Дәреже","Degree"),record.details,listOf(
                    t("Бакалавр","Бакалавр","Bachelor"),t("Магистр","Магистр","Master"),"PhD",t("Доктор наук","Ғылым докторы","Doctor of Sciences"),
                    t("Специалист","Маман","Specialist"),t("Резидентура","Резидентура","Medical residency"),t("Колледж / профессиональное образование","Колледж / кәсіптік білім","College / vocational qualification")
                ),{record=record.copy(details=it)})
                PersonalField(t("Год окончания","Бітірген жылы","Graduation year"),record.period,{record=record.copy(period=it.filter(Char::isDigit).take(4))},Icons.Outlined.CalendarMonth,KeyboardType.Number)
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(record.ongoing,{record=record.copy(ongoing=it)});Text(t("В настоящее время обучаюсь","Қазір оқып жатырмын","Currently studying"))}
            }
            "languages"->{
                ResumeChoice(t("Язык","Тіл","Language"),record.title,listOf(
                    t("Казахский","Қазақ тілі","Kazakh"),t("Русский","Орыс тілі","Russian"),t("Английский","Ағылшын тілі","English"),t("Польский","Поляк тілі","Polish"),
                    t("Немецкий","Неміс тілі","German"),t("Французский","Француз тілі","French"),t("Китайский","Қытай тілі","Chinese"),t("Турецкий","Түрік тілі","Turkish"),
                    t("Корейский","Корей тілі","Korean"),t("Испанский","Испан тілі","Spanish"),t("Арабский","Араб тілі","Arabic"),t("Японский","Жапон тілі","Japanese"),t("Узбекский","Өзбек тілі","Uzbek")
                ),{record=record.copy(title=it);invalid=false})
                val codes=listOf("native","A1","A2","B1","B2","C1","C2")
                ResumeChoice(t("Уровень владения","Меңгеру деңгейі","Proficiency level"),ResumeReference.level(record.subtitle,lang),codes.map {ResumeReference.level(it,lang)},{v->record=record.copy(subtitle=codes.firstOrNull {ResumeReference.level(it,lang)==v} ?: "")},custom=false)
                Text(t("A1–C2 — уровни CEFR. «Родной» — отдельный вариант, а не уровень CEFR.","A1–C2 — CEFR деңгейлері. «Ана тілі» — бөлек нұсқа, CEFR деңгейі емес.","A1–C2 are CEFR levels. Native is a separate option, not a CEFR level."),color=Muted)
            }
            else->PersonalField(t("Название","Атауы","Name"),record.title,{record=record.copy(title=it);invalid=false},Icons.Outlined.StarOutline)
        }
        if(invalid)Text(t("Заполните название и проверьте даты: начало не позже завершения, будущие месяцы не допускаются. Для языка выберите уровень.","Атауды толтырып, күндерді тексеріңіз: басталуы аяқталуынан кеш болмауы керек. Тіл деңгейін таңдаңыз.","Enter a title and check dates: start must precede end, and future months are not allowed. Choose a language level."),color=MaterialTheme.colorScheme.error)
        PrimaryButton(if(existing)t("Сохранить","Сақтау","Save") else t("Добавить","Қосу","Add")) {
            val missingDates=kind=="work" && !(record.startMonth.isBlank() && record.period.isNotBlank()) && !ResumeDates.validRange(record)
            invalid=record.title.isBlank() || missingDates || (kind=="languages" && record.subtitle.isBlank())
            if(!invalid)save(record.copy(title=record.title.trim(),period=if(kind=="work" && record.startMonth.isNotBlank())ResumeDates.period(record,lang) else record.period))
        }
    }
}
