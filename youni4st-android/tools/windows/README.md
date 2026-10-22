# Локальная диагностика AAPT2

Обычная сборка проекта использует официальный AAPT2 из Android Gradle Plugin. Этот файл не подключён к Gradle и не нужен для стандартной установки Android Studio.

В среде, где создавался прототип, Windows-версия AAPT2 завершалась с `0xc0000005` после записи ресурсов, перед завершением ZIP. Сбой воспроизводился и на пустом приложении. `AaptBridge.cs` позволил продолжить локальную сборку: он запускает официальный AAPT2 и только при этом конкретном сбое восстанавливает индекс ZIP, если все локальные записи полностью записаны и их размеры и CRC32 совпадают. Данные ресурсов не изменяются. Повреждённые и неполные файлы отвергаются.

Исходник оставлен для воспроизводимости диагностики. Это временный обход сбоя окружения, а не часть Android-приложения. Он не исправляет произвольные ошибки AAPT2. В обычной среде его использовать не следует.

Для повторения диагностики на Windows с .NET Framework:

```powershell
& "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:exe /out:aapt2.exe /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll AaptBridge.cs
$env:YOUNI_REAL_AAPT2 = 'C:\absolute\path\to\official\aapt2.exe'
$env:YOUNI_AAPT_LOG = 'C:\absolute\path\to\diagnostic.log'
# Запустить из корня Android-проекта, указав абсолютный путь к собранной обёртке:
.\gradlew.bat :app:assembleDebug '-Pandroid.aapt2FromMavenOverride=C:/absolute/path/to/bridge/aapt2.exe'
```

Оригинальный и промежуточный `aapt2.exe` должны лежать в разных папках. При воспроизведении нужно проверять итоговый APK через `apksigner verify`, Android resource dump и запуск на устройстве.
