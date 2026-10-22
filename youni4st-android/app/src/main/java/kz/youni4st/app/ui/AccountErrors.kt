package kz.youni4st.app.ui

import androidx.compose.runtime.Composable
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kz.youni4st.app.data.RegistryException

/** No raw SDK messages: they may include account details. Keep the diagnostic code. */
fun accountErrorCode(error:Exception):String=when(error) {
    is RegistryException->error.code
    is FirebaseAuthException->listOf("BILLING_NOT_ENABLED","SMS_REGION_CONFIG_ERROR","INVALID_APP_CREDENTIAL","INVALID_CERT_HASH").firstOrNull {error.message.orEmpty().contains(it)}?.let {"ERROR_$it"} ?: error.errorCode
    is FirebaseNetworkException,is java.io.IOException->"NETWORK"
    is FirebaseTooManyRequestsException->"TOO_MANY_REQUESTS"
    is GetCredentialCancellationException->"CANCELLED"
    is NoCredentialException->"NO_GOOGLE_ACCOUNT"
    else->"AUTH_UNAVAILABLE"
}

@Composable
fun accountErrorText(code:String):String=when(code) {
    "INVALID_USERNAME"->t("Никнейм: 3–24 символа, латинская буква в начале, затем буквы, цифры или _. Например: wombly.","Лақап ат: 3–24 таңба. Алдымен латын әрпі, кейін әріптер, сандар немесе _. Мысалы: wombly.","Username: 3–24 characters, starting with a Latin letter, then letters, numbers or _. Example: wombly.")
    "ALREADY_EXISTS"->t("Этот никнейм уже занят. Выбери другой.","Бұл лақап ат бос емес. Басқасын таңда.","That username is already taken. Choose another.")
    "FAILED_PRECONDITION","ABORTED"->t("Профиль изменился на другом устройстве или никнейм уже занят. Повтори сохранение.","Профиль басқа құрылғыда өзгерді немесе лақап ат бос емес. Қайта сақта.","Your profile changed on another device or the username was taken. Try saving again.")
    "PERMISSION_DENIED","NOT_FOUND"->t("Реестр никнеймов недоступен. Администратору нужно настроить Cloud Firestore и правила доступа.","Лақап аттар тізілімі қолжетімсіз. Әкімші Cloud Firestore мен ережелерді баптауы керек.","The username registry is unavailable. The administrator must configure Cloud Firestore and its rules.")
    "AUTH_REQUIRED","UNAUTHENTICATED"->t("Войди в аккаунт, чтобы закрепить никнейм за собой.","Лақап атты тіркеу үшін аккаунтқа кір.","Sign in to reserve your username.")
    "GOOGLE_CONFIG"->t("В этой сборке не настроен вход Google. Нужен обновлённый файл Firebase с OAuth-клиентом и SHA сертификата APK.","Бұл нұсқада Google кіруі бапталмаған. OAuth клиенті және APK сертификатының SHA мәні бар жаңартылған Firebase файлы қажет.","Google sign-in is not configured in this build. An updated Firebase file with an OAuth client and the APK certificate SHA is required.")
    "ERROR_EMAIL_ALREADY_IN_USE"->t("Аккаунт с такой почтой уже есть. Нажми «Уже есть аккаунт? Войти».","Бұл пошта тіркелген. «Аккаунт бар ма? Кіру» түймесін бас.","This email is already registered. Choose ‘Already registered? Sign in’.")
    "ERROR_INVALID_CREDENTIAL","ERROR_WRONG_PASSWORD","ERROR_USER_NOT_FOUND","ERROR_INVALID_LOGIN_CREDENTIALS"->t("Не подошла почта или пароль. Проверь их или восстанови пароль.","Пошта немесе құпиясөз сәйкес емес. Тексер немесе құпиясөзді қалпына келтір.","Email or password is incorrect. Check them or reset your password.")
    "ERROR_INVALID_VERIFICATION_CODE","ERROR_INVALID_VERIFICATION_ID","ERROR_SESSION_EXPIRED"->t("Код SMS неверный или истёк. Проверь код либо запроси новый.","SMS коды қате немесе мерзімі өткен. Тексер немесе жаңа код сұра.","The SMS code is incorrect or expired. Check it or request a new one.")
    "ERROR_CREDENTIAL_ALREADY_IN_USE","ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL","ERROR_EMAIL_ALREADY_IN_USE_LINK"->t("Этот способ входа относится к другому аккаунту. Войди в тот аккаунт; автоматического объединения данных нет.","Бұл кіру тәсілі басқа аккаунтқа тиесілі. Сол аккаунтқа кір; деректер автоматты біріктірілмейді.","This sign-in method belongs to another account. Sign in to that account; data is not merged automatically.")
    "ERROR_PROVIDER_ALREADY_LINKED"->t("Этот способ входа уже привязан к аккаунту.","Бұл кіру тәсілі аккаунтқа байланыстырылған.","This sign-in method is already linked.")
    "SMS_TIMEOUT"->t("Не удалось завершить отправку SMS. Проверь интернет и повтори запрос.","SMS жіберу аяқталмады. Интернетті тексеріп, қайтала.","The SMS request did not finish. Check your connection and retry.")
    "ERROR_SMS_REGION_CONFIG_ERROR"->t("Отправка SMS в этот регион запрещена в Firebase. Разреши Казахстан в Authentication → Settings → SMS region policy.","Firebase бұл аймаққа SMS жіберуді бұғаттаған. Authentication → Settings → SMS region policy ішінде Қазақстанға рұқсат бер.","Firebase blocks SMS to this region. Allow Kazakhstan under Authentication → Settings → SMS region policy.")
    "ERROR_BILLING_NOT_ENABLED"->t("Firebase требует настроить оплату для отправки SMS. Можно использовать вход Google или e-mail.","Firebase SMS үшін төлемді баптауды талап етеді. Google не e-mail арқылы кіруге болады.","Firebase requires billing for SMS. You can use Google or email sign-in.")
    "ERROR_INVALID_PHONE_NUMBER"->t("Проверь номер: после +7 должно быть 10 цифр.","Нөмірді тексер: +7-ден кейін 10 сан болуы керек.","Check the number: 10 digits must follow +7.")
    "ERROR_QUOTA_EXCEEDED","TOO_MANY_REQUESTS","ERROR_TOO_MANY_REQUESTS"->t("Достигнут лимит SMS или попыток входа. Попробуй позже или войди по почте.","SMS не кіру әрекеттерінің шегі асты. Кейін қайтала немесе поштамен кір.","The SMS or sign-in attempt limit was reached. Try later or use email.")
    "ERROR_APP_NOT_AUTHORIZED","ERROR_INVALID_APP_CREDENTIAL","ERROR_MISSING_APP_CREDENTIAL","ERROR_OPERATION_NOT_ALLOWED","ERROR_CAPTCHA_CHECK_FAILED","ERROR_INVALID_CERT_HASH","ERROR_MISSING_CLIENT_IDENTIFIER"->t("Firebase отклонил настройку входа. Администратору нужно проверить SHA сертификата, провайдер и разрешённые регионы SMS.","Firebase кіру баптауын қабылдамады. Әкімші сертификат SHA мәнін, провайдерді және SMS аймақтарын тексеруі керек.","Firebase rejected the sign-in configuration. The administrator must check certificate SHA, provider settings and allowed SMS regions.")
    "ERROR_USER_DISABLED"->t("Этот аккаунт отключён. Обратись в поддержку.","Бұл аккаунт өшірілген. Қолдауға хабарлас.","This account is disabled. Contact support.")
    "ERROR_WEAK_PASSWORD"->t("Выбери более длинный пароль.","Ұзынырақ құпиясөз таңда.","Choose a longer password.")
    "NETWORK","UNAVAILABLE","DEADLINE_EXCEEDED"->t("Нет соединения с сервисом. Проверь интернет и повтори.","Қызметке қосылмады. Интернетті тексеріп, қайтала.","Cannot connect to the service. Check your connection and retry.")
    "NO_GOOGLE_ACCOUNT"->t("Добавь Google-аккаунт в настройках телефона и повтори вход.","Телефон баптауларында Google аккаунтын қосып, қайта кір.","Add a Google account in your phone’s settings and retry.")
    "CANCELLED"->t("Вход отменён.","Кіру тоқтатылды.","Sign-in cancelled.")
    else->t("Сервис входа недоступен. Повтори попытку или отправь код ошибки в поддержку.","Кіру қызметі қолжетімсіз. Қайтала немесе қате кодын қолдауға жібер.","Sign-in is unavailable. Try again or send this error code to support.")
} + " [$code]"
