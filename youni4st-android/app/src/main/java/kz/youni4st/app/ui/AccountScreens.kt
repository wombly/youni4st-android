package kz.youni4st.app.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kz.youni4st.app.AppViewModel
import kz.youni4st.app.R
import kz.youni4st.app.data.editPhoneDigits

tailrec fun Context.activity(): Activity? = when(this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

class PhoneMask : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input=text.text.take(10)
        val builder=StringBuilder("+7 (")
        val offsets=mutableListOf(4)
        input.forEachIndexed { i,c ->
            builder.append(c)
            if(i==2) builder.append(") ")
            if(i==5 || i==7) builder.append('-')
            offsets.add(builder.length)
        }
        return TransformedText(AnnotatedString(builder.toString()),object:OffsetMapping {
            override fun originalToTransformed(offset:Int)=offsets[offset.coerceIn(0,input.length)]
            override fun transformedToOriginal(offset:Int)=offsets.indexOfLast { it<=offset }.coerceAtLeast(0).coerceAtMost(input.length)
        })
    }
}

@Composable
fun WelcomeScreen(vm:AppViewModel,navigate:(String)->Unit,onContinue:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(20.dp))
        Image(painterResource(R.drawable.brand_logo),"Youni4st",Modifier.width(205.dp).height(44.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
        SectionArt(1,Modifier.size(230.dp,170.dp))
        Text(t("Будущее начинается\nс твоего выбора","Болашақ сенің\nтаңдауыңнан басталады","Your future begins\nwith your choice"),style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
        Text(t("Выбирай вуз, находи первый опыт и задавай важные вопросы Сократусу.","ЖОО таңда, тәжірибе жина және Сократусқа сұрақ қой.","Find a university, build experience and ask Sokratus what matters."),color=Muted,textAlign=TextAlign.Center)
        PrimaryButton(t("Войти или зарегистрироваться","Кіру немесе тіркелу","Sign in or register")) {navigate("login")}
        TextButton(onContinue) {Text(t("Пока продолжить без входа","Әзірге кірмей жалғастыру","Explore without signing in"))}
        MenuRow(t("Язык","Тіл","Language"),"Русский · Қазақша · English",Icons.Outlined.Language) {navigate("settings")}
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            TextButton({navigate("support")}) {Text(t("Поддержка","Қолдау","Support"))}
            TextButton({navigate("about")}) {Text(t("О приложении","Қолданба туралы","About"))}
        }
    }
}

