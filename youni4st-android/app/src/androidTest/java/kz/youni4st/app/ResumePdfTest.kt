package kz.youni4st.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import kz.youni4st.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ResumePdfTest {
    @Test fun representativeResumeUsesTheNewLayout() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val photo=Bitmap.createBitmap(180,180,Bitmap.Config.ARGB_8888)
        photo.eraseColor(Color.rgb(35,95,133))
        val photoFile=File(context.cacheDir,"preview-photo.png")
        photoFile.outputStream().use {photo.compress(Bitmap.CompressFormat.PNG,100,it)};photo.recycle()
        val resume=Resume(name="Айгерім Нұрланова",phone="+77011234567",city="Алматы",email="aigerim@example.com",
            position="Менеджер проектов",birthday="12.10.2000",gender="Женский",salary="1000000",photo=android.net.Uri.fromFile(photoFile).toString(),
            about="Развиваю образовательные продукты. Организую работу команды, исследую потребности пользователей и довожу проекты до результата.",
            skillItems=listOf("Управление проектами","Excel и аналитика","Переговоры","Работа в команде"),employmentTypes=listOf("full_time","remote"),
            studies=listOf(ResumeRecord("Казахский национальный университет имени аль-Фараби","Менеджмент","2022","Бакалавр")),
            languages=listOf(ResumeRecord("Казахский","native"),ResumeRecord("Английский","B1"),ResumeRecord("Польский","A2"),ResumeRecord("Русский","C2")),
            work=listOf(ResumeRecord(title="Менеджер проектов",subtitle="Образовательная компания",details="Планирование сроков и бюджета. Координация команды из пяти человек. Запуск трёх учебных программ.",industry="industry_19",specialization="Онлайн-образование",startMonth="2022-09",ongoing=true)))
        val file=File(context.cacheDir,"resume-example.pdf")
        file.outputStream().use {ResumePdf.write(context,resume,Language.RU,it)}
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use {pdf->
            assertEquals(1,pdf.pageCount)
            pdf.openPage(0).use {page->
                val bitmap=Bitmap.createBitmap(1190,1684,Bitmap.Config.ARGB_8888)
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                assertEquals(Color.rgb(0,185,205),bitmap.getPixel(10,1000))
                File(context.cacheDir,"resume-example.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }
    }

    @Test fun longCyrillicResumeCreatesReadableMultiplePages() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"resume-layout-test.pdf")
        val resume=Resume(name="Айгерім Нұрланова",phone="+7 (701) 123-45-67",city="Алматы",
            education="Қазақ ұлттық университеті · Информатика",skills="Kotlin, Figma, қазақ тілі, русский, English",
            experience=(1..90).joinToString("\n") {"Проект $it — исследование задач пользователей, разработка и проверка результата."},
            studies=(1..25).map {ResumeRecord("Университет $it","Информатика","2020 - 2024","Бакалавриат")},
            work=listOf(ResumeRecord("Разработчик","Компания","2024 - сейчас","Разработка мобильных приложений")),
            languages=listOf(ResumeRecord("Қазақша","C2")),interests=listOf("Образование"),about="Помогаю создавать доступные цифровые продукты.")
        file.outputStream().use {ResumePdf.write(context,resume,Language.RU,it)}
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use {pdf ->
            assertTrue(pdf.pageCount>1)
            for(i in 0 until pdf.pageCount) pdf.openPage(i).use { page ->
                assertEquals(595,page.width);assertEquals(842,page.height)
                val bitmap=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                assertNotEquals(Color.WHITE,bitmap.getPixel(10,10))
                // Sidebar and brand survive every independently paginated continuation.
                assertEquals(Color.rgb(0,185,205),bitmap.getPixel(10,500))
                var branded=false
                for(y in 24..48) for(x in 473..566) if(bitmap.getPixel(x,y)!=Color.WHITE) branded=true
                assertTrue("Missing brand on page ${i+1}",branded)
                File(context.cacheDir,"resume-layout-page-${i+1}.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }
    }
}
