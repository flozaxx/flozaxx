# flozaxx.web.app

Projelerimin özet sitesi. Çerçeve ve derleme adımı yok: düz HTML, CSS ve
JavaScript, Firebase Hosting'de yayında.

```
public/
  index.html      sayfa iskeleti, Bağlantılar ve Araç kutusu
  projects.js     tüm proje içeriği (yeni proje buraya eklenir)
  app.js          liste, süzgeçler, açılır ayrıntılar, tema
  style.css       tasarım token'ları ve stiller
  p/rubik/        Rubik Küpü Rehberi
  p/bld/          BLD Harf Küpü
  404.html
firebase.json     barındırma ayarları, eski /rehberler ve /indirmeler yönlendirmeleri
```

## Proje eklemek

`public/projects.js` içindeki `PROJECTS` listesine bir nesne kopyala ve
düzenle. Alanların anlamı dosyanın başında yazıyor. Liste, süzgeç sayıları,
teknoloji listesi ve üstteki sayaç kendiliğinden güncellenir. `SITE.updated`
alt bilgideki tarihtir.

Bir projenin kendi bağlantısı `https://flozaxx.web.app/#<id>`; açıldığında o
projenin ayrıntısı açık gelir.

## Yerelde bakmak

```sh
cd public
python -m http.server 8000
```

## Yayınlamak

```sh
firebase deploy --only hosting
```

Firebase projesi `flozaxx-a8521`. Projenin varsayılan sitesi
`flozaxx-a8521.web.app`, bu site ise aynı projedeki ek `flozaxx` sitesi;
`firebase.json` içindeki `"site": "flozaxx"` yüklemeyi oraya yönlendirir.
