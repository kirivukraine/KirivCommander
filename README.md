# KirivCommander

**Сучасний двопанельний файловий менеджер для Android**
*by KirivSoft / Іван Кіранчук*

![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## Автоматична збірка на GitHub

### Крок 1 — Створення репозиторію

1. Зайдіть на [github.com](https://github.com) → **New repository**
2. Назва: `KirivCommander`
3. Visibility: **Private** (рекомендовано) або Public
4. **НЕ** ставте галочку "Add README" — репозиторій має бути порожнім
5. Натисніть **Create repository**

### Крок 2 — Завантаження проекту

```bash
# Розпакуйте ZIP у зручну папку, потім:
cd KirivCommander

git init
git add .
git commit -m "Initial commit: KirivCommander v1.0.0"

# Замініть YOUR_USERNAME на ваш GitHub логін
git remote add origin https://github.com/YOUR_USERNAME/KirivCommander.git
git branch -M main
git push -u origin main
```

### Крок 3 — GitHub Actions збере APK автоматично

Після `git push` відкрийте вкладку **Actions** у репозиторії.
Там побачите workflow **"Build KirivCommander APK"** — він запустився автоматично.

Час збірки: ~5-8 хвилин.

### Крок 4 — Завантаження APK

Після успішної збірки:
1. Actions → виберіть останній run
2. Прокрутіть до розділу **Artifacts**
3. Завантажте `KirivCommander-debug` або `KirivCommander-release-unsigned`

---

## Функції

| Модуль | Можливості |
|--------|-----------|
| **Файловий менеджер** | Дві незалежні панелі, breadcrumb, сортування (7 режимів), множинний вибір |
| **Операції з файлами** | Копіювання, переміщення, видалення, перейменування, архівування ZIP, розрахунок MD5/SHA256 |
| **Root-доступ** | libsu · SuFile · перегляд /system /data /proc · chmod/chown · shell exec |
| **Текстовий редактор** | Пошук/навігація, root-збереження, моноширинний шрифт, UTF-8 |
| **Медіаплеєр** | ExoPlayer Media3 · відео · аудіо · збереження позиції |
| **Переглядач фото** | PhotoView (пінч-зум) · ViewPager2 (свайп між фото) · GIF через Coil |
| **Переглядач PDF** | Системний PdfRenderer · перегортання сторінок |
| **Встановлення APK** | FileProvider · підтримка Android 7+ |
| **Мережа** | FTP (commons-net) · SMB (smbj) · SFTP (jsch) |
| **Пошук** | Kotlin Flow · по імені · по вмісту · regex · фільтри розміру/дати/типу |

---

## Вимоги

- **minSdk**: 26 (Android 8.0 Oreo)
- **targetSdk**: 34 (Android 14)
- **Root**: опціонально (Magisk, KernelSU)

---

## Структура проекту

```
app/src/main/
├── java/com/kirivsoft/commander/
│   ├── KirivCommanderApp.kt          # Application, libsu init
│   ├── ui/
│   │   ├── MainActivity.kt           # Двопанель, дії, дозволи
│   │   ├── SettingsActivity.kt       # Налаштування
│   │   ├── panels/
│   │   │   ├── FilePanelFragment.kt  # Одна панель
│   │   │   └── FileListAdapter.kt    # RecyclerView адаптер
│   │   ├── viewers/
│   │   │   ├── TextEditorActivity.kt
│   │   │   ├── MediaPlayerActivity.kt
│   │   │   └── ViewerActivities.kt   # Image + PDF + ApkHelper
│   │   └── dialogs/
│   │       └── Dialogs.kt            # NewFolderDialog, SearchDialog
│   ├── file/
│   │   ├── FileItem.kt               # Модель + FileListLoader + SortOrder
│   │   ├── FileOperationsManager.kt  # Copy/Move/Delete/Zip/Hash
│   │   └── FileOperationService.kt   # Foreground Service
│   ├── root/
│   │   └── RootAccessManager.kt      # libsu операції
│   ├── search/
│   │   └── SearchManager.kt          # Flow-based пошук
│   ├── network/
│   │   └── NetworkFSManager.kt       # FTP/SMB/SFTP
│   └── utils/
│       └── Formatters.kt             # Форматування розміру/дати
└── res/
    ├── layout/                       # Всі XML layouts
    ├── menu/                         # main_menu, editor_menu
    ├── values/                       # strings, colors, themes, arrays
    ├── drawable/                     # Іконки типів файлів
    ├── xml/                          # file_provider_paths, preferences
    └── mipmap-*/                     # Launcher icons
```

---

## Залежності

```
libsu:core/io/service:5.2.2    — root доступ
media3-exoplayer:1.2.1         — медіаплеєр
coil:2.5.0                     — завантаження зображень
PhotoView:2.3.0                — зум для фото
commons-compress:1.26.0        — ZIP/архіви
commons-net:3.10.0             — FTP
jsch:0.2.17                    — SFTP/SSH
smbj:0.13.0                    — SMB/Windows Share
kotlinx-coroutines:1.7.3       — асинхронність
```

---

## Підписання APK для розповсюдження

Якщо хочете підписаний APK (для встановлення без "невідомі джерела" помилок):

1. Згенеруйте keystore:
```bash
keytool -genkey -v -keystore kirivcommander.jks \
  -alias kirivsoft -keyalg RSA -keysize 2048 -validity 10000
```

2. Додайте secrets у GitHub (Settings → Secrets → Actions):
   - `KEYSTORE_BASE64` — base64 від keystore файлу
   - `KEY_ALIAS` — alias (kirivsoft)
   - `KEY_PASSWORD` — пароль ключа
   - `STORE_PASSWORD` — пароль keystore

3. Оновіть workflow `.github/workflows/build.yml` для підписання.

---

*© 2024 KirivSoft / Іван Кіранчук*
