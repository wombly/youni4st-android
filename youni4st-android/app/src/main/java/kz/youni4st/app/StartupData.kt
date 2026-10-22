package kz.youni4st.app

import android.content.Context
import kz.youni4st.app.data.*

/** Construct only on Dispatchers.IO: no disk or Firebase initialization on the animation thread. */
internal class StartupData(context: Context) {
    val store=LocalStore(context)
    val config=AppConfig(context)
    val account=AccountService(context,config)
    val welcomed=store.welcomed
    val receipts=store.receipts
    val language=store.language
    val favorites=store.favorites
    val drafts=store.drafts
    val resume=store.resume
    val profile=store.profile
    val chat=store.chat
}
