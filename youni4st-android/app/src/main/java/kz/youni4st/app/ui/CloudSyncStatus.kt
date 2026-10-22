package kz.youni4st.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.youni4st.app.AppViewModel

@Composable
fun CloudSyncStatus(vm:AppViewModel) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        val text=when {
            vm.accountUser==null->t("Войди в аккаунт, чтобы сохранять данные в облаке и открывать их на другом телефоне.","Деректерді бұлтта сақтап, басқа телефонда ашу үшін аккаунтқа кір.","Sign in to back up your data and open it on another phone.")
            vm.cloudBusy->t("Синхронизируем данные…","Деректер синхрондалуда…","Syncing your data…")
            vm.cloudError in setOf("PERMISSION_DENIED","NOT_FOUND")->t("Данные сохранены на телефоне. Для облачной копии нужно обновить правила Firestore.","Деректер телефонда сақталған. Бұлт үшін Firestore ережелерін жаңарту қажет.","Data is saved on this phone. Update Firestore rules to enable cloud backup.")
            vm.cloudError=="CLOUD_DATA_TOO_LARGE"->t("Данные сохранены на телефоне, но превышают размер облачной копии. Обратись в поддержку.","Деректер телефонда сақталған, бірақ бұлттық көшірме тым үлкен. Қолдауға хабарлас.","Your data is saved locally but exceeds the cloud document size. Contact support.")
            vm.cloudError.startsWith("CLOUD_PHOTO")->t("Данные сохранены на телефоне. Выбери фото заново, чтобы отправить его в облако.","Деректер телефонда сақталған. Бұлтқа жіберу үшін фотоны қайта таңда.","Data is saved locally. Select your photo again to upload it to the cloud.")
            vm.cloudError.isNotBlank() || vm.cloudHasPending->t("Изменения сохранены на телефоне и ожидают отправки в облако. Проверь интернет.","Өзгерістер телефонда сақталған және бұлтқа жіберуді күтуде. Интернетті тексер.","Changes are saved on this phone and are waiting to sync. Check your connection.")
            vm.cloudLastSaved>0->t("Данные синхронизированы с аккаунтом. На другом устройстве войди в этот же аккаунт.","Деректер аккаунтпен синхрондалған. Басқа құрылғыда осы аккаунтқа кір.","Your data is synced. Sign in to the same account on another device.")
            else->t("Готовим облачную копию…","Бұлттық көшірме дайындалуда…","Preparing cloud backup…")
        }
        Text(text,color=Muted,fontSize=12.sp)
        if(vm.accountUser!=null) TextButton({vm.requestCloudSync(pull=true)},enabled=!vm.cloudBusy) {Text(t("Синхронизировать","Синхрондау","Sync now"))}
    }
}
