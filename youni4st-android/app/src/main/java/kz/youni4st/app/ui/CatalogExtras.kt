package kz.youni4st.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kz.youni4st.app.data.Entry
import kz.youni4st.app.data.safeKaspiUrl

@Composable
fun CatalogHero(kind:String,count:Int,onSaved:()->Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF235F85))) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White,size.width*.62f,Offset(size.width*1.25f,size.height*.6f),style=Stroke(2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth().padding(22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(when(kind){
                    "programs"->t("Образование","Білім","Education")
                    "grants"->t("Гранты","Гранттар","Grants")
                    "news"->t("Новости","Жаңалықтар","Education news")
                    else->"IELTS"
                },color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text(when(kind){
                    "programs"->t("10 компаний · 20 программ","10 компания · 20 бағдарлама","10 providers · 20 programs")
                    "grants"->t("Условия, результаты\nи возможности","Шарттар, нәтижелер\nжәне мүмкіндіктер","Requirements, results\nand opportunities")
                    "news"->t("События, которые\nоткрывают возможности","Мүмкіндік ашатын жаңалықтар","News that opens opportunities")
                    else->t("Экзамен, подготовка\nи регистрация","Емтихан, дайындық және тіркелу","Exam, preparation and booking")
                },color=Color.White.copy(alpha=.8f),fontSize=13.sp)
            }
            CountOrb(count,t("Мой список","Менің тізімім","My list"),dark=true,onClick=onSaved)
        }
    }
}

@Composable
fun ProgramActions(entry:Entry) {
    var paymentInfo by remember {mutableStateOf(false)}
    val paymentUrl=safeKaspiUrl(entry.raw.optString("kaspi_url"))
    ExternalLinkButton(entry.raw.optString("source_url"),t("Программа на сайте школы","Мектеп сайтындағы бағдарлама","Course on provider website"),entry.raw.optString("company"))
    Button({paymentInfo=true},Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFF14635),contentColor=Color.White)) {
        Icon(Icons.Outlined.Payments,null);Spacer(Modifier.width(10.dp));Text(t("Оплатить через Kaspi.kz","Kaspi.kz арқылы төлеу","Pay with Kaspi.kz"),fontWeight=FontWeight.Bold)
    }
    Text(t("Оплата после согласования со школой","Мектеппен келіскеннен кейін төлеу","Payment after confirmation with the provider"),color=Muted,fontSize=12.sp)
    if(paymentInfo) AlertDialog(onDismissRequest={paymentInfo=false},title={Text("Kaspi.kz")},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(paymentUrl!=null) {
                Text(t("Перед оплатой проверьте название получателя, программу и сумму счёта.","Төлем алдында алушыны, бағдарламаны және шот сомасын тексеріңіз.","Before paying, check the recipient, course and invoice amount."))
                ExternalLinkButton(paymentUrl,t("Открыть счёт Kaspi","Kaspi шотын ашу","Open Kaspi invoice"),note=false)
            } else {
                Text(t("У школы пока нет подтверждённой платёжной ссылки в каталоге. Уточните доступность Kaspi и запросите счёт у школы на выбранную программу.","Каталогта мектептің расталған төлем сілтемесі жоқ. Kaspi қолжетімділігін мектептен нақтылап, бағдарламаға шот сұраңыз.","This catalog has no verified payment link for this provider. Ask the school whether Kaspi is available and request an invoice for your course."))
                ExternalLinkButton(entry.raw.optString("source_url"),t("Связаться со школой","Мектеппен байланысу","Contact the school"),note=false)
            }
        }
    },confirmButton={TextButton({paymentInfo=false}) {Text(t("Понятно","Түсінікті","Got it"))}})
}
