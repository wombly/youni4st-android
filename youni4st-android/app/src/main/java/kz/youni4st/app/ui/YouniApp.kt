@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package kz.youni4st.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.data.Language

@Composable
fun YouniApp(vm: AppViewModel, restartSignal:Int=0, initialIntroDone:Boolean=false) {
    CompositionLocalProvider(LocalLanguage provides vm.language) {
        YouniTheme {
            var introDone by rememberSaveable {mutableStateOf(initialIntroDone)}
            if(!introDone) {BrandIntro {introDone=true};return@YouniTheme}
            var route by rememberSaveable { mutableStateOf(if(vm.welcomed || vm.accountUser!=null) "home" else "welcome") }
            var stack by rememberSaveable {mutableStateOf(listOf<String>())}
            var editProfile by rememberSaveable { mutableStateOf(false) }
            var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
            fun navigate(next:String) {if(next=="edit_profile"){editProfile=true;return};if(route!=next){stack=stack+route;route=next}}
            fun back() {route=stack.lastOrNull() ?: if(vm.welcomed)"home" else "welcome";stack=stack.dropLast(1)}
            fun enter() {vm.enterApp();stack=emptyList();route="home"}
            BackHandler(enabled=route !in setOf("home","welcome") && selectedId==null) {back()}
            val catalog=vm.catalog
            val fullScreen=route in setOf("chat","resume","login","link_account","welcome","settings","support","about","privacy","terms")
            Surface(Modifier.fillMaxSize().dismissKeyboardOnBackgroundTap(),color=CanvasColor) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
                    Scaffold(modifier=Modifier.widthIn(max=600.dp).fillMaxSize(),containerColor=CanvasColor,
                        contentWindowInsets=WindowInsets.systemBars,
                        topBar={Column(Modifier.statusBarsPadding()){if(!fullScreen) BrandHeader(vm,::navigate)}},
                        bottomBar={if(!fullScreen)BottomDock(route){navigate(it)}}
                    ) {padding ->
                        Box(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
                            val needsCatalog=route !in setOf("home","welcome","login","link_account","chat","settings","support","about","privacy","terms")
                            if(needsCatalog && vm.loadError) EmptyState(t("Не удалось открыть каталог","Каталог ашылмады","Could not load catalog"),t("Повторить","Қайталау","Retry")){vm.loadCatalog()}
                            else if(needsCatalog && catalog==null) CircularProgressIndicator(Modifier.align(Alignment.Center))
                            else androidx.compose.animation.Crossfade(targetState=route,label="page",animationSpec=androidx.compose.animation.core.tween(180)) {page ->
                                when(page) {
                                    "welcome"->WelcomeScreen(vm,::navigate,::enter)
                                    "login"->LoginScreen(vm,::back,::navigate,::enter)
                                    "link_account"->LoginScreen(vm,::back,::navigate,{stack=emptyList();route="profile"},linkMode=true)
                                    "settings"->SettingsScreen(vm,::back,::navigate)
                                    "support"->SupportScreen(vm,::back,::navigate)
                                    "about"->AboutScreen(vm,::back,::navigate)
                                    "privacy","terms"->LegalScreen(vm,page=="privacy",::back)
                                    "home"->HomeScreen(vm,::navigate)
                                    "universities"->UniversityScreen(vm,{navigate("saved_universities")}){selectedId=it}
                                    "jobs"->JobsScreen(vm,::navigate){selectedId=it}
                                    "mentors"->MentorsScreen(vm,::back,{navigate("saved_mentors")}){selectedId=it}
                                    "chat"->ChatScreen(vm,::back)
                                    "resume"->ResumeScreen(vm) {stack=emptyList();route="jobs"}
                                    "profile"->ProfileScreen(vm,::navigate){selectedId=it}
                                    "favorites","drafts","saved_universities","saved_mentors","saved_jobs","saved_programs","saved_ielts","saved_grants","saved_news"->SavedScreen(vm,page=="drafts",when(page){"saved_universities"->"universities";"saved_mentors"->"mentors";"saved_jobs"->"jobs";"saved_programs"->"programs";"saved_ielts"->"ielts";"saved_grants"->"grants";"saved_news"->"news";else->""},::back,::navigate){selectedId=it}
                                    else->ArticlesScreen(vm,page,::back,{selectedId=it},{navigate("saved_$page")})
                                }
                            }
                        }
                    }
                }
            }
            val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle,vm) {
                val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->if(event==androidx.lifecycle.Lifecycle.Event.ON_START)vm.requestCloudSync(pull=true)}
                lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
            }
            var resumeIntro by remember(restartSignal) {mutableStateOf(false)}
            LaunchedEffect(restartSignal) {if(vm.consumeRestartSignal(restartSignal)){resumeIntro=true;vm.loadCatalog()}}
            if(editProfile) ProfileEditor(vm,onLogin={editProfile=false;navigate("login")}) {editProfile=false}
            if(vm.cloudRestoring && vm.accountUser!=null) AlertDialog(onDismissRequest={},
                title={Text(t("Восстанавливаем данные","Деректер қалпына келтірілуде","Restoring your data"))},
                text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){LinearProgressIndicator(Modifier.fillMaxWidth());Text(t("Загружаем данные твоего аккаунта из облака.","Аккаунт деректері бұлттан жүктелуде.","Loading your account data from the cloud."))}},confirmButton={})
            else if(vm.accountUser!=null && (!vm.usernameLoaded || vm.registeredUsername.isBlank())) UsernameGate(vm)
            selectedId?.let {id ->catalog?.entries?.find {it.id==id}?.let {entry ->
                EntrySheet(entry,vm,{selectedId=null},{selectedId=null;navigate("resume")},{selectedId=null;navigate("login")})
            }}
            if(resumeIntro) androidx.compose.ui.window.Dialog(onDismissRequest={},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {BrandIntro {resumeIntro=false}}
        }
    }
}
@Composable
private fun BottomDock(route: String, onSelect: (String) -> Unit) {
    val career = route in setOf("jobs", "mentors", "drafts")
    Row(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp)
        .fillMaxWidth().shadow(14.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
        .background(Dock).heightIn(min = 66.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        DockItem(Icons.Outlined.Home, t("Главная", "Басты бет", "Home"), route == "home") { onSelect("home") }
        DockItem(Icons.Outlined.School, t("Вузы", "ЖОО", "Universities"), route == "universities") { onSelect("universities") }
        IconButton(
    { onSelect("chat") },
    Modifier
        .size(54.dp)
        .shadow(
            9.dp,
            CircleShape,
            ambientColor = Color(0xFF235F85),
            spotColor = Color(0xFF235F85)
        )
        .background(Color(0xFF235F85), CircleShape)
        .border(1.5.dp, Color.White, CircleShape)
) {
    Icon(
        Icons.Rounded.AutoAwesome,
        t("AI Сократус", "AI Сократус", "AI Sokratus"),
        Modifier.size(29.dp),
        tint = Color.White
    )

        }
        DockItem(Icons.Outlined.WorkOutline, t("Карьера", "Мансап", "Career"), career) { onSelect("jobs") }
        DockItem(Icons.Outlined.PersonOutline, t("Профиль", "Профиль", "Profile"), (route in setOf("profile", "favorites") || route.startsWith("saved_"))) { onSelect("profile") }
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(48.dp).background(if(selected) Color.White.copy(alpha=.14f) else Color.Transparent,CircleShape)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(25.dp))
    }
}
