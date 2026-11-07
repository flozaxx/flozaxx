/*
  Sitedeki proje içeriğinin tamamı bu dosyadadır. Yeni proje eklemek için
  PROJECTS listesine bir nesne kopyalayıp düzenlemek yeterli; sıra, sitedeki
  sıradır.

  kind   : "app" Uygulama · "tool" Rehber / araç · "web" Web
  status : "live" Yayında · "dev" Geliştiriliyor · "private" Özel
  icon   : app.js içindeki ICONS anahtarlarından biri; yoksa adın baş harfi
  color  : ikon zemini (CSS rengi ya da gradyan), ink: ikon rengi
  links  : private: true olan bağlantı claude.ai hesabımda durur; paylaşıma
           açılmadıysa ziyaretçi açamaz, sitede kilit simgesiyle gösterilir
*/

window.SITE = {
  updated: "7 Kasım 2026",
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
    links: [
      { label: "Web uygulamasını aç", url: "https://noty-app1.web.app" },
      { label: "Tasarım önizlemesi", url: "https://claude.ai/artifact/KQSau8oKYPNZsZFwJPAj83", private: true },
    ],
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
    links: [
      { label: "İkon önerileri", url: "https://claude.ai/artifact/DQXCSs7PwRo8SRsRydeM8H", private: true },
    ],
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
  },
  {
    id: "bld-harf-kupu",
    name: "BLD Harf Küpü",
    tagline: "Körleme harf şemasını ezberleme aracı",
    kind: "tool",
    status: "live",
    icon: "letters",
    color: "#1F6BD6",
    platforms: ["Web"],
    tech: ["HTML/CSS/JS"],
    started: "Ekim 2026",
    summary:
      "Körleme çözümde her çıkartmanın bir harfi vardır. Bu araç kendi şemamı (sarı üst, kırmızı ön; büyük harf köşe, küçük harf kenar) 3B küp üzerinde öğretir ve sınar.",
    features: [
      "Keşfet: çıkartmaya dokun, aynı parçadaki diğer harfleri gör",
      "Harf sor: parlayan çıkartmanın harfini bil",
      "Yer bul: verilen harfi küpte ya da açınımda bul",
      "Sık yanılınan harfler daha sık sorulur; zayıf harfler listesi",
      "Köşe / kenar filtresi, sürüklenebilir küp ve klavye kısayolları",
    ],
    links: [{ label: "Aracı aç", url: "/p/bld/" }],
  },
  {
    id: "once-bak-sonra-yap",
    name: "Önce Bak, Sonra Yap",
    tagline: "AI destekli yazılımda profesyonel standartlar",
    kind: "tool",
    status: "private",
    icon: "compass",
    color: "#7A5AF8",
    platforms: ["Doküman"],
    tech: [],
    started: "Kasım 2026",
    summary:
      "Yeni bir uygulama ya da ekrana başlarken izlediğim yöntem: kod yazmadan önce iyi örnekleri incele, ortak olanı kural kabul et, farkı bilinçli yerlerde yarat.",
    features: [
      "Referans araştırma şablonu: 3–5 örnek uygulama, ortak kalıplar ve kararlar",
      "Arayüz kuralları: tutarlı bileşenler, en fazla iki ayar katmanı, boş ve hata ekranları",
      "AI’ın sık hataları: koda gömülü gizli anahtarlar, açık veritabanı kuralları, kopya kod",
      "Kod sadeliği ve optimizasyon ilkeleri",
    ],
    links: [
      { label: "Rehberi oku", url: "https://claude.ai/artifact/QeQpQXGQoa8WUrGVJeuGBp", private: true },
    ],
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
      "Kategori ve teknoloji filtresi",
      "Her projenin açılır ayrıntı bölümü ve paylaşılabilir bağlantısı",
      "Koyu ve açık tema; varsayılan olarak cihaz ayarını izler",
    ],
    links: [{ label: "GitHub", url: "https://github.com/flozaxx" }],
  },
];
