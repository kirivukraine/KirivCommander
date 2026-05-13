# KirivCommander

**Сучасний двопанельний файловий менеджер для Android**
*by KirivSoft / Іван Кіранчук*

![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin)
![Build](https://github.com/YOUR_USERNAME/KirivCommander/actions/workflows/build.yml/badge.svg)

---

## Завантажити APK

Готовий APK — вкладка **Actions → останній run → Artifacts → KirivCommander-debug**

---

## Збірка на GitHub (покрокова інструкція)

### Крок 1 — Створити репозиторій

1. Зайдіть на [github.com](https://github.com) → кнопка **New repository**
2. Назва: `KirivCommander`
3. Visibility: Private або Public (на вибір)
4. ❌ НЕ ставте "Add README", "Add .gitignore" — репозиторій має бути порожнім
5. **Create repository**

### Крок 2 — Завантажити проект

```bash
# Розпакуйте ZIP у будь-яку папку
unzip KirivCommander_GitHub.zip
cd KirivCommander

# Ініціалізуйте git
git init
git add .
git commit -m "KirivCommander v1.0.0 — initial commit"

# Підключіть свій репозиторій (замініть YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/KirivCommander.git
git branch -M main
git push -u origin main
```

### Крок 3 — Дочекатись збірки

Після `git push` GitHub автоматично запустить **Actions → Build KirivCommander APK**.

Час збірки: **~5-8 хвилин**.

### Крок 4 — Завантажити APK

`Actions` → клацніть на останній успішний run → розділ **Artifacts** внизу →
завантажте `KirivCommander-debug`.

---

## Як це працює (без gradlew)

Workflow використовує **`gradle/actions/setup-gradle@v3`** — офіційний GitHub Action від команди Gradle. Він сам завантажує і налаштовує Gradle 8.4, тому `gradlew` і `gradle-wrapper.jar` не потрібні.

```yaml
- uses: gradle/actions/setup-gradle@v3
  with:
    gradle-version: '8.4'
- run: gradle assembleDebug --no-daemon
```

---

## Функції

| Модуль | Можливості |
|--------|-----------|
| Файловий менеджер | 2 незалежні панелі, breadcrumb, 7 режимів сортування, множинний вибір |
| Операції з файлами | Копіювання, переміщення, видалення, перейменування, ZIP, MD5/SHA256 |
| Root (libsu) | /system, /data, /proc · chmod/chown · shell exec · SuFile R/W |
| Текстовий редактор | Пошук, root-збереження, UTF-8, моноширинний шрифт |
| Медіаплеєр | ExoPlayer Media3 · відео · аудіо |
| Переглядач фото | PhotoView (пінч-зум) · ViewPager2 (свайп) · GIF |
| Переглядач PDF | PdfRenderer · перегортання сторінок |
| Встановлення APK | FileProvider · Android 7+ |
| Мережа | FTP · SMB/Samba · SFTP/SSH |
| Пошук | Flow · по імені · по вмісту · regex · фільтри |

---

## Вимоги

- minSdk 26 (Android 8.0)
- targetSdk 34 (Android 14)
- Root: опціонально (Magisk / KernelSU)

---

## Підписання APK

Для підписаного APK:

```bash
# 1. Генеруємо keystore
keytool -genkey -v -keystore kirivcommander.jks \
  -alias kirivsoft -keyalg RSA -keysize 2048 -validity 10000
```

```bash
# 2. Конвертуємо в base64 (для GitHub Secret)
base64 -i kirivcommander.jks | pbcopy   # macOS
base64 kirivcommander.jks               # Linux
```

Додайте у `Settings → Secrets → Actions`:
- `KEYSTORE_BASE64`
- `KEY_ALIAS` = kirivsoft
- `KEY_PASSWORD`
- `STORE_PASSWORD`

---

*© 2024 KirivSoft / Іван Кіранчук*
