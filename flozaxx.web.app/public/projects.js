/*
  Sitedeki içeriğin tamamı bu dosyadadır. Yeni proje eklemek için PROJECTS
  listesine bir nesne kopyalayıp düzenlemek yeterli; sıra, sitedeki sıradır.

  kind      : "app" Uygulama · "tool" Rehber / araç · "web" Web
  status    : "live" Yayında · "dev" Geliştiriliyor · "idea" Planlanıyor
              (planlanan projede "Öne çıkanlar" yerine "Planlananlar" yazar)
  icon      : app.js içindeki ICONS anahtarlarından biri; yoksa adın baş harfi
  color     : ikon zemini (CSS rengi ya da gradyan), ink: ikon rengi
  links     : ilk bağlantı satırdaki hızlı "aç" düğmesi olur
  resources : projeye bağlı ek kaynaklar (ayrıntıda "Ek kaynaklar" olarak görünür)

  SITE.now     : sağ sütundaki "Şu an" kutusu
  SITE.updates : sağ sütundaki "Son güncellemeler", en yeni en üstte; en yeni 4 tanesi görünür
*/

window.SITE = {
  updated: "7 Kasım 2026",
  now: [
    { project: "sticker-studio", text: "İlk sürüm üzerinde çalışıyorum: editör ve paket akışı." },
    { project: "noty", text: "1.12.1 yayında; telefonda ve web’de kullanılabilir." },
  ],
  updates: [
    { date: "7 Kas 2026", project: "flozaxx-web", text: "Site yenilendi: tüm projeler tek sayfada." },
    { date: "3 Kas 2026", project: "noty", text: "Noty 1.12: e-postayla erişim, erişim isteği, “Notlarıma ekle”." },
    { date: "3 Kas 2026", project: "noty", text: "Noty 1.11: uçtan uca şifreli not paylaşımı, okuma görünümü." },
    { date: "1 Kas 2026", project: "once-bak-sonra-yap", text: "“Önce Bak, Sonra Yap” rehberi yazıldı." },
    { date: "24 Eki 2026", project: "noty", text: "Noty 1.10: iki yönlü sabitleme, grup şeridi." },
  ],
};

