package kz.youni4st.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

object SalaryInput {
    fun digits(value:String)=value.filter {it in '0'..'9'}.take(12)
    fun format(value:String)=digits(value).reversed().chunked(3).joinToString(".").reversed()
    fun cursorToFormatted(offset:Int,digits:Int):Int {
        val n=offset.coerceIn(0,digits)
        return n+(0 until n).count {i->i>0 && (digits-i)%3==0}
    }
    fun cursorToOriginal(offset:Int,value:String)=format(value).take(offset.coerceAtLeast(0)).count {it.isDigit()}
}
object ResumeDates {
    fun month(value:String)=runCatching {YearMonth.parse(value)}.getOrNull()
    fun validRange(record:ResumeRecord,today:YearMonth=YearMonth.now()):Boolean {
        val start=month(record.startMonth) ?: return false
        val end=if(record.ongoing)today else month(record.endMonth) ?: return false
        return start.year>=1900 && start<=end && end<=today
    }
    /** Inclusive calendar months; simultaneous roles are merged, not double-counted. */
    fun experienceMonths(records:List<ResumeRecord>,today:YearMonth=YearMonth.now()):Long {
        val ranges=records.filter {validRange(it,today)}.map {
            val a=month(it.startMonth)!!;val b=if(it.ongoing)today else month(it.endMonth)!!
            (a.year*12L+a.monthValue-1) to (b.year*12L+b.monthValue-1)
        }.sortedBy {it.first}
        var total=0L;var start:Long?=null;var end=0L
        for((a,b) in ranges) {
            if(start==null){start=a;end=b}
            else if(a<=end+1)end=maxOf(end,b)
            else {total+=end-start+1;start=a;end=b}
        }
        return total+if(start!=null)end-start+1 else 0L
    }
    fun duration(months:Long,lang:Language):String {
        fun ru(n:Long,a:String,b:String,c:String)=when {n%100 in 11..14->c;n%10==1L->a;n%10 in 2..4->b;else->c}
        val y=months/12;val m=months%12
        return when(lang){Language.RU->"$y ${ru(y,"год","года","лет")} $m ${ru(m,"месяц","месяца","месяцев")}";Language.KZ->"$y жыл $m ай";Language.EN->"$y ${if(y==1L)"year" else "years"} $m ${if(m==1L)"month" else "months"}"}
    }
    fun label(value:String,lang:Language):String = month(value)?.format(DateTimeFormatter.ofPattern("LLLL yyyy",locale(lang))) ?: value
    fun period(record:ResumeRecord,lang:Language):String = if(record.startMonth.isBlank())record.period else label(record.startMonth,lang)+" – "+if(record.ongoing)when(lang){Language.RU->"настоящее время";Language.KZ->"қазіргі уақыт";Language.EN->"present"} else label(record.endMonth,lang)
    fun age(value:String,today:LocalDate=LocalDate.now()):Int?=runCatching {val d=LocalDate.parse(value,PersonalValidation.dateFormat);if(d.isAfter(today))null else Period.between(d,today).years}.getOrNull()
    fun birthday(value:String,lang:Language):String=runCatching {LocalDate.parse(value,PersonalValidation.dateFormat).format(DateTimeFormatter.ofPattern(if(lang==Language.EN)"MMMM d" else "d MMMM",locale(lang)))}.getOrDefault(value)
    private fun locale(lang:Language)=Locale.forLanguageTag(when(lang){Language.RU->"ru";Language.KZ->"kk";Language.EN->"en"})
}
