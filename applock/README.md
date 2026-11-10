# Kilit

Seçili uygulamaları PIN ve biyometrik ile kilitleyen Android uygulaması. Play Store'da değil,
tek kullanıcı için APK olarak dağıtılır.

## Modüller

- `rules/`: Android'den bağımsız kural motoru (kilitle / geç kararı) ve birim testleri.
- `app/`: Android uygulaması.

## Derleme

    ./gradlew :rules:test :app:assembleDebug

İmzalı sürüm GitHub Actions'ta üretilir ([applock.yml](../.github/workflows/applock.yml)).
Gereken depo gizli anahtarları: `APPLOCK_KEYSTORE_BASE64`, `APPLOCK_KEYSTORE_PASSWORD`,
`APPLOCK_KEY_ALIAS`, `APPLOCK_KEY_PASSWORD`. Aynı anahtar korunmazsa güncelleme kurulamaz ve
uygulamanın verisi gider.
