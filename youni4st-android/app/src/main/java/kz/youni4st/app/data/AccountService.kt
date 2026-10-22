package kz.youni4st.app.data

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AccountService(private val context: Context, private val config: AppConfig) {
    var auth: FirebaseAuth? = null
        private set
    suspend fun initialize() { auth = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching {
        val apiKey = config.group("firebase", "api_key").ifBlank {kz.youni4st.app.BuildConfig.FIREBASE_API_KEY}
        val appId = config.group("firebase", "application_id").ifBlank {kz.youni4st.app.BuildConfig.FIREBASE_APP_ID}
        val projectId = config.group("firebase", "project_id").ifBlank {kz.youni4st.app.BuildConfig.FIREBASE_PROJECT_ID}
        if (listOf(apiKey, appId, projectId).any(String::isBlank)) null else {
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder().setApiKey(apiKey).setApplicationId(appId).setProjectId(projectId).build())
            FirebaseAuth.getInstance(app)
        }
    }.getOrNull() } }
    val ready get() = auth != null
    private val googleClientId get()=config.group("firebase", "google_web_client_id").ifBlank {kz.youni4st.app.BuildConfig.GOOGLE_WEB_CLIENT_ID}
    val googleReady get() = ready && googleClientId.isNotBlank()
    suspend fun email(email: String, password: String, create: Boolean) {
        val a = requireNotNull(auth)
        if (create) { a.createUserWithEmailAndPassword(email.trim(), password).await(); a.currentUser?.sendEmailVerification() }
        else a.signInWithEmailAndPassword(email.trim(), password).await()
    }
    suspend fun resetPassword(email: String) { requireNotNull(auth).sendPasswordResetEmail(email.trim()).await() }
    suspend fun google(activity: Activity, link:Boolean=false) {
        check(googleReady)
        val option = GetSignInWithGoogleOption.Builder(googleClientId).build()
        val result = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        check(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val credential=GoogleAuthProvider.getCredential(token,null)
        if(link) requireNotNull(auth?.currentUser).linkWithCredential(credential).await()
        else requireNotNull(auth).signInWithCredential(credential).await()
    }
    fun phone(activity: Activity, number: String, onCode: (String) -> Unit, onVerified: () -> Unit, onError: (Exception) -> Unit, link:Boolean=false,isCurrent:()->Boolean={true}) {
        val a = requireNotNull(auth)
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if(!isCurrent())return
                val task=if(link) {
                    val user=a.currentUser
                    if(user==null){onError(RegistryException("AUTH_REQUIRED"));return}
                    user.linkWithCredential(credential)
                } else a.signInWithCredential(credential)
                task.addOnSuccessListener { onVerified() }.addOnFailureListener { onError(it) }
            }
            override fun onVerificationFailed(e: FirebaseException) {if(isCurrent())onError(e)}
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {if(isCurrent())onCode(id)}
            override fun onCodeAutoRetrievalTimeOut(id:String) {if(isCurrent())onCode(id)}
        }
        try { PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(a).setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(callbacks).build()) } catch(e:Exception) {onError(e)}
    }
    suspend fun verifyCode(id:String,code:String,link:Boolean=false) {
        val credential=PhoneAuthProvider.getCredential(id,code)
        if(link)requireNotNull(auth?.currentUser).linkWithCredential(credential).await()
        else requireNotNull(auth).signInWithCredential(credential).await()
    }
    suspend fun signOut(context: Context) {
        auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