@Composable
fun LoginScreen(vm:AppViewModel,onBack:()->Unit,navigate:(String)->Unit,onSuccess:()->Unit,linkMode:Boolean=false) {
    val context=LocalContext.current
    val activity=context.activity()
    val scope=rememberCoroutineScope()
    var phone by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var emailMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var phoneRequestActive by remember {mutableStateOf(false)}
    var phoneAttempt by remember {mutableIntStateOf(0)}
    var active by remember {mutableStateOf(true)}
    DisposableEffect(Unit) {onDispose {active=false;phoneAttempt++}}
    var resetSent by remember { mutableStateOf(false) }
    val googleLinked=vm.accountUser?.providerData?.any {it.providerId=="google.com"}==true
    val phoneLinked=vm.accountUser?.providerData?.any {it.providerId=="phone"}==true
    var phoneStartedAt by rememberSaveable {mutableLongStateOf(0L)}
    var resendAt by rememberSaveable {mutableLongStateOf(0L)}
    var clock by remember {mutableLongStateOf(android.os.SystemClock.elapsedRealtime())}
    LaunchedEffect(verificationId) {while(verificationId.isNotBlank()){clock=android.os.SystemClock.elapsedRealtime();delay(1000)}}
    fun beginPhone(rebind:Boolean=false) {
        if(activity==null)return
        if(!rebind){phoneAttempt++;phoneStartedAt=android.os.SystemClock.elapsedRealtime();verificationId="";code=""}
        val attempt=phoneAttempt
        busy=true;phoneRequestActive=true;error=""
        vm.account.phone(activity,"+7$phone",
            {if(active && attempt==phoneAttempt){verificationId=it;busy=false;phoneRequestActive=false;resendAt=phoneStartedAt+60_000}},
            {if(active && attempt==phoneAttempt){busy=false;phoneRequestActive=false;onSuccess()}},
            {if(active && attempt==phoneAttempt){busy=false;phoneRequestActive=false;error=accountErrorCode(it)}},link=linkMode,isCurrent={active && attempt==phoneAttempt})
    }
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,phoneRequestActive,emailMode,verificationId,phoneAttempt) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME && phoneRequestActive && !emailMode && verificationId.isBlank() && phoneAttempt>0) {
                if(android.os.SystemClock.elapsedRealtime()-phoneStartedAt<60_000)beginPhone(rebind=true)
                else {busy=false;phoneRequestActive=false;phoneAttempt++;error="SMS_TIMEOUT"}
            }
        }
        lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(phoneRequestActive,phoneAttempt) {
        if(phoneRequestActive && !emailMode && verificationId.isBlank()) {
            val attempt=phoneAttempt;delay(65_000)
            if(phoneRequestActive && attempt==phoneAttempt && verificationId.isBlank()){busy=false;phoneRequestActive=false;phoneAttempt++;error="SMS_TIMEOUT"}
        }
    }
    fun action(block:suspend ()->Unit) { scope.launch {phoneRequestActive=false;phoneAttempt++;busy=true;error="";try {block();onSuccess()} catch(e:CancellationException){throw e} catch(e:Exception){error=accountErrorCode(e)} finally{busy=false} } }
    Column(Modifier.fillMaxSize().imePadding()) {
        PageTitle(if(linkMode)t("Способы входа","Кіру тәсілдері","Sign-in methods") else t("Личный кабинет","Жеке кабинет","Your account"),onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Image(painterResource(R.drawable.brand_logo),"Youni4st",Modifier.width(175.dp).height(37.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
            Text(if(linkMode)t("Один аккаунт — разные способы входа","Бір аккаунт — бірнеше кіру тәсілі","One account, multiple sign-in methods") else t("Твой следующий шаг","Сенің келесі қадамың","Your next step"),style=MaterialTheme.typography.headlineMedium)
            Text(t("Все возможности — ближе, чем кажется.","Мүмкіндіктер сен ойлағаннан жақын.","Your possibilities are closer than you think."),color=Muted)
            if(!vm.authInitialized) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if(!vm.account.ready) Text(accountErrorText("AUTH_UNAVAILABLE"),color=MaterialTheme.colorScheme.error)
            if(!linkMode) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton({emailMode=false;error=""},Modifier.weight(1f),enabled=!busy,border=BorderStroke(if(!emailMode)2.dp else 1.dp,Color.Black)){Text(t("Телефон","Телефон","Phone"))}
                OutlinedButton({emailMode=true;error=""},Modifier.weight(1f),enabled=!busy,border=BorderStroke(if(emailMode)2.dp else 1.dp,Color.Black)){Text("E-mail")}
            }
            if(emailMode && !linkMode) {
                OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("E-mail")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),shape=RoundedCornerShape(18.dp))
                OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text(t("Пароль · от 8 символов","Құпиясөз · 8 таңбадан","Password · 8+ characters"))},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),shape=RoundedCornerShape(18.dp))
                TextButton({create=!create}) {Text(if(create)t("Уже есть аккаунт? Войти","Аккаунт бар ма? Кіру","Already registered? Sign in") else t("Создать аккаунт","Аккаунт ашу","Create account"))}
                PrimaryButton(if(create)t("Создать аккаунт","Аккаунт ашу","Create account") else t("Войти","Кіру","Sign in"),enabled=vm.authInitialized && vm.account.ready && !busy && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && password.length>=(if(create)8 else 6)) {action {vm.account.email(email,password,create)}}
                TextButton({scope.launch {busy=true;try{vm.account.resetPassword(email);resetSent=true}catch(e:Exception){error=accountErrorCode(e)}finally{busy=false}}},enabled=vm.authInitialized && vm.account.ready && !busy && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {Text(t("Забыли пароль?","Құпиясөзді ұмыттыңыз ба?","Forgot password?"))}
                if(resetSent) Text(t("Проверь почту: отправлены инструкции для восстановления.","Қалпына келтіру нұсқаулығын поштадан тексер.","Check your email for reset instructions."),color=Teal)
            } else {
                OutlinedTextField(phone,{phone=editPhoneDigits(phone,it)},Modifier.fillMaxWidth(),enabled=!busy && verificationId.isBlank(),label={Text(t("Номер телефона","Телефон нөмірі","Phone number"))},visualTransformation=PhoneMask(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),shape=RoundedCornerShape(18.dp))
                if(verificationId.isNotBlank()) OutlinedTextField(code,{code=it.filter(Char::isDigit).take(6)},Modifier.fillMaxWidth(),label={Text(t("Код из SMS","SMS коды","SMS code"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),shape=RoundedCornerShape(18.dp))
                PrimaryButton(if(busy)t("Подождите…","Күтіңіз…","Please wait…") else t("Продолжить","Жалғастыру","Continue"),enabled=vm.authInitialized && vm.account.ready && !busy && !(linkMode && phoneLinked) && phone.length==10 && (verificationId.isBlank() || code.length==6)) {
                    if(verificationId.isNotBlank()) action {vm.account.verifyCode(verificationId,code,link=linkMode)}
                    else beginPhone()
                }
                if(verificationId.isNotBlank()) {
                    Text(t("Код отправлен на +7$phone","Код +7$phone нөміріне жіберілді","Code sent to +7$phone"),color=Muted)
                    TextButton({code="";beginPhone()},enabled=!busy && clock>=resendAt) {Text(if(clock<resendAt)t("Повторить через ${(resendAt-clock+999)/1000} с","${(resendAt-clock+999)/1000} с кейін қайталау","Resend in ${(resendAt-clock+999)/1000}s") else t("Отправить код ещё раз","Кодты қайта жіберу","Resend code"))}
                    TextButton({phoneAttempt++;verificationId="";code=""},enabled=!busy) {Text(t("Изменить номер","Нөмірді өзгерту","Change number"))}
                }
                if(linkMode && phoneLinked) Text(t("Телефон уже привязан к этому аккаунту.","Телефон осы аккаунтқа байланыстырылған.","A phone number is already linked to this account."),color=Muted)
            }
            if(error.isNotBlank()) Text(accountErrorText(error),color=MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {HorizontalDivider(Modifier.weight(1f));Text(t("или","немесе","or"),color=Muted);HorizontalDivider(Modifier.weight(1f))}
            OutlinedButton({if(!vm.account.googleReady) error="GOOGLE_CONFIG" else if(activity!=null)action{vm.account.google(activity,link=linkMode)}},Modifier.fillMaxWidth().heightIn(min=52.dp),enabled=vm.authInitialized && !busy && !(linkMode && googleLinked),shape=RoundedCornerShape(24.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=androidx.compose.ui.graphics.Color.White),border=BorderStroke(1.dp,androidx.compose.ui.graphics.Color(0xFF747775))) {
                Image(painterResource(R.drawable.google_brand_g),null,Modifier.size(20.dp));Spacer(Modifier.width(12.dp));Text(if(linkMode && googleLinked)t("Google уже привязан","Google байланыстырылған","Google already linked") else if(linkMode)t("Привязать Google","Google байланыстыру","Link Google") else t("Продолжить через Google","Google арқылы жалғастыру","Continue with Google"))
            }
            if(!linkMode) OutlinedButton({emailMode=!emailMode;error=""},Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(18.dp)) {Icon(if(emailMode)Icons.Outlined.PhoneAndroid else Icons.Outlined.MailOutline,null);Spacer(Modifier.width(10.dp));Text(if(emailMode)t("Войти по телефону","Телефонмен кіру","Use phone number") else t("Регистрация через E-mail","E-mail арқылы тіркелу","Register with email"))}
            Text(t("Нажимая «Продолжить», вы соглашаетесь с условиями использования и Политикой конфиденциальности.","«Жалғастыру» түймесін басу арқылы пайдалану шарттары мен Құпиялық саясатына келісесіз.","By continuing, you agree to the Terms of Use and Privacy Policy."),color=Muted,fontSize=12.sp,lineHeight=18.sp)
            Row {TextButton({navigate("terms")}){Text(t("Условия","Шарттар","Terms"))};TextButton({navigate("privacy")}){Text(t("Конфиденциальность","Құпиялық","Privacy"))}}
        }
    }
}

