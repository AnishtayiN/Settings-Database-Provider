# Settings Database Provider (v2 — hardened)

پل بین اپ‌های دیگر و دیتابیس تنظیمات سیستم اندروید — با کلاس‌بندی امنیتی بهتر، ساخت خودکار در GitHub Actions و پشتیبانی از Android 15.

## چی فرق کرد در نسخهٔ ۱.۲ (v2)

- **پرمیشن BRIDGE (سطح normal)** اضافه شد: حالا هر اپی که می‌خواد از پل استفاده کنه باید پرمیشن رو declare کنه — اپ‌های ناشناس دیگر بدون اجازه به دیتابیس settings دسترسی ندارن
- **کد provider مقاوم‌سازی شد:** خطاها SecurityException-safe شدن (دیگه با URI بد یا پرمیشن نبودن کرش نمی‌کنه)، `getType` برای مسیر `/writeSecurePermission` جواب درست می‌ده، و NPE هنگام context null رفع شد
- **به‌روزرسانی ابزار بیلد:** compileSdk 34، AGP 7.4.2، Kotlin 1.8.22، Gradle 7.5.2، core-ktx 1.9.0
- **workflow بیلد خودکار در GitHub Actions** — هر پوش به main یه APK unsigned می‌سازه و به release اضافه می‌شه
- **versionCode 3 / versionName 1.2**

## چطور کار می‌کنه

اپ فقط یک ContentProvider داره که مثل پل عمل می‌کنه بین اپ‌های دیگه و دیتابیس settings. چون targetSdk ۲۲ می‌مونه، محدودیت‌های اندرویدهای جدید دور زده می‌شه و اپ‌های دیگه (با targetSdk بالا) می‌تونن از این طریق تنظیمات سیستمی رو تغییر بدن.

## نصب

از [Releases](https://github.com/AnishtayiN/Settings-Database-Provider/releases/latest) آخرین APK رو بگیر.

### Grant Permission (اختیاری ولی مهم)

برای ویرایش جداول secure و global بعد از نصب این دستور ADB رو بزن:

```
adb shell pm grant com.netvor.settings.database.provider android.permission.WRITE_SECURE_SETTINGS
```

یا با LADB / Brevent:

```
pm grant com.netvor.settings.database.provider android.permission.WRITE_SECURE_SETTINGS
```

## نصب کنار نسخهٔ اصلی (هم‌زمان)

این نسخه **applicationId جدا** دارد (`com.anishtayin.settings.database.provider`) و **authority جدا** (`com.anishtayin.provider.SettingsDatabaseProvider`) — یعنی بدون حذف نسخهٔ اصلی gamemn02 می‌توانی هر دو را هم‌زمان نصب داشته باشی.

اگر ابزاری (مثل Settings Database Editor) از نسخهٔ اصلی استفاده می‌کرد و می‌خواهی از این نسخه استفاده کند، در تنظیمات ابزار آدرس provider را عوض کن به:

```
content://com.anishtayin.provider.SettingsDatabaseProvider
```

و این پرمیشن را در مانیفست ابزار declare کن (اگر خودت اپ را می‌سازی):

```xml
<uses-permission android:name="com.anishtayin.settings.database.provider.permission.BRIDGE" />
```

دستور grant برای این نسخه:

```
adb shell pm grant com.anishtayin.settings.database.provider android.permission.WRITE_SECURE_SETTINGS
```

## گارد کلیدهای حساس (v1.2.2)

کلیدهایی که از طریق پل **قابل تغییر نیستند** (زنجیرهٔ سوءاستفادهٔ شناخته‌شده دارند، از جمله CVE-2024-31317):

`hidden_api_blacklist_exemptions` · `adb_enabled` · `adb_wifi_enabled` · `development_settings_enabled` · `verifier_verify_adb_installs` · `package_verifier_enable` · `upload_apk_enable` · `device_provisioned`

تلاش برای خواندن/نوشتن این‌ها `SecurityException` می‌گیرد.

## اندروید ۱۴ و ۱۵

اندروید ۱۴+ نصب اپ‌هایی که targetSdk < 23 دارن رو می‌بنده (اندروید ۱۵: < 24). برای نصب:

**با PC:**
```
adb install --bypass-low-target-sdk-block sdp-v1.2-unsigned.apk
```

**با LADB / Brevent:**
```
cat sdcard/Download/sdp-v1.2-unsigned.apk | pm install --bypass-low-target-sdk-block -S <FILE_SIZE>
```

اگه خطای `Unknown option --bypass-low-target-sdk-block` گرفتی:
```
cat sdcard/Download/sdp-v1.2-unsigned.apk | su -c pm install --bypass-low-target-sdk-block -S <FILE_SIZE>
```

`<FILE_SIZE>` حجم دقیق فایل APK به بایت.

## تغییرات برای اپ‌های استفاده‌کننده از پل

اگه اپ تو از این پل استفاده می‌کنه، این خط رو به manifest اضافه کن:

```xml
<uses-permission android:name="com.netvor.settings.database.provider.permission.BRIDGE" />
```

بدون این خط، `ContentResolver` موقع query/insert/update/delete از پل `SecurityException` می‌گیره.

## بیلد

### CLI

```
gradlew assembleRelease
```

### GitHub Actions (خودکار)

- هر push به `main` → بیلد و آپلود artifact
- Manual: Actions → Build APK → Run workflow → version (مثلاً `v1.2`) → APK به release اضافه می‌شه

## سازندهٔ اپ اصلی

اپ اصلی توسط [gamemn02](https://github.com/gamemn02) ساخته شده — [Settings-Database-Provider](https://github.com/gamemn02/Settings-Database-Provider) — تحت لایسنس MIT (فایل LICENSE).

## پشتیبانی

- Issues همین ریپو
- گروه تلگرام اپ اصلی: [Settings Database Editor](https://t.me/+o140MdSojRI2MjA0)
