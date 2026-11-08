# flozaxx.com

Projelerimin özet sitesi. Çerçeve ve derleme adımı yok: düz HTML, CSS ve
JavaScript, Firebase Hosting'de yayında.

```
public/
  index.html      sayfa iskeleti, yan sütunlar, Bağlantılar ve Araç kutusu
  projects.js     tüm içerik: projeler, "Şu an", "Son güncellemeler"
  site.js         tüm sayfaların ortak parçaları: ikonlar, sol panel, telefondaki menü
  app.js          ana sayfa: proje kartları, süzgeçler, sağ sütun
  page.html       proje sayfalarının şablonu (/noty, /noty/changelog …)
  page.js         proje sayfasını ve sürüm geçmişini projects.js'ten çizer
  theme.js        koyu / açık tema (tüm sayfalar)
  style.css       tasarım token'ları ve stiller
  doc.css, doc.js rehber sayfalarının okuma düzeni ve içindekiler
  p/rubik/                Rubik Küpü Rehberi
  p/bld/                  BLD Harf Küpü (Rubik rehberinin ek kaynağı)
  p/once-bak-sonra-yap/   Önce Bak, Sonra Yap rehberi
  404.html
firebase.json     barındırma ayarları, eski /rehberler ve /indirmeler yönlendirmeleri
```

## Proje eklemek

`public/projects.js` içindeki `PROJECTS` listesine bir nesne kopyala ve
düzenle. Alanların anlamı dosyanın başında yazıyor. Liste, süzgeç sayıları,
teknoloji listesi ve üstteki sayaç kendiliğinden güncellenir. `SITE.updated`
alt bilgideki tarihtir; `SITE.now` ve `SITE.updates` sağ sütundaki kutulardır.

## Rehberi güncellemek

`public/p/once-bak-sonra-yap/index.html` düz HTML'dir; metin doğrudan orada
düzenlenir. İçindekiler `h2` başlıklarından elle yazılı, yeni bölüm eklenirse
oraya da bir satır eklenir.

Bir projenin kendi bağlantısı `https://flozaxx.com/#<id>`; açıldığında o
projenin ayrıntısı açık gelir.

## Yerelde bakmak

```sh
cd public
python -m http.server 8000
```

## Yayınlamak

`main` dalına `flozaxx.web.app/` altında bir değişiklik gelince GitHub
Actions siteyi kendiliğinden yayınlar (`.github/workflows/deploy.yml`).
Actions sekmesindeki "Siteyi yayınla" iş akışı elle de çalıştırılabilir.
Bunun için depoda `FIREBASE_SERVICE_ACCOUNT_FLOZAXX_A8521` gizli anahtarı
olmalı; bir kez bu klasörde `firebase init hosting:github` çalıştırmak onu
oluşturur. Anahtar yoksa iş akışı yayını atlar.

Elle yayınlamak için:

```sh
firebase deploy --only hosting
```

Firebase projesi `flozaxx-a8521`. Projenin varsayılan sitesi
`flozaxx-a8521.web.app`, bu site ise aynı projedeki ek `flozaxx` sitesi;
`firebase.json` içindeki `"site": "flozaxx"` yüklemeyi oraya yönlendirir.

## İçerik

Projeler, "Şu an" ve son güncellemeler Firestore'daki `site/content`
belgesinde durur ve yönetim panelinden (`/admin/`) düzenlenir.
`public/projects.js` yalnızca veritabanına ulaşılamazsa kullanılan yedektir.

Kodla içerik değiştirmek için (Claude bu yolu kullanır):

```sh
python tools/content.py pull     # canlı içerik -> content/content.json
# content/content.json düzenlenir, main'e gönderilir
```

`.github/workflows/content.yml` dosyayı Firestore'a yazar. Arada panelden
kayıt yapılmışsa yazma reddedilir; içerik yeniden çekilip değişiklik tekrar
uygulanır. İş akışının hesabında Firestore'a yazma izni (Cloud Datastore
User) olmalıdır.
