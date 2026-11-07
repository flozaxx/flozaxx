# flozaxx

fazlasıyla işlevsel. fazlasıyla yaratıcı.

Önce kendi ihtiyacım için uygulamalar ve araçlar yapıyorum. Hepsinin özeti,
durumu ve bağlantıları tek sayfada: **[flozaxx.web.app](https://flozaxx.web.app)**

## Projeler

| Proje | Ne işe yarar | Teknoloji | Durum |
| --- | --- | --- | --- |
| **[Noty](https://noty-app1.web.app)** | Şifreli not ve şifre kasası. Her şey cihazda şifrelenir, sunucuya yalnızca şifreli veri gider. | Flutter · Firebase | Yayında (web + Android) |
| **Sticker Studio** | Fotoğraf ve videolardan WhatsApp çıkartma paketleri. | Kotlin · Jetpack Compose | Geliştiriliyor |
| **[Rubik Küpü Rehberi](https://flozaxx.web.app/p/rubik/)** | Hiç çözmemişlikten körlemeye beş yöntem, formül kütüphanesi, serbest küp. Ek kaynak: [BLD Harf Küpü](https://flozaxx.web.app/p/bld/). | HTML/CSS/JS · three.js | Yayında |
| **[Önce Bak, Sonra Yap](https://flozaxx.web.app/p/once-bak-sonra-yap/)** | Yeni bir özelliğe başlarken izlediğim yöntem ve kontrol listesi. | — | Yayında |

## Bu depo

[flozaxx.web.app](https://flozaxx.web.app) sitesinin kaynağı
[`flozaxx.web.app/`](flozaxx.web.app) klasöründe: çerçevesiz HTML, CSS ve
JavaScript, Firebase Hosting'de. İçerik `flozaxx.web.app/public/projects.js`
dosyasından gelir.

```sh
cd flozaxx.web.app
firebase deploy --only hosting
```
