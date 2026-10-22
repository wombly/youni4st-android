package kz.youni4st.app.ui
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast
import kz.youni4st.app.R
import kz.youni4st.app.StartupSettings
import kz.youni4st.app.StartupHaptics

/** Native preview and first frame share a background and centred Y4 mark. */
@Composable
fun BrandIntro(startedAt:Long?=null,playHaptics:Boolean=true,onFinished:()->Unit) {
    val progress=remember {Animatable(0f)}
    val done by rememberUpdatedState(onFinished)
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val vibrator=remember(context) {if(Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else context.getSystemService(Vibrator::class.java)}
    DisposableEffect(lifecycle,vibrator) {
        val observer=LifecycleEventObserver {_,event -> if(event==Lifecycle.Event.ON_STOP) vibrator?.cancel()}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer);vibrator?.cancel()}
    }
    LaunchedEffect(Unit) {
        lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {}
        withFrameNanos {}
        if(playHaptics)StartupHaptics.play(context)
        val elapsed=startedAt?.let {(android.os.SystemClock.elapsedRealtime()-it).coerceAtLeast(0)} ?: 0L
        val initial=(elapsed.toFloat()/StartupSettings.INTRO_DURATION_MS).coerceIn(0f,1f)
        progress.snapTo(initial)
        progress.animateTo(1f,tween((StartupSettings.INTRO_DURATION_MS-elapsed).coerceAtLeast(1).toInt(),easing=FastOutSlowInEasing))
        done()
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF080D17)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p=progress.value
            val center=Offset(size.width*.5f,size.height*.5f)
            val radius=size.width*.75f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF00B9CD).copy(alpha=.19f*p),Color.Transparent),center,radius),radius,center)
            val orbit=226.dp.toPx()
            drawArc(Color(0xFF25C1D2).copy(alpha=.35f*p),215f+12f*p,92f,false,
                Offset(center.x-orbit/2,center.y-orbit/2),Size(orbit,orbit),style=Stroke(1.dp.toPx()))
            val wide=orbit+36.dp.toPx()
            drawArc(Color.White.copy(alpha=.12f*p),36f-10f*p,68f,false,
                Offset(center.x-wide/2,center.y-wide/2),Size(wide,wide),style=Stroke(1.dp.toPx()))
        }
        Image(painterResource(R.drawable.app_icon),"Youni4st",
            Modifier.size(160.dp).graphicsLayer {scaleX=1f+.025f*progress.value;scaleY=scaleX}.clip(RoundedCornerShape(36.dp)))
        Column(Modifier.align(Alignment.Center).offset(y=156.dp).padding(horizontal=26.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("YOUNI4ST",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Medium,letterSpacing=4.sp)
            Text(t("Твой путь к будущему","Болашаққа апарар жолың","Your path to what’s next"),color=Color(0xFFA5C4D4),fontSize=14.sp,textAlign=TextAlign.Center)
        }
    }
}
