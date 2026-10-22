package kz.youni4st.app

import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.youni4st.app.ui.*
import kz.youni4st.app.data.Language
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private var stoppedAt:Long?=null
    private val restartSignal=mutableIntStateOf(0)
    override fun onStop() {
        if(!isChangingConfigurations) stoppedAt=SystemClock.elapsedRealtime()
        StartupHaptics.cancel(this)
        super.onStop()
    }
    override fun onStart() {
        super.onStart()
        stoppedAt?.let {if(StartupSettings.shouldReplay(SystemClock.elapsedRealtime()-it)) restartSignal.intValue++}
        stoppedAt=null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge()
        if(savedInstanceState==null) {
            setContentView(EarlySplashView(this) {at->showCompose(at)})
        } else showCompose(null)
    }
    private fun showCompose(startedAt:Long?) {
        setContent {
            val appVm:AppViewModel = viewModel()
            var introDone by rememberSaveable { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                if(introDone && appVm.startupReady) key(appVm.accountUser?.uid) {YouniApp(appVm,restartSignal.intValue,initialIntroDone=true)}
                if(!introDone || !appVm.startupReady) CompositionLocalProvider(LocalLanguage provides appVm.language) {
                    YouniTheme {
                        BrandIntro(startedAt=startedAt,playHaptics=false) {introDone=true}
                        if(appVm.startupFailed) androidx.compose.material3.AlertDialog(
                            onDismissRequest={},
                            title={androidx.compose.material3.Text(t("Не удалось загрузить данные","Деректер жүктелмеді","Could not load data"))},
                            confirmButton={androidx.compose.material3.TextButton(onClick={appVm.initialize()}) {androidx.compose.material3.Text(t("Повторить","Қайталау","Retry"))}}
                        )
                    }
                }
            }
        }
    }
}