window.PROJECTS = [
  {
    id: "noty",
    name: "Noty",
    tagline: "Şifreli not ve şifre kasası",
    kind: "app",
    status: "live",
    icon: "noty",
    color: "linear-gradient(135deg, #2F8F65, #1E6446)",
    platforms: ["Web", "Android"],
    tech: ["Flutter", "Dart", "Firebase", "Riverpod"],
    version: "1.12.1",
    started: "Ekim 2026",
    summary:
      "Notlarımı ve şifrelerimi tek yerde, güvenle tutmak için yaptım. Başlıklar dahil her şey cihazda, hesaba özel bir anahtarla şifrelenir; sunucuya yalnızca şifreli veri gider. Anahtarın sunucuda kopyası yoktur.",
    features: [
      "Zengin metin notlar: başlıklar, listeler, yapılacaklar, alıntı, kod ve görsel; satır başında “/” ile komut menüsü",
      "Şifre kasası: güçlü şifre üretici, zayıf ve tekrar kullanılan şifre uyarıları; kopyalanan şifre 30 sn sonra panodan silinir",
      "Uçtan uca şifreli not paylaşımı: bağlantıyla ya da e-postayla, görüntüleme veya düzenleme yetkisiyle",
      "Gruplar, sabitleme, 30 günlük çöp kutusu; bilgisayarda Ctrl+K komut paleti",
      "Şifreli .noty yedeği; Chrome, Firefox, Bitwarden (CSV) ve Google Keep’ten içe aktarma",
      "PIN, desen ya da şifre kilidi; Android’de ekran görüntüsü koruması",
      "Koyu ve açık tema; telefonda sade arayüz, geniş ekranda panel düzeni",
    ],
    security: "AES-256-GCM şifreleme · Argon2id ile korunan yedekler · sıkı Firestore kuralları",
    links: [{ label: "Web uygulamasını aç", url: "https://noty-app1.web.app" }],
  },
  {
    id: "sticker-studio",
    name: "Sticker Studio",
    tagline: "Fotoğraf ve videolardan WhatsApp çıkartma paketleri",
    kind: "app",
    status: "dev",
    icon: "sticker",
    color: "#4FF07F",
    ink: "#003914",
    platforms: ["Android"],
    tech: ["Kotlin", "Jetpack Compose", "Material 3"],
    started: "Ekim 2026",
    summary:
      "Telefondaki fotoğraf ve videolardan birkaç dokunuşla WhatsApp çıkartma paketi hazırlayan Android uygulaması. Paket, WhatsApp’a eklenmeden önce orada nasıl görüneceğiyle önizlenir. Henüz mağazada değil.",
    features: [
      "Fotoğraftan statik, videodan animasyonlu çıkartma",
      "Kontur (beyaz çerçeve) stilleri; seçim önizlemeyle yapılır",
      "Akıcı, Dengeli ve Net kalite seçenekleri",
      "Adım rehberi: biten, sıradaki ve isteğe bağlı adımlar tek bakışta",
      "“WhatsApp’ta böyle görünecek” paket önizlemesi",
      "Hızlı paket ön ayarları ve Paketlerim listesi",
      "Türkçe ve İngilizce arayüz",
    ],
    links: [],
  },
  {
    id: "pc-kontrol",
    name: "PC Kontrol",
    tagline: "Evdeki bilgisayarı telefondan açıp yönetmek",
    kind: "app",
    status: "idea",
    icon: "power",
    color: "#E5484D",
    platforms: ["Android", "Windows"],
    tech: ["Wake-on-LAN"],
    summary:
      "Evdeki bilgisayarımı telefondan yönetmek için planladığım uygulama. İlk hedef Wake-on-LAN ile kapalı bilgisayarı uzaktan açmak; ardından durumunu görmek, kapatmak ve uyutmak.",
    features: [
      "Wake-on-LAN: telefondan bilgisayarın ağ kartına “sihirli paket” göndererek onu açmak",
      "Ev dışından açma: sihirli paket yalnızca yerel ağda çalışır; bu yüzden evde sürekli açık bir aracı (modem, Raspberry Pi ya da evde kalan eski bir telefon) paketi iletir",
      "Durum: bilgisayar açık mı, kapalı mı, uykuda mı tek bakışta",
      "Uzaktan kapatma, uyku ve yeniden başlatma",
      "Güvenlik: komutları yalnızca kendi hesabım ve cihazım gönderebilir; bilgisayarda internete açık bir bağlantı noktası olmaz",
      "Hazırlık: BIOS'ta ve ağ kartı ayarlarında Wake-on-LAN'ı açmak, Windows'ta hızlı başlatmayı kapatmak",
    ],
    links: [],
  },
  {
    id: "rubik",
    name: "Rubik Küpü Rehberi",
    tagline: "Hiç çözmemişlikten körlemeye, etkileşimli küp rehberi",
    kind: "tool",
    status: "live",
    icon: "cube",
    color: "#F08619",
    platforms: ["Web"],
    tech: ["HTML/CSS/JS", "three.js"],
    started: "Ekim 2026",
    summary:
      "Küpü sıfırdan öğrenmek ya da hızlanmak için beş yol. Her yöntem adım adım anlatılır, formüller 3B küp üzerinde oynatılır.",
    features: [
      "Çırak: 8 durakta, kat kat ilk çözüm",
      "CFOP (Cross · F2L · OLL · PLL) ve Roux (iki blok · CMLL · LSE)",
      "ZB ile son katı tek formülde bitirme",
      "Körleme: ezberle, gözü kapalı çöz",
      "Formül kütüphanesi: F2L, OLL, PLL, ZBLL, CMLL",
      "Serbest küp: sürükleyip çevir, karıştır, süre tut; kendi küpünü kamerayla tara, beş yöntemi yarıştır",
    ],
    links: [{ label: "Rehberi aç", url: "/p/rubik/" }],
    resources: [
      {
        name: "BLD Harf Küpü",
        url: "/p/bld/",
        text: "Körleme harf şemasını (sarı üst, kırmızı ön; büyük harf köşe, küçük harf kenar) 3B küpte öğreten ve sınayan araç. Keşfet, harf sor, yer bul; sık yanılınan harfler daha sık sorulur.",
      },
    ],
  },
  {
    id: "once-bak-sonra-yap",
    name: "Önce Bak, Sonra Yap",
    tagline: "AI destekli yazılımda profesyonel standartlar",
    kind: "tool",
    status: "live",
    icon: "compass",
    color: "#7A5AF8",
    platforms: ["Web"],
    tech: [],
    started: "Kasım 2026",
    summary:
      "Yeni bir uygulama ya da ekrana başlarken izlediğim yöntem: kod yazmadan önce iyi örnekleri incele, ortak olanı kural kabul et, farkı bilinçli yerlerde yarat. Kaynaklı tablolar ve işaretlenebilir bir kontrol listesiyle.",
    features: [
      "Referans araştırma şablonu: 3–5 örnek uygulama, ortak kalıplar ve kararlar",
      "AI ile yazılmış yazılımların büyük sorunları ve sektördeki karşılıkları",
      "Arayüz: profesyonel editörlerin ortak düzeni, Sticker Studio editörü için öneriler",
      "Optimizasyon: kare bütçesi, vekil önizleme, arka plan işleri",
      "Kod sadeliği: AI kodunu ele veren izler, yorum kuralları, anlama borcu",
      "Planlama, geliştirme ve yayın öncesi için kontrol listesi",
    ],
    links: [{ label: "Rehberi oku", url: "/p/once-bak-sonra-yap/" }],
  },
  {
    id: "flozaxx-web",
    name: "flozaxx.web.app",
    tagline: "Bu site: tüm projelerimin özeti",
    kind: "web",
    status: "live",
    icon: "globe",
    color: "linear-gradient(135deg, #1D3D53, #191819)",
    platforms: ["Web"],
    tech: ["HTML/CSS/JS", "Firebase"],
    started: "2025",
    summary:
      "Projelerimi tek sayfada toplayan kişisel site. Çerçevesiz, sade HTML, CSS ve JavaScript; içeriğin tamamı tek bir veri dosyasından gelir.",
    features: [
      "Kategori ve teknoloji süzgeci; aynı anda tek proje açık kalır",
      "Her projenin paylaşılabilir bağlantısı (#kimlik)",
      "Geniş ekranda sayfa içi gezinme, güncel durum ve son güncellemeler",
      "Koyu ve açık tema; varsayılan olarak cihaz ayarını izler",
    ],
    links: [{ label: "Kaynak kodu", url: "https://github.com/flozaxx/flozaxx/tree/main/flozaxx.web.app" }],
  },
];
