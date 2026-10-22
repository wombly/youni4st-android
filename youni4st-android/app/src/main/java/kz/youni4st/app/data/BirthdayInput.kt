package kz.youni4st.app.data

object BirthdayInput {
    fun format(input:String):String {
        val digits=input.filter(Char::isDigit).take(8)
        return buildString {digits.forEachIndexed {i,c -> if(i==2 || i==4) append('.');append(c)}}
    }
}
