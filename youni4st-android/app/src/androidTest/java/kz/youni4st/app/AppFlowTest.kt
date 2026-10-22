package kz.youni4st.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Пока продолжить без входа").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithContentDescription("Главная").fetchSemanticsNodes().isNotEmpty()
        }
        if(compose.onAllNodesWithText("Пока продолжить без входа").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Пока продолжить без входа").performScrollTo().performClick()
    }
    @Test fun universityFiltersAndSpecialtiesAccordion() {
        ready()
        compose.onNodeWithContentDescription("Вузы").performClick()
        compose.onNodeWithText("Введите ваш балл (0–140)").performTextInput("141")
        compose.onNodeWithText("Укажите число от 0 до 140").assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("95")
        compose.onNodeWithText("Astana IT University").performScrollTo().performClick()
        compose.onNodeWithText("Специальности ·",substring=true).performScrollTo().performClick()
        compose.onNodeWithText("Программная инженерия").performScrollTo().assertIsDisplayed()
    }
    @Test fun mentorsOpenFromHomeAndMissingLinkDoesNotPretendToBook() {
        ready()
        compose.onNodeWithText("Найди своего наставника").performScrollTo().performClick()
        compose.onNodeWithText("Алишер Рахимов").performScrollTo().performClick()
        compose.onNodeWithText("Перейти к наставнику").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Подготовить запись").assertDoesNotExist()
    }
    @Test fun favoriteUniversityCounterOpensOnlyUniversities() {
        ready()
        compose.onNodeWithContentDescription("Вузы").performClick()
        compose.onNodeWithText("Мой список").performClick()
        compose.onNodeWithText("Избранное").assertExists()
        compose.onNodeWithText("Алишер Рахимов").assertDoesNotExist()
    }
    @Test fun resumeSavesAndOffersPdfExport() {
        ready()
        compose.onNodeWithContentDescription("Профиль").performClick()
        compose.onNodeWithText("Моё резюме").performScrollTo().performClick()
        compose.onNodeWithText("ФИО").performTextReplacement("Тест Юнифорст")
        compose.onNodeWithText("Сохранить и скачать в PDF").performScrollTo().assertExists()
        compose.onNodeWithText("Сохранить",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Резюме сохранено").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Моё резюме").performScrollTo().performClick()
        compose.onNodeWithText("Тест Юнифорст").performScrollTo().assertExists()
    }
}
