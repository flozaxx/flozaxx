# Noty

Şifreli not ve şifre kasası. Tek Flutter kod tabanı: Android APK + Web (Firebase Hosting).

Tüm içerik (başlıklar dahil) cihazda, ana şifreden türetilen anahtarla
şifrelenir. Firestore'a yalnızca şifreli veri, kayıt türü ve tarihler gider.

> **Ana şifrenizi unutursanız verileriniz kurtarılamaz.** Sunucuda şifrenin
> veya anahtarın bir kopyası yoktur.

## Gereksinimler

- Flutter (stable) — geliştirme sırasında 3.47.x kullanıldı
- Android SDK (APK için)
- [Firebase CLI](https://firebase.google.com/docs/cli) ve
  [FlutterFire CLI](https://firebase.google.com/docs/flutter/setup)

## Firebase kurulumu

Firebase yapılandırma dosyaları depoya **girmez** (`.gitignore`):
`lib/firebase_options.dart`, `android/app/google-services.json`.
Her geliştirme ortamında bir kez üretin:

1. Firebase konsolunda bir proje oluşturun.
2. **Authentication → Sign-in method** altında **Google** sağlayıcısını etkinleştirin.
3. **Firestore Database** oluşturun.
4. Proje kökünde:
   ```sh
   flutterfire configure --platforms=android,web
   ```
   Bu komut `lib/firebase_options.dart` ve `android/app/google-services.json`
   dosyalarını üretir ve Android Gradle dosyalarına `google-services`
   eklentisini ekler.
5. **Android'de Google girişi için** imzalama anahtarınızın SHA-1 ve SHA-256
   parmak izlerini Firebase konsolunda Android uygulamasına ekleyin, ardından
   `flutterfire configure` komutunu yeniden çalıştırın (google-services.json
   güncellenmeli):
   ```sh
   cd android && ./gradlew signingReport
   ```
6. **Web'de** Authentication → Settings → Authorized domains listesinde
   kullanılan alan adı (ör. `localhost`, hosting alan adı) bulunmalı.

## Çalıştırma

```sh
flutter pub get
flutter run -d chrome        # web
flutter run -d <android-id>  # Android
```

## Test

```sh
flutter analyze
flutter test
```

## Proje yapısı

```
lib/
  main.dart             Firebase başlatma
  app.dart              tema, yerelleştirme, oturum yönlendirmesi
  core/                 kripto, oturum/kilit, pano, metinler
  data/                 Firebase depoları ve modeller
  features/             ekranlar
test/                   birim ve widget testleri
```
