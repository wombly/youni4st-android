# Где редактировать

Пути относительно корня проекта `youni4st-android`.

- Данные вузов, контакты, названия, наличие общежитий/кафедр: `app/src/main/assets/universities_db.json`.
- География, основные направления и дочерние специальности: `app/src/main/assets/university_search.json`.
- Логотипы: `app/src/main/assets/images/universities/`. В поле `logo_url` укажите `file:///android_asset/images/universities/имя.png`; `logo_source` хранит первоисточник.
- Поле `min_score` допускает null: не подставляйте 0 вместо неизвестного порога. Значение относится к вузу в целом, поэтому используйте только подтверждённый общий минимум.
- Поле `has_dormitory`/`has_military_dept` согласуйте со ссылкой `dormitory_source`/`military_source`. false при пустой ссылке означает отсутствие подтверждения.
- Фильтры/связи города и области: `data/UniversityDirectory.kt`.
- Экран и шрифты кнопок главной страницы: `ui/CatalogScreens.kt`, функция HomeScreen, `Text(titles[i], fontSize=22.sp)`; подписи `Text(subtitles[i], fontSize=14.sp, color=Color.Black)`.
- Выбор города/специальности: `ui/DirectoryPicker.kt`; список программ внутри вуза: `ui/UniversityPrograms.kt`.
- Настройка заставки и таймера возврата: `StartupSettings.kt`: `INTRO_DURATION_MS` и `RESTART_AFTER_BACKGROUND_MS`.
- Мозаика и вибрация: `ui/BrandIntro.kt`. При замене splash_logo.png пересчитайте его alpha bounds (сейчас x=25,y=534,w=1242,h=230).
- Начальное окно Android: `res/drawable/launch_background.xml`, `res/drawable/launch_icon.xml`, `res/values-v31/styles.xml` и MainActivity.

Таймер возврата повторяет заставку и перечитывает локальный каталог, сохраняя открытый экран и введённые данные. Он не загружает свежие данные вузов из интернета: каталог обновляется новой версией приложения.
