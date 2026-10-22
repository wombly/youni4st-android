package kz.youni4st.app

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View

/** First app frame is a lightweight native View: Compose/Firebase/catalog start afterwards. */
class EarlySplashView(context:Context,private val onShown:(Long)->Unit):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val logo=BitmapFactory.decodeResource(resources,R.drawable.app_icon)
    private val density=resources.displayMetrics.density
    private val scaledDensity=resources.displayMetrics.scaledDensity
    private var shown=false
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(8,13,23))
        val side=160*density
        val rect=RectF((width-side)/2,(height-side)/2,(width+side)/2,(height+side)/2)
        val path=Path().apply {addRoundRect(rect,36*density,36*density,Path.Direction.CW)}
        canvas.save();canvas.clipPath(path);canvas.drawBitmap(logo,null,rect,paint);canvas.restore()
        paint.color=Color.WHITE;paint.textSize=25*scaledDensity;paint.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        val title="YOUNI4ST";val spacing=4*scaledDensity
        val titleWidth=title.sumOf {paint.measureText(it.toString()).toDouble()}.toFloat()+spacing*(title.length-1)
        var x=(width-titleWidth)/2
        val center=height/2f+141*density
        val baseline=center-(paint.ascent()+paint.descent())/2
        title.forEach {c->val text=c.toString();canvas.drawText(text,x,baseline,paint);x+=paint.measureText(text)+spacing}
        if(!shown) {
            shown=true
            val at=SystemClock.elapsedRealtime()
            post {
                if((context as? MainActivity)?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)==true)StartupHaptics.play(context)
                onShown(at)
            }
        }
    }
}
