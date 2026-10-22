package kz.youni4st.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kz.youni4st.app.data.*
import kotlinx.coroutines.tasks.await

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var store: LocalStore
    private val api by lazy { ChatApi(BuildConfig.API_BASE_URL) }
    private val introReplay=IntroReplayGate()
    fun consumeRestartSignal(signal:Int)=introReplay.consume(signal)
    lateinit var config: AppConfig
        private set
    lateinit var account: AccountService
        private set
    var authInitialized by mutableStateOf(false)
        private set
    var accountUser by mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null)
        private set
    var welcomed by mutableStateOf(false)
        private set
    var applicationBusy by mutableStateOf<String?>(null)
        private set
    var applicationError by mutableStateOf(false)
        private set
    var receipts by mutableStateOf(org.json.JSONObject())
        private set
    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
        val user=auth.currentUser
        if(accountUser?.uid!=user?.uid) {
            usernameJob?.cancel();syncJob?.cancel();retryJob?.cancel();chatJob?.cancel()
            chatBusy=false;applicationBusy=null;cloudBusy=false;cloudRestoring=false;cloudError="";cloudHasPending=false;cloudLastSaved=0L;syncFailures=0;cloudPullRequested=false
            accountUser=user;registeredUsername="";usernameLoaded=false;usernameError=""
            store.activate(user?.uid)
            if(user!=null){registeredUsername=store.verifiedUsername(user.uid);usernameLoaded=registeredUsername.isNotBlank()}
            restoreLocalAccount()
            if(user!=null) {
                enterApp()
                val needsRestore=profile==UserProfile() && resume==Resume() && favorites.isEmpty() && messages.isEmpty() && !store.hasCloud(user.uid)
                profile=profile.copy(name=profile.name.ifBlank {user.displayName.orEmpty()},email=profile.email.ifBlank {user.email.orEmpty()})
                store.withoutTracking {store.profile=profile}
                refreshUsername();cloudRestoring=needsRestore;requestCloudSync(pull=true)
            }
        }
    }
    private fun restoreLocalAccount() {
        profile=store.profile;resume=store.resume;favorites=store.favorites;drafts=store.drafts
        messages=store.chat;language=store.language;receipts=store.receipts
        chatFailed=!chatBusy && messages.lastOrNull()?.role=="user"
    }
    private var syncJob:kotlinx.coroutines.Job?=null
    private var retryJob:kotlinx.coroutines.Job?=null
    private var chatJob:kotlinx.coroutines.Job?=null
    private var syncRequested=false
    private var cloudPullRequested=false
    private var syncFailures=0
    private val photos by lazy {CloudPhotos(getApplication())}
    private var networkCallback:android.net.ConnectivityManager.NetworkCallback?=null
    var cloudBusy by mutableStateOf(false)
        private set
    var cloudRestoring by mutableStateOf(false)
        private set
    var cloudError by mutableStateOf("")
        private set
    var cloudHasPending by mutableStateOf(false)
        private set
    var cloudLastSaved by mutableStateOf(0L)
        private set
    fun requestCloudSync(pull:Boolean=false) {
        val user=accountUser ?: return
        cloudHasPending=store.pending(user.uid).isNotEmpty();syncRequested=true;cloudPullRequested=cloudPullRequested || pull || cloudRestoring
        retryJob?.cancel()
        if(syncJob?.isActive==true)return
        syncJob=viewModelScope.launch {
            kotlinx.coroutines.delay(if(cloudRestoring)0 else 700)
            do {
                syncRequested=false;cloudBusy=true;cloudError=""
                try {
                    val pull=cloudPullRequested;cloudPullRequested=false
                    syncAccount(user,pull)
                    syncFailures=0;cloudLastSaved=System.currentTimeMillis()
                } catch(e:CancellationException){throw e}
                catch(e:Exception) {
                    cloudError=kz.youni4st.app.ui.accountErrorCode(e);syncFailures++
                    if(cloudError in setOf("NETWORK","UNAVAILABLE","DEADLINE_EXCEEDED")) {
                        retryJob=viewModelScope.launch {
                            kotlinx.coroutines.delay((5_000L shl syncFailures.coerceAtMost(4)).coerceAtMost(60_000L))
                            retryJob=null
                            if(accountUser?.uid==user.uid)requestCloudSync(pull=true)
                        }
                    }
                } finally {
                    if(accountUser?.uid==user.uid) {cloudBusy=false;cloudRestoring=false;cloudHasPending=store.pending(user.uid).isNotEmpty()}
                }
            } while(accountUser?.uid==user.uid && cloudError.isBlank() && (syncRequested || cloudHasPending))
        }
    }
    private suspend fun syncAccount(user:com.google.firebase.auth.FirebaseUser,pull:Boolean) {
        val uid=user.uid
        val token=kotlinx.coroutines.withTimeoutOrNull(15_000) {user.getIdToken(false).await().token} ?: throw RegistryException("DEADLINE_EXCEEDED")
        val api=CloudAccountApi(requireNotNull(account.auth?.app?.options?.projectId))
        val shouldRead=pull || !store.hasCloud(uid)
        val remote=if(shouldRead)kotlinx.coroutines.withContext(Dispatchers.IO) {api.read(uid,token)?.also {o ->
            for(section in listOf("profile","resume"))o.optJSONObject(section)?.let {o.put(section,photos.download(it,uid))}
        }} else null
        if(accountUser?.uid!=uid)return
        if(shouldRead) {
        val pending=store.pending(uid)
        val first=!store.hasCloud(uid)
        // Old local favorites are imported once; server tombstones prevent resurrecting deletions.
        for(section in listOf("favorites","drafts")) {
            val local=if(section=="favorites")store.favorites else store.drafts
            if(first)local.forEach {id ->if(remote?.optJSONObject(section)?.has(CloudItems.hash(id))!=true)store.mark("$section|$id")}
        }
        store.withoutTracking {
            if(remote!=null) {
                remote.optJSONObject("profile")?.takeIf {"profile" !in pending}?.let {store.profile=UserProfile.fromJson(it).copy(username=registeredUsername)}
                remote.optJSONObject("resume")?.takeIf {"resume" !in pending}?.let {store.resume=Resume.fromJson(it)}
                remote.optJSONArray("chat")?.takeIf {"chat" !in pending}?.let {a->store.chat=(0 until a.length()).map {i->a.getJSONObject(i).let {ChatMessage(it.getString("role"),it.getString("content"))}}}
                remote.optJSONObject("settings")?.takeIf {"settings" !in pending}?.let {o->runCatching {Language.valueOf(o.optString("language"))}.getOrNull()?.let {store.language=it}}
                remote.optJSONObject("receipts")?.takeIf {"receipts" !in pending}?.let {store.receipts=it}
                for(section in listOf("favorites","drafts")) {
                    val before=if(section=="favorites")store.favorites else store.drafts
                    val merged=CloudMerge.items(remote.optJSONObject(section),before,store.pending(uid).keys,section)
                    if(section=="favorites")store.favorites=merged else store.drafts=merged
                }
            }
        }
        if(remote!=null)restoreLocalAccount()
        for(section in listOf("profile","resume","chat","settings","receipts"))if(remote?.has(section)!=true && section !in store.pending(uid))store.mark(section)
        }
        val toSend=store.pending(uid)
        if(toSend.isNotEmpty()) {
            val fields=org.json.JSONObject();val paths=mutableListOf<String>()
            toSend.keys.forEach {key ->
                when {
                    key.startsWith("favorites|")->{val id=key.substringAfter('|');paths+=CloudItems.patch(fields,"favorites",id,id in store.favorites)}
                    key.startsWith("drafts|")->{val id=key.substringAfter('|');paths+=CloudItems.patch(fields,"drafts",id,id in store.drafts)}
                    key=="profile"->{fields.put(key,store.profile.toJson());paths+=key}
                    key=="resume"->{fields.put(key,store.resume.toJson());paths+=key}
                    key=="chat"->{fields.put(key,org.json.JSONArray(store.chat.map {org.json.JSONObject().put("role",it.role).put("content",it.content)}));paths+=key}
                    key=="settings"->{fields.put(key,org.json.JSONObject().put("language",store.language.name));paths+=key}
                    key=="receipts"->{fields.put(key,store.receipts);paths+=key}
                }
            }
            withContext(Dispatchers.IO) {
                for(section in listOf("profile","resume"))fields.optJSONObject(section)?.let {fields.put(section,photos.upload(it))}
                api.write(uid,token,fields,paths)
            }
            // A late response from an old session never changes the new account's state.
            if(accountUser?.uid!=uid)return
            store.acknowledge(uid,toSend)
        }
        store.markCloud(uid)
    }
    private fun observeNetwork() {
        val manager=getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)
        val callback=object:android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network:android.net.Network) {viewModelScope.launch {if(startupReady)requestCloudSync(pull=true)}}
        }
        runCatching {manager.registerDefaultNetworkCallback(callback);networkCallback=callback}
    }
    private var usernameJob:kotlinx.coroutines.Job?=null
    var registeredUsername by mutableStateOf("")
        private set
    var usernameLoaded by mutableStateOf(false)
        private set
    var usernameBusy by mutableStateOf(false)
        private set
    var usernameError by mutableStateOf("")
        private set
    private fun registry()=UsernameRegistry(requireNotNull(account.auth?.app?.options?.projectId))
    fun refreshUsername() {
        val user=accountUser ?: return
        usernameJob?.cancel();usernameBusy=true;usernameError=""
        usernameJob=viewModelScope.launch {
            try {
                val token=requireNotNull(user.getIdToken(false).await().token)
                val result=withContext(Dispatchers.IO) {registry().lookup(user.uid,token)}
                if(accountUser?.uid==user.uid) {
                    registeredUsername=result?.username.orEmpty();usernameLoaded=true
                    store.rememberUsername(user.uid,registeredUsername)
                    profile=profile.copy(username=registeredUsername)
                    store.saveAccountProfile(user.uid,profile)
                }
            } catch(e:CancellationException){throw e}
            catch(e:Exception){if(accountUser?.uid==user.uid) usernameError=kz.youni4st.app.ui.accountErrorCode(e)}
            finally {if(accountUser?.uid==user.uid)usernameBusy=false}
        }
    }
    suspend fun reserveUsername(value:String):String {
        val user=accountUser ?: throw RegistryException("AUTH_REQUIRED")
        val token=requireNotNull(user.getIdToken(false).await().token)
        val name=withContext(Dispatchers.IO) {registry().claim(user.uid,value,token)}
        if(accountUser?.uid!=user.uid) throw RegistryException("AUTH_REQUIRED")
        registeredUsername=name;usernameLoaded=true;usernameError=""
        store.rememberUsername(user.uid,name)
        profile=profile.copy(username=name);store.profile=profile;store.mark("profile");requestCloudSync()
        return name
    }
    fun enterApp() { welcomed = true; store.welcomed = true }
    fun isSent(id: String) = receipts.optString("${accountUser?.uid}|$id").isNotBlank()
    fun submit(entry: Entry) {
        if (entry.raw.optString("source")=="Enbek" || entry.raw.optBoolean("archived") || applicationBusy != null || isSent(entry.id)) return
        applicationBusy = entry.id; applicationError = false
        viewModelScope.launch {
            try {
                val user = requireNotNull(account.auth?.currentUser)
                val token = requireNotNull(user.getIdToken(false).await().token)
                val key = "${user.uid}|${entry.id}"
                val receipt = ApplicationApi().send(config.value("applications_endpoint"), token, entry.id, resume, store.requestId(key))
                if(accountUser?.uid!=user.uid)return@launch
                receipts = org.json.JSONObject(receipts.toString()).put(key, receipt)
                if(accountUser?.uid==user.uid){store.receipts=receipts;requestCloudSync()}
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { applicationError = true }
            finally { applicationBusy = null }
        }
    }
    fun signOut() { viewModelScope.launch { account.signOut(getApplication()) } }
    var catalog by mutableStateOf<Catalog?>(null)
        private set
    var loadError by mutableStateOf(false)
        private set
    var language by mutableStateOf(Language.RU)
        private set
    var favorites by mutableStateOf(emptySet<String>())
        private set
    var drafts by mutableStateOf(emptySet<String>())
        private set
    var resume by mutableStateOf(Resume())
        private set
    var profile by mutableStateOf(UserProfile())
        private set
    suspend fun saveProfile(value: UserProfile) {
        val normalized=UsernamePolicy.normalize(value.username)
        val name=if(registeredUsername.isNotBlank() && normalized==registeredUsername)normalized else reserveUsername(value.username)
        profile=value.copy(username=name)
        store.profile=profile;requestCloudSync()
    }
    var messages by mutableStateOf(emptyList<ChatMessage>())
        private set
    var chatBusy by mutableStateOf(false)
        private set
    var chatFailed by mutableStateOf(messages.lastOrNull()?.role == "user")
        private set

    var startupReady by mutableStateOf(false)
        private set
    var startupFailed by mutableStateOf(false)
        private set
    init { initialize() }
    fun initialize() {
        startupFailed=false
        viewModelScope.launch {
            try {
                val initial=withContext(Dispatchers.IO) {StartupData(getApplication())}
                store=initial.store; config=initial.config; account=initial.account
                welcomed=initial.welcomed; receipts=initial.receipts; language=initial.language
                favorites=initial.favorites; drafts=initial.drafts; resume=initial.resume
                profile=initial.profile; messages=initial.chat
                chatFailed=!chatBusy && messages.lastOrNull()?.role=="user"
                viewModelScope.launch {
                    account.initialize(); authInitialized=true
                    account.auth?.addAuthStateListener(authListener)
                }
                loadCatalog()
                val aliases=mapOf("universities:6" to "universities:5", "universities:45" to "universities:24")
                favorites=favorites.map {aliases[it] ?: it}.toSet()
                store.favorites=favorites
                startupReady=true
                observeNetwork()
            } catch(e:CancellationException) {throw e}
            catch(_:Exception) {startupFailed=true}
        }
    }
    override fun onCleared() {
        networkCallback?.let {callback->runCatching {getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java).unregisterNetworkCallback(callback)}}
        if(::account.isInitialized) account.auth?.removeAuthStateListener(authListener); super.onCleared() }
    fun loadCatalog() {
        loadError = false
        viewModelScope.launch {
            try {
                catalog = withContext(Dispatchers.IO) { Catalog(getApplication()) }
                val aliases=mapOf("universities:6" to "universities:5", "universities:45" to "universities:24")
                favorites=favorites.map {aliases[it] ?: it}.toSet()
                store.favorites=favorites
            }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { loadError = true }
        }
    }
    fun changeLanguage(value: Language) { language = value; store.language = value;requestCloudSync() }
    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        store.favorites = favorites;requestCloudSync()
    }
    fun saveDraft(id: String) { drafts = drafts + id; store.drafts = drafts;requestCloudSync() }
    fun removeDraft(id: String) { drafts = drafts - id; store.drafts = drafts;requestCloudSync() }
    fun saveResume(value: Resume) { resume = value; store.resume = value;requestCloudSync() }
    fun clearChat() { if (!chatBusy) { messages = emptyList(); store.chat = messages; chatFailed = false;requestCloudSync() } }
    fun send(text: String, retry: Boolean = false) {
        if (chatBusy || text.isBlank()) return
        if (!retry) {
            // An unanswered prior message is retained as context for the next question.
            messages = messages + ChatMessage("user", text.trim().take(2000))
            store.chat = messages
        }
        val last = messages.lastOrNull() ?: return
        val history = messages.dropLast(1)
        chatBusy = true; chatFailed = false
        val chatUid=accountUser?.uid
        chatJob=viewModelScope.launch {
            try {
                val answer = api.reply(last.content, history)
                if(accountUser?.uid!=chatUid)return@launch
                messages = (messages + ChatMessage("assistant", answer.take(8000))).takeLast(40)
                store.chat = messages
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { chatFailed = true }
            finally {if(accountUser?.uid==chatUid){chatBusy=false;requestCloudSync()}}
        }
    }
}
