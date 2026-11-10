# Kilit

Seçili uygulamaları PIN ve biyometrik ile kilitleyen Android uygulaması. Play Store'da değil,
tek kullanıcı için APK olarak dağıtılır. Hedef: Samsung Galaxy (One UI 8.5 ve üzeri), Android 14+.

## Nasıl çalışır

Gerçek bir yalıtım değildir: kilitli uygulama öne geçtiğinde üstüne bir kilit ekranı açılır.

| Paket | Görevi |
| --- | --- |
| `rules/` (ayrı modül) | Saf Kotlin kural motoru: paket, ağ durumu, saat ve oturumdan "kilitle / geç" kararı. Birim testli. |
| `detection` | Erişilebilirlik servisi; öne geçen paketi kural motoruna yollar, kilit ekranı gelene kadar uygulamanın üstünü örter. |
| `context` | Bağlı Wi-Fi'nin BSSID'sini izler. Okunamazsa kilit etkin sayılır. |
| `lock` | Kilit ekranı. PIN mantığı `LockController`'da; `styles/` altındaki her dosya bir görünümdür. |
| `storage` | DataStore ayarları; PIN ve kurtarma kodu PBKDF2 + Android Keystore HMAC ile saklanır. |
| `protection` | Cihaz yöneticisi, ön plan servisi, açılışta başlama, izin durumu. |
| `notifications` | Kilitli uygulamaların bildirimlerini gizler; yeni kurulan uygulama için "kilitlensin mi" sorar. |
| `ui` | Kurulum sihirbazı, uygulama listesi, ayarlar. |

Yeni kilit ekranı stili: `lock/styles/` altına bir dosya, `LockScreenStyles.all` listesine bir satır.

## Derleme

    ./gradlew :rules:test :app:testDebugUnitTest :app:assembleDebug

Ekran görüntüsü testi kilit stillerini ve ekranları cihazsız çizer; görseller
`app/build/outputs/roborazzi/` altına yazılır.

## İmzalı sürüm

GitHub Actions her gönderimde APK üretir ([applock.yml](../.github/workflows/applock.yml)) ve
iş akışı çıktısına ekler. İmza için dört depo gizli anahtarı gerekir; yoksa geçici bir debug
APK çıkar (üstüne güncelleme yapılamaz).

Anahtarı bir kez üretin ve dosyayı güvenli bir yerde yedekleyin. **Bu anahtar kaybolursa
güncelleme kurulamaz, uygulama kaldırılıp yeniden kurulur ve verisi gider.**

```powershell
keytool -genkeypair -v -keystore kilit-release.jks -alias kilit -keyalg RSA -keysize 4096 -validity 10000
```

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("kilit-release.jks")) | Set-Clipboard
```

Depo → Settings → Secrets and variables → Actions:

| Gizli anahtar | Değer |
| --- | --- |
| `APPLOCK_KEYSTORE_BASE64` | Panoya kopyalanan metin |
| `APPLOCK_KEYSTORE_PASSWORD` | Anahtar deposu parolası |
| `APPLOCK_KEY_ALIAS` | `kilit` |
| `APPLOCK_KEY_PASSWORD` | Anahtar parolası (sorulmadıysa depo parolasıyla aynı) |
