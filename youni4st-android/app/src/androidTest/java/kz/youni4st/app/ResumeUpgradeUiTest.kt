package kz.youni4st.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ResumeUpgradeUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun skillsAndEmploymentCanBeEditedInTheResume() {
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("Пока продолжить без входа").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithContentDescription("Профиль").fetchSemanticsNodes().isNotEmpty()
        }
        if(compose.onAllNodesWithText("Пока продолжить без входа").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Пока продолжить без входа").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Профиль").performClick()
        compose.onNodeWithText("Моё резюме").performScrollTo().performClick()
        compose.onNodeWithText("ФИО").performTextReplacement("Тестовый пользователь")
        compose.onNodeWithText("Желаемая зарплата, ₸").performScrollTo().performTextReplacement("1000000")
        compose.onNodeWithText("Тип занятости").performScrollTo()
        compose.onAllNodesWithText("Выбрать").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Полная занятость").performClick()
        compose.onNodeWithText("Удалённая работа").performClick()
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithContentDescription("Добавить навык").performScrollTo().performClick()
        compose.onNodeWithText("Название навыка").performTextReplacement("SQL")
        compose.onNodeWithText("Добавить").performClick()
        compose.onNodeWithText("SQL").assertExists()
        compose.onNodeWithText("Сохранить резюме").performScrollTo().performClick()
        compose.onNodeWithText("Резюме сохранено").performScrollTo().assertExists()
    }
}
