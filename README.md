# flozaxx

Önce kendi ihtiyacım için uygulamalar ve araçlar yapıyorum. Hepsinin özeti,
durumu ve bağlantıları tek sayfada: **[flozaxx.com](https://flozaxx.com)**

## Projeler

| Proje | Ne işe yarar | Teknoloji | Durum |
| --- | --- | --- | --- |
| **[Noty](https://flozaxx.com/noty)** | Şifreli not ve şifre kasası. Her şey cihazda şifrelenir, sunucuya yalnızca şifreli veri gider. | Flutter · Firebase | Yayında (web + Android) |
| **[Sticker Studio](https://flozaxx.com/stickerstudio)** | Fotoğraf ve videolardan WhatsApp çıkartma paketleri. | Kotlin · Jetpack Compose | Geliştiriliyor |
| **[PC Kontrol](https://flozaxx.com/pckontrol)** | Evdeki bilgisayarı telefondan yönetmek: kilitle, uyut, kapat, ekran görüntüsü al. Sırada Wake-on-LAN ile uzaktan açma var. | Kotlin · PowerShell | Geliştiriliyor |
| **[Rubik Küpü Rehberi](https://flozaxx.com/rubik)** | Hiç çözmemişlikten körlemeye beş yöntem, formül kütüphanesi, serbest küp. Ek kaynak: [BLD Harf Küpü](https://flozaxx.com/p/bld/). | HTML/CSS/JS · three.js | Yayında |
| **[Önce Bak, Sonra Yap](https://flozaxx.com/oncebak)** | Yeni bir özelliğe başlarken izlediğim yöntem ve A, B, C diye sıralanmış adımlar. | — | Yayında |

## Bu depo

[flozaxx.com](https://flozaxx.com) sitesinin kaynağı
[`flozaxx.web.app/`](flozaxx.web.app) klasöründe: çerçevesiz HTML, CSS ve
JavaScript, Firebase Hosting'de. İçerik `flozaxx.web.app/public/projects.js`
dosyasından gelir.

`main` dalına gelen her site değişikliği GitHub Actions ile otomatik
yayınlanır ([deploy.yml](.github/workflows/deploy.yml)).
