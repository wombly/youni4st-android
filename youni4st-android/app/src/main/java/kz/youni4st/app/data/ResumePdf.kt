package kz.youni4st.app.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import kz.youni4st.app.R
import java.io.OutputStream

/** Independent column pagination preserves readable text even in very long resumes. */
object ResumePdf {
    private const val BOTTOM=802f
    private val sidebar=Color.rgb(0,185,205)
    private val ink=Color.rgb(62,62,62)
    private class Pages {
        val commands=mutableListOf(mutableListOf<(Canvas)->Unit>())
        fun add(page:Int,draw:(Canvas)->Unit) {
            while(commands.size<=page)commands.add(mutableListOf())
            commands[page].add(draw)
        }
    }
    private class Column(private val pages:Pages,private val x:Float,private val width:Int,private val color:Int,start:Float) {
        var page=0
        var y=start
        private fun room(height:Float) {if(y+height>BOTTOM){page++;y=76f}}
        fun gap(value:Float=10f){y+=value}
        fun heading(value:String) {
            room(64f)
            text(value,13f,bold=true,gap=6f)
            val lineY=y
            pages.add(page) {c->c.drawLine(x,lineY,x+width,lineY,Paint(Paint.ANTI_ALIAS_FLAG).apply {this.color=this@Column.color;strokeWidth=.8f})}
            y+=14f
        }
        fun text(value:String,size:Float=10.5f,bold:Boolean=false,muted:Boolean=false,gap:Float=8f,timeline:Boolean=false) {
            if(value.isBlank())return
            val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize=size;this.color=if(muted && this@Column.color==Color.WHITE)Color.rgb(215,215,215) else this@Column.color
                typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
            }
            val indent=if(timeline)18 else 0
            val layout=StaticLayout.Builder.obtain(value,0,value.length,paint,width-indent).setIncludePad(false).setLineSpacing(3f,1f).build()
            var first=0
            while(first<layout.lineCount) {
                room((layout.getLineBottom(first)-layout.getLineTop(first)).toFloat())
                val lineTop=layout.getLineTop(first)
                var end=first
                while(end+1<layout.lineCount && y+layout.getLineBottom(end+1)-lineTop<=BOTTOM)end++
                val height=(layout.getLineBottom(end)-lineTop).toFloat()
                val top=y;val firstLine=first==0
                pages.add(page) {canvas ->
                    canvas.save();canvas.clipRect(x+indent,top,x+width,top+height)
                    canvas.translate(x+indent,top-lineTop);layout.draw(canvas);canvas.restore()
                    if(timeline) {
                        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {this.color=ink;strokeWidth=.8f;style=Paint.Style.STROKE}
                        canvas.drawLine(x+4,top+4,x+4,(top+height+gap).coerceAtMost(BOTTOM),p)
                        if(firstLine && bold) {canvas.drawCircle(x+4,top+5,3f,Paint(Paint.ANTI_ALIAS_FLAG).apply {this.color=Color.WHITE});canvas.drawCircle(x+4,top+5,3f,p)}
                    }
                }
                y+=height;first=end+1
            }
            y+=gap
        }
        fun experience(record:ResumeRecord,student:String) {
            room(72f)
            text(record.title,12f,bold=true,gap=4f,timeline=true)
            text(listOf(record.subtitle,record.period,if(record.ongoing)student else "").filter {it.isNotBlank()}.joinToString(" · "),10f,gap=8f,timeline=true)
            text(record.details,10.5f,gap=16f,timeline=true)
            gap(8f)
        }
    }

    fun write(context:Context,resume:Resume,language:Language,output:OutputStream) {
        fun label(ru:String,kz:String,en:String)=when(language){Language.RU->ru;Language.KZ->kz;Language.EN->en}
        val pages=Pages()
        val photo=loadPhoto(context,resume.photo)
        val logo=BitmapFactory.decodeResource(context.resources,R.drawable.brand_logo)
        val logoBounds=visibleBounds(logo)
        val refs=ResumeReference(context)
        val left=Column(pages,26f,158,Color.rgb(25,58,82),if(photo!=null)193f else 75f)
        val right=Column(pages,239f,328,ink,84f)
        val name=resume.name.ifBlank {label("Моё резюме","Менің түйіндемем","My resume")}
        right.text(name,24f,bold=true,gap=10f)
        val demographic=mutableListOf<String>()
        if(resume.gender.isNotBlank())demographic+=label("Пол: ","Жынысы: ","Gender: ")+resume.gender
        ResumeDates.age(resume.birthday)?.let {demographic+=label("Возраст: ","Жасы: ","Age: ")+it}
        if(resume.birthday.isNotBlank())demographic+=label("Дата рождения: ","Туған күні: ","Birthday: ")+ResumeDates.birthday(resume.birthday,language)
        right.text(demographic.joinToString(" · "),10f,gap=12f)
        if(resume.position.isNotBlank())right.text(label("Желаемая должность: ","Қалаған лауазым: ","Desired position: ")+resume.position,12f,bold=true,gap=18f)
        val contacts=listOf(
            label("Телефон: ","Телефон: ","Phone: ") to PersonalValidation.formattedPhone(resume.phone),
            label("Почта: ","Пошта: ","Email: ") to resume.email,
            label("Город: ","Қала: ","City: ") to resume.city).filter {it.second.isNotBlank()}.map {it.first+it.second}
        if(contacts.isNotEmpty()) {left.heading(label("КОНТАКТЫ","БАЙЛАНЫС","CONTACT"));contacts.forEach {left.text(it,10f,muted=true)};left.gap(14f)}
        if(resume.salary.isNotBlank()) {left.heading(label("Желаемая зарплата","Қалаған жалақы","Expected salary"));left.text(SalaryInput.format(resume.salary)+" ₸");left.gap(14f)}
        if(resume.employmentTypes.isNotEmpty()) {left.heading(label("Тип занятости","Жұмыспен қамту","Employment type"));left.text(ResumeReference.employment(resume.employmentTypes,language));left.gap(12f)}
        if(resume.allSkills.isNotEmpty()){left.heading(label("Навыки","Дағдылар","Skills"));resume.allSkills.forEach {left.text(it,10.5f,gap=5f)};left.gap(12f)}
        if(resume.languages.isNotEmpty()) {left.heading(label("Языки","Тілдер","Languages"));resume.languages.forEach {left.text(listOf(it.title,ResumeReference.level(it.subtitle,language)).filter {s->s.isNotBlank()}.joinToString(" · "),10f,muted=true)};left.gap(12f)}
        if(resume.interests.isNotEmpty()){left.heading(label("Интересы","Қызығушылықтар","Interests"));left.text(resume.interests.joinToString("\n"),10f,muted=true)}
        if(resume.about.isNotBlank()){right.heading(label("Обо мне","Өзім туралы","Career summary"));right.text(resume.about);right.gap(20f)}
        if(resume.experience.isNotBlank() || resume.work.isNotEmpty()) {
            right.heading(label("Опыт работы","Жұмыс тәжірибесі","Professional experience"))
            right.text(resume.experience,gap=20f,timeline=true)
            val months=ResumeDates.experienceMonths(resume.work)
            if(resume.work.any {ResumeDates.validRange(it)})right.text(label("Общий опыт: ","Жалпы өтілі: ","Total experience: ")+ResumeDates.duration(months,language),11f,bold=true,gap=16f)
            resume.work.forEach {record->
                val industry=listOf(refs.industry(record.industry,language),record.specialization).filter(String::isNotBlank).joinToString(" · ")
                val details=listOf(industry,record.details,if(record.student)label("Студент","Студент","Student") else "").filter(String::isNotBlank).joinToString("\n")
                right.experience(record.copy(period=ResumeDates.period(record,language),details=details,ongoing=false),"")
            }
        }
        if(resume.studies.isNotEmpty() || resume.education.isNotBlank() || resume.educationLevel.isNotBlank()) {
            right.heading(label("Образование","Білім","Education"))
            right.text(resume.educationLevel,10f,muted=true)
            right.text(resume.education,10.5f)
            resume.studies.forEach {
                right.text(it.details.ifBlank {it.title},11f,bold=true,gap=4f)
                right.text(listOf(if(it.details.isNotBlank())it.title else "",it.subtitle,it.period,if(it.ongoing)label("Сейчас обучаюсь","Қазір оқимын","Currently studying") else "").filter {s->s.isNotBlank()}.joinToString("\n"),10f,muted=true,gap=18f)
            }
            right.gap()
        }
        val document=PdfDocument()
        var active:PdfDocument.Page?=null
        try {
            pages.commands.forEachIndexed {index,commands ->
                val page=document.startPage(PdfDocument.PageInfo.Builder(595,842,index+1).create());active=page
                val canvas=page.canvas
                canvas.drawColor(Color.WHITE)
                canvas.drawRect(0f,0f,210f,842f,Paint().apply {color=sidebar})
                val logoWidth=94f;val logoHeight=logoWidth*logoBounds.height()/logoBounds.width()
                canvas.drawBitmap(logo,logoBounds,RectF(473f,24f,567f,24f+logoHeight),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {alpha=210})
                if(index==0 && photo!=null) {
                    val edge=minOf(photo.width,photo.height);val sx=(photo.width-edge)/2;val sy=(photo.height-edge)/2
                    canvas.save();canvas.clipPath(Path().apply {addCircle(105f,105f,60f,Path.Direction.CW)})
                    canvas.drawBitmap(photo,Rect(sx,sy,sx+edge,sy+edge),RectF(45f,45f,165f,165f),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));canvas.restore()
                }
                commands.forEach {it(canvas)}
                val footer=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.GRAY;textSize=8f;typeface=Typeface.create("sans-serif",Typeface.NORMAL)}
                canvas.drawText("${index+1} / ${pages.commands.size}",544f,824f,footer)
                document.finishPage(page);active=null
            }
            document.writeTo(output)
        } finally {active?.let {document.finishPage(it)};document.close();photo?.recycle();logo.recycle()}
    }

    private fun loadPhoto(context:Context,value:String):Bitmap? = if(value.isBlank())null else runCatching {
        val uri=Uri.parse(value)
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
        val options=BitmapFactory.Options().apply {inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/480).coerceAtLeast(1)}
        context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,options)}
    }.getOrNull()

    private fun visibleBounds(bitmap:Bitmap):Rect {
        var left=bitmap.width;var top=bitmap.height;var right=0;var bottom=0
        val row=IntArray(bitmap.width)
        for(y in 0 until bitmap.height){bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1);for(x in row.indices)if(Color.alpha(row[x])>12){left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)}}
        return if(left>right)Rect(0,0,bitmap.width,bitmap.height) else Rect(left,top,right+1,bottom+1)
    }
}
