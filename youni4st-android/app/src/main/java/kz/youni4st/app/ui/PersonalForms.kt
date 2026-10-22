@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package kz.youni4st.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import kz.youni4st.app.data.*
import java.io.File
import java.time.LocalDate

val FormBackground=Color(0xFFF6F6FC)

@Composable
fun PersonalField(label:String,value:String,onChange:(String)->Unit,icon:ImageVector?=null,
    keyboard:KeyboardType=KeyboardType.Text,multiline:Boolean=false,error:Boolean=false,phone:Boolean=false,salary:Boolean=false) {
    OutlinedTextField(if(phone) phoneDigits(value) else if(salary) SalaryInput.digits(value) else value, { input ->
        onChange(if(phone) { val digits=editPhoneDigits(phoneDigits(value),input); if(digits.isBlank()) "" else "+7$digits" } else if(salary) SalaryInput.digits(input) else input.take(if(multiline) 4000 else 200))
    },Modifier.fillMaxWidth(),label={Text(label,fontSize=13.sp)},
        leadingIcon=if(icon!=null) {{Icon(icon,null,Modifier.size(22.dp))}} else null,
        singleLine=!multiline,minLines=if(multiline) 4 else 1,maxLines=if(multiline) 10 else 1,
        visualTransformation=if(phone) PhoneMask() else if(salary) SalaryMask() else VisualTransformation.None,
        keyboardOptions=KeyboardOptions(capitalization=if(!phone && keyboard==KeyboardType.Text && !label.contains("@")) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,keyboardType=if(phone) KeyboardType.Phone else keyboard,imeAction=if(multiline) ImeAction.Default else ImeAction.Next),
        isError=error,shape=RoundedCornerShape(19.dp),
        colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Black,focusedBorderColor=Color.Black,
            focusedContainerColor=FormBackground,unfocusedContainerColor=FormBackground,focusedLabelColor=Ink,unfocusedLabelColor=Muted))
}

@Composable
fun PersonalPhoto(photo:String,onChange:(String)->Unit,onBusy:(Boolean)->Unit={},label:String=t("Фото для резюме","Түйіндеме суреті","Resume photo")) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri!=null) scope.launch {
            busy=true;onBusy(true);failed=false
            try {
                val saved=withContext(Dispatchers.IO) {
                    val file=File(context.filesDir,"personal-photo-${System.nanoTime()}")
                    try { context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: error("Unreadable photo") }
                    catch(e:Exception) {file.delete();throw e}
                    Uri.fromFile(file).toString()
                }
                onChange(saved)
            } catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true}
            finally {busy=false;onBusy(false)}
        }
    }
    Column(Modifier.dismissKeyboardOnBackgroundTap().fillMaxWidth().padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Box {
            Surface(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,
                modifier=Modifier.size(112.dp),shape=CircleShape,color=Color.White,border=BorderStroke(1.dp,Color(0xFFE0E3EA))) {
                if(photo.isNotBlank()) AsyncImage(photo,label,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.PersonOutline,null,Modifier.size(48.dp),tint=Navy) }
            }
            Box(Modifier.align(Alignment.BottomEnd).size(32.dp).background(Navy,CircleShape).border(2.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) {
                if(busy) CircularProgressIndicator(Modifier.size(16.dp),color=Color.White,strokeWidth=2.dp)
                else Icon(Icons.Outlined.PhotoCamera,null,Modifier.size(17.dp),tint=Color.White)
            }
        }
        Text(label,color=Muted,fontSize=13.sp)
        if(photo.isNotBlank()) TextButton({onChange("")},enabled=!busy) {Text(t("Убрать фото","Фотоны жою","Remove photo"))}
        if(failed) Text(t("Не удалось открыть фото","Фото ашылмады","Could not open photo"),color=MaterialTheme.colorScheme.error)
    }
}

@Composable
fun PersonalSheet(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=Color(0xFFFAF6FC),shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
        Column(Modifier.dismissKeyboardOnBackgroundTap().fillMaxWidth().heightIn(max=660.dp).verticalScroll(rememberScrollState()).padding(start=22.dp,end=22.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,style=MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

@Composable
fun ChoiceField(label:String,value:String,options:List<String>,onChange:(String)->Unit,icon:ImageVector) {
    var open by rememberSaveable {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,Modifier.padding(start=12.dp),fontSize=13.sp,color=Muted)
        Surface(onClick={open=true},shape=RoundedCornerShape(19.dp),color=FormBackground,border=BorderStroke(1.dp,Color.Black)) {
            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(icon,null,Modifier.size(22.dp));Text(value.ifBlank {t("Выбрать","Таңдау","Choose")},Modifier.weight(1f));Icon(Icons.Outlined.ExpandMore,null)
            }
        }
    }
    if(open) PersonalSheet(label,{open=false}) {
        options.forEach { option -> TextButton({onChange(option);open=false},Modifier.fillMaxWidth()) {Text(option,Modifier.weight(1f));if(option==value) Icon(Icons.Outlined.Check,null)} }
    }
}

@Composable
fun BirthdayField(value:String,onChange:(String)->Unit) {
    OutlinedTextField(value.filter(Char::isDigit),{input -> onChange(BirthdayInput.format(input))},
        Modifier.fillMaxWidth(),label={Text(t("Дата рождения","Туған күні","Date of birth"))},
        placeholder={Text("ДД.ММ.ГГГГ")},supportingText={Text(t("Например: 25.08.2005","Мысалы: 25.08.2005","Example: 25.08.2005"))},
        leadingIcon={Icon(Icons.Outlined.Cake,null)},singleLine=true,
        visualTransformation=BirthdayMask(),
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
        isError=value.isNotBlank() && !PersonalValidation.birthday(value),shape=RoundedCornerShape(19.dp),
        colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Black,focusedBorderColor=Color.Black,
            focusedContainerColor=FormBackground,unfocusedContainerColor=FormBackground))
}

private class BirthdayMask:VisualTransformation {
    override fun filter(text:androidx.compose.ui.text.AnnotatedString):TransformedText {
        val formatted=BirthdayInput.format(text.text)
        val mapping=object:OffsetMapping {
            override fun originalToTransformed(offset:Int):Int = (offset + (if(offset>2) 1 else 0) + (if(offset>4) 1 else 0)).coerceAtMost(formatted.length)
            override fun transformedToOriginal(offset:Int):Int = (offset - (if(offset>2) 1 else 0) - (if(offset>5) 1 else 0)).coerceIn(0,text.length)
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(formatted),mapping)
    }
}
